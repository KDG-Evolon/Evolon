package com.example.evolon.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.Chat;
import com.example.evolon.entity.OrderStatus;
import com.example.evolon.entity.ReviewResult;
import com.example.evolon.entity.User;
import com.example.evolon.service.AppOrderService;
import com.example.evolon.service.ChatService;
import com.example.evolon.service.OrderMessageService;
import com.example.evolon.service.ReviewService;
import com.example.evolon.service.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Controller
@RequestMapping("/orders")
public class AppOrderController {

	private final AppOrderService appOrderService;
	private final UserService userService;

	// 商品詳細の「商品チャット」を使っているなら残す（不要なら後で消してOK）
	private final ChatService chatService;

	// 評価関連
	private final ReviewService reviewService;

	// ★ 取引メッセージ（今回の本命）
	private final OrderMessageService orderMessageService;

	@Value("${stripe.public-key}")
	private String stripePublicKey;

	public AppOrderController(
			AppOrderService appOrderService,
			UserService userService,
			ChatService chatService,
			ReviewService reviewService,
			OrderMessageService orderMessageService) {

		this.appOrderService = appOrderService;
		this.userService = userService;
		this.chatService = chatService;
		this.reviewService = reviewService;
		this.orderMessageService = orderMessageService;
	}

	/* =====================
	 * 購入開始
	 * ===================== */
	@PostMapping("/initiate")
	public String initiatePurchase(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("itemId") Long itemId,
			RedirectAttributes ra) {

		User buyer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 配送先情報が未登録ならプロフィール編集へ誘導
		if (isBlank(buyer.getLastName())
				|| isBlank(buyer.getFirstName())
				|| isBlank(buyer.getPostalCode())
				|| isBlank(buyer.getAddress())) {

			ra.addFlashAttribute("errorMessage", "購入には配送先情報（氏名・郵便番号・住所）の登録が必要です");
			return "redirect:/my-page/profile/edit?returnTo=/items/" + itemId;
		}

		try {
			PaymentIntent pi = appOrderService.initiatePurchase(itemId, buyer);
			return "redirect:/orders/confirm"
					+ "?clientSecret=" + pi.getClientSecret()
					+ "&paymentIntentId=" + pi.getId();
		} catch (StripeException | IllegalStateException e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/items/" + itemId;
		}
	}

	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/* =====================
	 * Stripe 決済画面
	 * ===================== */
	@GetMapping("/confirm")
	public String confirmPayment(
			@RequestParam String clientSecret,
			@RequestParam String paymentIntentId,
			Model model) {

		model.addAttribute("clientSecret", clientSecret);
		model.addAttribute("paymentIntentId", paymentIntentId);
		model.addAttribute("stripePublicKey", stripePublicKey);
		return "payment_confirmation";
	}

	/* =====================
	 * 決済完了
	 * ===================== */
	@GetMapping("/complete-purchase")
	public String completePurchase(
			@RequestParam("payment_intent") String paymentIntentId,
			RedirectAttributes ra) {

		try {
			appOrderService.completePurchase(paymentIntentId);
			ra.addFlashAttribute("successMessage", "商品を購入しました");
			return "redirect:/my-page/orders";
		} catch (Exception e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/items";
		}
	}

	/* =====================
	 * 取引詳細
	 * ===================== */
	@GetMapping("/{id:\\d+}")
	public String orderDetail(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			Model model,
			RedirectAttributes ra) {

		User loginUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		AppOrder order = appOrderService.getOrderById(id)
				.orElseThrow(() -> new RuntimeException("注文が見つかりません"));

		boolean isBuyer = order.getBuyer().getId().equals(loginUser.getId());
		boolean isSeller = order.getItem().getSeller().getId().equals(loginUser.getId());

		if (!isBuyer && !isSeller) {
			ra.addFlashAttribute("errorMessage", "権限がありません");
			return "redirect:/my-page";
		}

		// -------------------------
		// ★ 取引メッセージ一覧（今回の本命）
		// -------------------------
		model.addAttribute("orderMessages", orderMessageService.getMessages(order));

		// -------------------------
		// ※ 商品詳細のチャット（商品チャット）も表示したいなら残す
		//    order_detail.html が chats を使ってないなら削除してOK
		// -------------------------
		List<Chat> chats = chatService.getChatMessagesByItem(order.getItem().getId());
		model.addAttribute("chats", chats);

		// ✅ 評価済み判定は ReviewService に統一
		boolean buyerReviewed = reviewService.hasReviewed(id, order.getBuyer());
		boolean sellerReviewed = reviewService.hasReviewed(id, order.getItem().getSeller());

		OrderStatus st = order.getOrderStatus();

		model.addAttribute("order", order);
		model.addAttribute("isBuyer", isBuyer);
		model.addAttribute("isSeller", isSeller);

		model.addAttribute("canShip", isSeller && st == OrderStatus.PURCHASED);
		model.addAttribute("canDeliver", isBuyer && st == OrderStatus.SHIPPED);

		model.addAttribute("canBuyerReview",
				isBuyer && st == OrderStatus.DELIVERED && !buyerReviewed);

		model.addAttribute("canSellerReview",
				isSeller && st == OrderStatus.DELIVERED && buyerReviewed && !sellerReviewed);

		model.addAttribute("buyerReviewed", buyerReviewed);
		model.addAttribute("sellerReviewed", sellerReviewed);

		return "order_detail";
	}

	/* =====================
	 * 取引メッセージ送信
	 * ===================== */
	@PostMapping("/{id}/chat")
	public String sendChat(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam String message,
			RedirectAttributes ra) {

		if (message == null || message.trim().isEmpty()) {
			ra.addFlashAttribute("errorMessage", "メッセージを入力してください");
			return "redirect:/orders/" + id;
		}

		User sender = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		AppOrder order = appOrderService.getOrderById(id)
				.orElseThrow(() -> new RuntimeException("注文が見つかりません"));

		boolean allowed = order.getBuyer().getId().equals(sender.getId())
				|| order.getItem().getSeller().getId().equals(sender.getId());

		if (!allowed) {
			ra.addFlashAttribute("errorMessage", "送信権限がありません");
			return "redirect:/my-page";
		}

		// ★ ここが重要：商品チャット(ChatService)ではなく、取引メッセージ(OrderMessage)に保存する
		orderMessageService.send(order, sender, message.trim());

		return "redirect:/orders/" + id;
	}

	/* =====================
	 * 発送
	 * ===================== */
	@PostMapping("/{id}/ship")
	public String ship(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			RedirectAttributes ra) {

		try {
			appOrderService.markOrderAsShipped(id, userDetails.getUsername());
			ra.addFlashAttribute("successMessage", "発送しました");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/orders/" + id;
	}

	/* =====================
	 * 到着確認
	 * ===================== */
	@PostMapping("/{id}/deliver")
	public String deliver(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			RedirectAttributes ra) {

		try {
			appOrderService.markOrderAsDelivered(id, userDetails.getUsername());
			ra.addFlashAttribute("successMessage", "到着確認しました");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/orders/" + id;
	}

	/* =====================
	 * 評価（購入者・出品者共通）
	 * ===================== */
	@PostMapping("/{id}/review")
	public String review(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam ReviewResult result,
			@RequestParam String comment,
			RedirectAttributes ra) {

		try {
			appOrderService.submitReview(
					id,
					userDetails.getUsername(),
					result,
					comment);
			ra.addFlashAttribute("successMessage", "評価を送信しました");
		} catch (Exception e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/orders/" + id;
	}
}
