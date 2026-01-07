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
import com.example.evolon.entity.Review;
import com.example.evolon.entity.ReviewResult;
import com.example.evolon.entity.User;
import com.example.evolon.repository.AppOrderRepository;
import com.example.evolon.repository.ItemRepository;
import com.example.evolon.repository.ReviewRepository; // ★追加
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Service
public class AppOrderService {

	private final AppOrderRepository appOrderRepository;
	private final ItemRepository itemRepository;
	private final ItemService itemService;
	private final StripeService stripeService;
	private final LineNotifyService lineNotifyService;
	private final ReviewRepository reviewRepository; // ★追加

	public AppOrderService(
			AppOrderRepository appOrderRepository,
			ItemRepository itemRepository,
			ItemService itemService,
			StripeService stripeService,
			LineNotifyService lineNotifyService,
			ReviewRepository reviewRepository // ★追加
	) {
		this.appOrderRepository = appOrderRepository;
		this.itemRepository = itemRepository;
		this.itemService = itemService;
		this.stripeService = stripeService;
		this.lineNotifyService = lineNotifyService;
		this.reviewRepository = reviewRepository; // ★追加
	}

	/* =================================================
	 * 購入開始（Stripe 決済前：仮注文）
	 * ================================================= */
	@Transactional
	public PaymentIntent initiatePurchase(Long itemId, User buyer) throws StripeException {

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

		// ✅ 注文：決済待ち
		order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
		order.setStatus(OrderStatus.PAYMENT_PENDING.getLabel()); // 表示用（将来は消してOK）

		order.setPaymentIntentId(paymentIntent.getId());
		order.setCreatedAt(LocalDateTime.now());

		appOrderRepository.save(order);

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

		// ✅ 二重実行防止：PAYMENT_PENDING 以外なら何もしない
		if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
			return order;
		}

		// ✅ 注文：購入済（=発送待ち）
		order.setOrderStatus(OrderStatus.PURCHASED);
		order.setStatus(OrderStatus.PURCHASED.getLabel()); // 表示用（将来は消してOK）

		// ✅ 商品：出品一覧から消す（出品中ではなくなる）
		itemService.markAsPaymentDone(order.getItem().getId());

		notifySellerPurchased(order);
		return order;
	}

	/* =================================================
	 * 発送（出品者）
	 * ================================================= */
	@Transactional
	public void markOrderAsShipped(Long orderId, String sellerEmail) {

		AppOrder order = findOrder(orderId);

		// ✅ 出品者本人チェック
		if (!order.getItem().getSeller().getEmail().equals(sellerEmail)) {
			throw new IllegalStateException("発送の権限がありません");
		}

		// ✅ 状態チェック（購入済=発送待ち のときだけ発送可能）
		if (order.getOrderStatus() != OrderStatus.PURCHASED) {
			throw new IllegalStateException("発送できないステータスです");
		}

		order.ship(LocalDateTime.now()); // ここで SHIPPED に変わる想定
		order.setStatus(OrderStatus.SHIPPED.getLabel()); // 表示用（将来は消してOK）

		notifyBuyerShipped(order);
	}

	/* =================================================
	 * 到着確認（購入者）
	 * ================================================= */
	@Transactional
	public void markOrderAsDelivered(Long orderId, String buyerEmail) {

		AppOrder order = findOrder(orderId);

		// ✅ 購入者本人チェック
		if (!order.getBuyer().getEmail().equals(buyerEmail)) {
			throw new IllegalStateException("到着確認の権限がありません");
		}

		// ✅ 状態チェック（発送済のときだけ到着確認可能）
		if (order.getOrderStatus() != OrderStatus.SHIPPED) {
			throw new IllegalStateException("到着確認できないステータスです");
		}

		order.deliver(LocalDateTime.now()); // ここで DELIVERED に変わる想定
		order.setStatus(OrderStatus.DELIVERED.getLabel()); // 表示用（将来は消してOK）
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
						OrderStatus.DELIVERED,
						OrderStatus.COMPLETED));
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

	/** 管理者ダッシュボード用 */
	public List<AppOrder> getAllOrders() {
		return appOrderRepository.findAll();
	}

	/* =================================================
	 * 集計用
	 * ================================================= */
	public BigDecimal getTotalSales(LocalDate start, LocalDate end) {
		return appOrderRepository.findAll().stream()
				.filter(o -> o.getOrderStatus() == OrderStatus.PURCHASED
						|| o.getOrderStatus() == OrderStatus.SHIPPED
						|| o.getOrderStatus() == OrderStatus.DELIVERED
						|| o.getOrderStatus() == OrderStatus.COMPLETED) // ★追加推奨
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
	 * 到着確認 + 評価（同時処理）
	 * ================================================= */
	@Transactional
	public void completeOrderWithReview(
			Long orderId,
			String buyerEmail,
			ReviewResult result,
			String comment) {

		AppOrder order = findOrder(orderId);

		// 購入者チェック
		if (!order.getBuyer().getEmail().equals(buyerEmail)) {
			throw new IllegalStateException("権限がありません");
		}

		// ステータスチェック
		if (order.getOrderStatus() != OrderStatus.SHIPPED) {
			throw new IllegalStateException("この注文は完了できません");
		}

		// コメント必須
		if (comment == null || comment.trim().isEmpty()) {
			throw new IllegalArgumentException("コメントは必須です");
		}

		// 二重評価防止
		if (reviewRepository.existsByOrder_Id(orderId)) {
			throw new IllegalStateException("すでに評価済みです");
		}

		Review review = new Review();
		review.setOrder(order);
		review.setReviewer(order.getBuyer());
		review.setReviewee(order.getItem().getSeller());
		review.setResult(result);
		review.setComment(comment.trim());
		review.setCreatedAt(LocalDateTime.now());

		reviewRepository.save(review);

		// 注文完了（OrderStatusにCOMPLETEDが必要）
		order.setOrderStatus(OrderStatus.COMPLETED);
		order.setStatus(OrderStatus.COMPLETED.getLabel());
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
