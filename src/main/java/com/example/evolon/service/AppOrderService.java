package com.example.evolon.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.OrderStatus;
import com.example.evolon.entity.Review;
import com.example.evolon.entity.ReviewResult;
import com.example.evolon.entity.User;
import com.example.evolon.repository.AppOrderRepository;
import com.example.evolon.repository.ItemRepository;
import com.example.evolon.repository.ReviewRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Service
public class AppOrderService {

	private final AppOrderRepository appOrderRepository;
	private final ItemRepository itemRepository;
	private final ItemService itemService;
	private final StripeService stripeService;
	private final LineNotifyService lineNotifyService;
	private final ReviewRepository reviewRepository;

	public AppOrderService(
			AppOrderRepository appOrderRepository,
			ItemRepository itemRepository,
			ItemService itemService,
			StripeService stripeService,
			LineNotifyService lineNotifyService,
			ReviewRepository reviewRepository) {

		this.appOrderRepository = appOrderRepository;
		this.itemRepository = itemRepository;
		this.itemService = itemService;
		this.stripeService = stripeService;
		this.lineNotifyService = lineNotifyService;
		this.reviewRepository = reviewRepository;
	}

	/* =====================
	 * 決済開始
	 * ===================== */
	@Transactional
	public PaymentIntent initiatePurchase(Long itemId, User buyer) throws StripeException {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が存在しません"));

		if (!item.canBePurchased()) {
			throw new IllegalStateException("この商品は購入できません");
		}

		PaymentIntent intent = stripeService.createPaymentIntent(
				item.getPrice(),
				"jpy",
				"購入: " + item.getName());

		AppOrder order = new AppOrder();
		order.setItem(item);
		order.setBuyer(buyer);
		order.setPrice(item.getPrice());
		order.setOrderStatus(OrderStatus.PAYMENT_PENDING);
		order.setStatus(OrderStatus.PAYMENT_PENDING.getLabel());
		order.setPaymentIntentId(intent.getId());
		order.setCreatedAt(LocalDateTime.now());

		appOrderRepository.save(order);
		return intent;
	}

	/* =====================
	 * 決済完了
	 * ===================== */
	@Transactional
	public AppOrder completePurchase(String paymentIntentId) throws StripeException {

		if (!stripeService.isPaymentSucceeded(paymentIntentId)) {
			throw new IllegalStateException("決済が完了していません");
		}

		AppOrder order = appOrderRepository.findByPaymentIntentId(paymentIntentId)
				.orElseThrow(() -> new IllegalStateException("注文が見つかりません"));

		if (order.getOrderStatus() != OrderStatus.PAYMENT_PENDING) {
			return order;
		}

		order.completePurchase();
		itemService.markAsSold(order.getItem().getId());
		notifySellerPurchased(order);

		return order;
	}

	/* =====================
	 * 発送（出品者）
	 * ===================== */
	@Transactional
	public void markOrderAsShipped(Long orderId, String sellerEmail) {

		AppOrder order = findOrder(orderId);

		if (!order.getItem().getSeller().getEmail().equals(sellerEmail)) {
			throw new IllegalStateException("発送権限がありません");
		}
		if (order.getOrderStatus() != OrderStatus.PURCHASED) {
			throw new IllegalStateException("発送できないステータスです");
		}

		order.ship(LocalDateTime.now());
		notifyBuyerShipped(order);
	}

	/* =====================
	 * 到着確認
	 * ===================== */
	@Transactional
	public void markOrderAsDelivered(Long orderId, String buyerEmail) {

		AppOrder order = findOrder(orderId);

		if (!order.getBuyer().getEmail().equals(buyerEmail)) {
			throw new IllegalStateException("到着確認の権限がありません");
		}
		if (order.getOrderStatus() != OrderStatus.SHIPPED) {
			throw new IllegalStateException("到着確認できません");
		}

		order.deliver(LocalDateTime.now());
		order.setStatus(OrderStatus.DELIVERED.getLabel());
	}

	/* =====================
	 * レビュー（共通）
	 * ===================== */
	@Transactional
	public void submitReview(Long orderId, String reviewerEmail, ReviewResult result, String comment) {

		AppOrder order = findOrder(orderId);

		boolean isBuyer = order.getBuyer().getEmail().equals(reviewerEmail);
		boolean isSeller = order.getItem().getSeller().getEmail().equals(reviewerEmail);

		if (!isBuyer && !isSeller) {
			throw new IllegalStateException("評価権限がありません");
		}

		User reviewer = isBuyer ? order.getBuyer() : order.getItem().getSeller();

		if (reviewRepository.existsByOrder_IdAndReviewer(orderId, reviewer)) {
			throw new IllegalStateException("すでに評価済みです");
		}

		if (isSeller) {
			boolean buyerReviewed = reviewRepository.existsByOrder_IdAndReviewer(orderId, order.getBuyer());
			if (!buyerReviewed) {
				throw new IllegalStateException("購入者の評価完了後に出品者は評価できます");
			}
		}

		User reviewee = isBuyer ? order.getItem().getSeller() : order.getBuyer();

		Review review = new Review();
		review.setOrder(order);
		review.setReviewer(reviewer);
		review.setReviewee(reviewee);
		review.setSeller(order.getItem().getSeller());
		review.setItem(order.getItem());
		review.setResult(result);
		review.setRating(result == ReviewResult.GOOD ? 5 : 1);
		review.setComment(comment.trim());

		reviewRepository.save(review);

		boolean buyerReviewed = reviewRepository.existsByOrder_IdAndReviewer(orderId, order.getBuyer());
		boolean sellerReviewed = reviewRepository.existsByOrder_IdAndReviewer(orderId, order.getItem().getSeller());

		if (buyerReviewed && sellerReviewed) {
			order.setOrderStatus(OrderStatus.COMPLETED);
			order.setStatus(OrderStatus.COMPLETED.getLabel());
			 
			itemService.markAsSold(order.getItem().getId()); 

		}
	}

	/* =====================
	 * 取得系（既存Controller互換）
	 * ===================== */

	public Optional<AppOrder> getOrderById(Long id) {
		return appOrderRepository.findById(id);
	}

	public List<AppOrder> findPurchasedOrdersByBuyer(User buyer) {
		return appOrderRepository.findByBuyer(buyer);
	}

	public List<AppOrder> findOrdersBySeller(User seller) {
		return appOrderRepository.findByItem_Seller(seller);
	}

	public List<AppOrder> getAllOrders() {
		return appOrderRepository.findAll();
	}

	public List<AppOrder> getRecentOrders() {
		return appOrderRepository.findTop5ByOrderByCreatedAtDesc();
	}

	public BigDecimal getTotalSales(LocalDate start, LocalDate end) {
		return appOrderRepository.findAll().stream()
				.filter(o -> o.getCreatedAt().toLocalDate().compareTo(start) >= 0)
				.filter(o -> o.getCreatedAt().toLocalDate().compareTo(end) <= 0)
				.map(AppOrder::getPrice)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Map<String, Long> getOrderCountByStatus(LocalDate start, LocalDate end) {
		return appOrderRepository.findAll().stream()
				.filter(o -> o.getCreatedAt().toLocalDate().compareTo(start) >= 0)
				.filter(o -> o.getCreatedAt().toLocalDate().compareTo(end) <= 0)
				.collect(Collectors.groupingBy(
						o -> o.getOrderStatus().name(),
						Collectors.counting()));
	}

	/* =====================
	 * util
	 * ===================== */

	private AppOrder findOrder(Long id) {
		return appOrderRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("注文が見つかりません"));
	}

	private void notifySellerPurchased(AppOrder order) {
		User seller = order.getItem().getSeller();
		if (seller.getLineNotifyToken() != null) {
			lineNotifyService.sendMessage(
					seller.getLineNotifyToken(),
					"商品が購入されました：" + order.getItem().getName());
		}
	}

	private void notifyBuyerShipped(AppOrder order) {
		User buyer = order.getBuyer();
		if (buyer.getLineNotifyToken() != null) {
			lineNotifyService.sendMessage(
					buyer.getLineNotifyToken(),
					"商品が発送されました：" + order.getItem().getName());
		}
	}
}
