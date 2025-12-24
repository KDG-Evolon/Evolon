package com.example.evolon.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.OrderStatus;
import com.example.evolon.entity.User;
import com.example.evolon.repository.AppOrderRepository;
import com.example.evolon.repository.ItemRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Service
public class AppOrderService {

	private final AppOrderRepository appOrderRepository;
	private final ItemRepository itemRepository;
	private final ItemService itemService;
	private final StripeService stripeService;
	private final LineNotifyService lineNotifyService;

	public AppOrderService(
			AppOrderRepository appOrderRepository,
			ItemRepository itemRepository,
			ItemService itemService,
			StripeService stripeService,
			LineNotifyService lineNotifyService) {
		this.appOrderRepository = appOrderRepository;
		this.itemRepository = itemRepository;
		this.itemService = itemService;
		this.stripeService = stripeService;
		this.lineNotifyService = lineNotifyService;
	}

	/* =================================================
	 * 購入開始（Stripe 決済前：仮注文）
	 * ================================================= */
	@Transactional
	public PaymentIntent initiatePurchase(Long itemId, User buyer) throws StripeException {

		System.out.println("★★ AppOrder 作成開始 ★★");

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が存在しません"));

		if (!item.canBePurchased()) {
			throw new IllegalStateException("この商品は購入できません");
		}

		PaymentIntent paymentIntent = stripeService.createPaymentIntent(
				item.getPrice(),
				"jpy",
				"購入: " + item.getName());

		AppOrder order = new AppOrder();
		order.setItem(item);
		order.setBuyer(buyer);
		order.setPrice(item.getPrice());
		order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
		order.setStatus(OrderStatus.PAYMENT_PENDING.getLabel()); // 表示用
		order.setPaymentIntentId(paymentIntent.getId());
		order.setCreatedAt(LocalDateTime.now());

		appOrderRepository.save(order);

		System.out.println("★★ AppOrder 保存完了 ★★");

		return paymentIntent;
	}

	/* =================================================
	 * 決済完了（Stripe 成功 → 注文確定）
	 * ================================================= */
	@Transactional
	public AppOrder completePurchase(String paymentIntentId) throws StripeException {

		if (!stripeService.isPaymentSucceeded(paymentIntentId)) {
			throw new IllegalStateException("決済が完了していません");
		}

		AppOrder order = appOrderRepository.findByPaymentIntentId(paymentIntentId)
				.orElseThrow(() -> new IllegalStateException("注文が見つかりません"));

		// 二重実行防止
		if (order.getOrderStatus() == OrderStatus.PURCHASED) {
			return order;
		}

		order.completePurchase(); // PURCHASED
		itemService.markAsSold(order.getItem().getId());

		notifySellerPurchased(order);
		return order;
	}

	/* =================================================
	 * 発送（出品者）
	 * ================================================= */
	@Transactional
	public void markOrderAsShipped(Long orderId) {
		AppOrder order = findOrder(orderId);
		order.ship(LocalDateTime.now());
		notifyBuyerShipped(order);
	}

	/* =================================================
	 * 到着確認（購入者）
	 * ================================================= */
	@Transactional
	public void markOrderAsDelivered(Long orderId, String email) {

		AppOrder order = findOrder(orderId);

		if (!order.getBuyer().getEmail().equals(email)) {
			throw new IllegalStateException("到着確認の権限がありません");
		}

		order.deliver(LocalDateTime.now());
	}

	/* =================================================
	 * 取得系（Controller 用）
	 * ================================================= */

	/** 購入履歴（表示用） */
	public List<AppOrder> findPurchasedOrdersByBuyer(User buyer) {
		return appOrderRepository.findByBuyerAndOrderStatusIn(
				buyer,
				List.of(
						OrderStatus.PURCHASED,
						OrderStatus.SHIPPED,
						OrderStatus.DELIVERED));
	}

	/** 出品者の売上 */
	public List<AppOrder> findOrdersBySeller(User seller) {
		return appOrderRepository.findByItem_Seller(seller);
	}

	public Optional<AppOrder> getOrderById(Long orderId) {
		return appOrderRepository.findById(orderId);
	}

	public List<AppOrder> getRecentOrders() {
		return appOrderRepository.findTop5ByOrderByCreatedAtDesc();
	}

	/** ★ 管理者ダッシュボード用（これが必要だった） */
	public List<AppOrder> getAllOrders() {
		return appOrderRepository.findAll();
	}

	/* =================================================
	 * 集計用
	 * ================================================= */
	public BigDecimal getTotalSales(LocalDate start, LocalDate end) {
		return appOrderRepository.findAll().stream()
				.filter(o -> o.getOrderStatus() == OrderStatus.PURCHASED
						|| o.getOrderStatus() == OrderStatus.SHIPPED)
				.filter(o -> !o.getCreatedAt().toLocalDate().isBefore(start)
						&& !o.getCreatedAt().toLocalDate().isAfter(end))
				.map(AppOrder::getPrice)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Map<String, Long> getOrderCountByStatus(LocalDate start, LocalDate end) {
		return appOrderRepository.findAll().stream()
				.filter(o -> !o.getCreatedAt().toLocalDate().isBefore(start)
						&& !o.getCreatedAt().toLocalDate().isAfter(end))
				.collect(Collectors.groupingBy(
						o -> o.getOrderStatus().name(),
						Collectors.counting()));
	}

	/* =================================================
	 * private helper
	 * ================================================= */
	private AppOrder findOrder(Long id) {
		return appOrderRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("注文が見つかりません"));
	}

	private void notifySellerPurchased(AppOrder order) {
		User seller = order.getItem().getSeller();
		if (seller.getLineNotifyToken() == null)
			return;

		String msg = String.format(
				"\n商品が購入されました\n商品名: %s\n購入者: %s\n価格: ¥%s",
				order.getItem().getName(),
				order.getBuyer().getName(),
				order.getPrice());

		lineNotifyService.sendMessage(seller.getLineNotifyToken(), msg);
	}

	private void notifyBuyerShipped(AppOrder order) {
		User buyer = order.getBuyer();
		if (buyer.getLineNotifyToken() == null)
			return;

		String msg = String.format(
				"\n商品が発送されました\n商品名: %s\n出品者: %s",
				order.getItem().getName(),
				order.getItem().getSeller().getName());

		lineNotifyService.sendMessage(buyer.getLineNotifyToken(), msg);
	}
}
