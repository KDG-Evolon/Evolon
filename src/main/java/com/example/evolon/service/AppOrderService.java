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

	public AppOrderService(AppOrderRepository appOrderRepository,
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

	@Transactional
	public PaymentIntent initiatePurchase(Long itemId, User buyer) throws StripeException {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));

		if (!"出品中".equals(item.getStatus())) {
			throw new IllegalStateException("Item is not available for purchase.");
		}

		PaymentIntent paymentIntent = stripeService.createPaymentIntent(item.getPrice(), "jpy",
				"購入: " + item.getName());

		AppOrder appOrder = new AppOrder();
		appOrder.setItem(item);
		appOrder.setBuyer(buyer);
		appOrder.setPrice(item.getPrice());
		appOrder.setStatus("決済待ち");
		appOrder.setPaymentIntentId(paymentIntent.getId());
		appOrder.setCreatedAt(LocalDateTime.now());

		appOrderRepository.save(appOrder);
		return paymentIntent;
	}

	@Transactional
	public AppOrder completePurchase(String paymentIntentId) throws StripeException {

		PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(paymentIntentId);

		if (!"succeeded".equals(paymentIntent.getStatus())) {
			throw new IllegalStateException("Payment not succeeded. Status: " +
					paymentIntent.getStatus());
		}

		AppOrder appOrder = appOrderRepository.findByPaymentIntentId(paymentIntentId)
				.orElseThrow(() -> new IllegalStateException("Order for PaymentIntent not found."));

		if ("購入済".equals(appOrder.getStatus()) || "発送済".equals(appOrder.getStatus())) {
			return appOrder;
		}

		appOrder.setStatus("購入済");
		itemService.markItemAsSold(appOrder.getItem().getId());

		AppOrder savedOrder = appOrderRepository.save(appOrder);

		if (savedOrder.getItem().getSeller().getLineNotifyToken() != null) {

			String message = String.format(
					"\n 商品が購入されました！\n 商品名: %s\n 購入者: %s\n 価格: ¥%s",
					savedOrder.getItem().getName(),
					savedOrder.getBuyer().getName(),
					savedOrder.getPrice());

			lineNotifyService.sendMessage(
					savedOrder.getItem().getSeller().getLineNotifyToken(),
					message);
		}

		return savedOrder;
	}

	public List<AppOrder> getAllOrders() {
		return appOrderRepository.findAll();
	}

	public List<AppOrder> getOrdersByBuyer(User buyer) {
		return appOrderRepository.findByBuyer(buyer);
	}

	public List<AppOrder> getOrdersBySeller(User seller) {
		return appOrderRepository.findByItem_Seller(seller);
	}

	@Transactional
	public void markOrderAsShipped(Long orderId) {

		AppOrder appOrder = appOrderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found"));

		appOrder.setStatus("発送済");

		AppOrder savedOrder = appOrderRepository.save(appOrder);

		String message = String.format(
				"\n購入した商品が発送されました！\n商品名: %s\n出品者: %s",
				savedOrder.getItem().getName(),
				savedOrder.getItem().getSeller().getName());

		lineNotifyService.sendMessage(
				savedOrder.getBuyer().getLineNotifyToken(),
				message);
	}

	public Optional<AppOrder> getOrderById(Long orderId) {
		return appOrderRepository.findById(orderId);
	}

	public Optional<Long> getLatestCompletedOrderId() {
		return appOrderRepository.findAll().stream()
				.filter(o -> "購入済".equals(o.getStatus()))
				.map(AppOrder::getId)
				.max(Long::compare);
	}

	public BigDecimal getTotalSales(LocalDate startDate, LocalDate endDate) {

		return appOrderRepository.findAll().stream()
				.filter(order -> order.getStatus().equals("購入済")
						|| order.getStatus().equals("発送済"))
				.filter(order -> order.getCreatedAt().toLocalDate().isAfter(startDate.minusDays(1))
						&& order.getCreatedAt().toLocalDate().isBefore(endDate.plusDays(1)))
				.map(AppOrder::getPrice)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public Map<String, Long> getOrderCountByStatus(LocalDate startDate, LocalDate endDate) {

		return appOrderRepository.findAll().stream()
				.filter(order -> order.getCreatedAt().toLocalDate().isAfter(startDate.minusDays(1))
						&& order.getCreatedAt().toLocalDate().isBefore(endDate.plusDays(1)))
				.collect(Collectors.groupingBy(AppOrder::getStatus, Collectors.counting()));
	}

	public List<AppOrder> getRecentOrders(int limit) {
		// 今回は5固定でもOK。拡張余地として引数だけ残す
		return appOrderRepository.findTop5ByOrderByCreatedAtDesc();
	}

}
