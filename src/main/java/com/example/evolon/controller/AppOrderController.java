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
import com.example.evolon.entity.ReviewResult;
import com.example.evolon.entity.User;
import com.example.evolon.service.AppOrderService;
import com.example.evolon.service.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Controller
@RequestMapping("/orders")
public class AppOrderController {

	private final AppOrderService appOrderService;
	private final UserService userService;

	@Value("${stripe.public-key}")
	private String stripePublicKey;

	public AppOrderController(AppOrderService appOrderService, UserService userService) {
		this.appOrderService = appOrderService;
		this.userService = userService;
	}

	/* =====================
	 * 購入開始（仮注文作成）
	 * ===================== */
	@PostMapping("/initiate")
	public String initiatePurchase(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("itemId") Long itemId,
			RedirectAttributes redirectAttributes) {

		User buyer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		try {
			PaymentIntent paymentIntent = appOrderService.initiatePurchase(itemId, buyer);

			return "redirect:/orders/confirm"
					+ "?clientSecret=" + paymentIntent.getClientSecret()
					+ "&paymentIntentId=" + paymentIntent.getId();

		} catch (StripeException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/items/" + itemId;
		}
	}

	/* =====================
	 * Stripe 決済画面
	 * ===================== */
	@GetMapping("/confirm")
	public String confirmPayment(
			@RequestParam(value = "clientSecret", required = false) String clientSecret,
			@RequestParam(value = "paymentIntentId", required = false) String paymentIntentId,
			Model model) {

		if (clientSecret == null || paymentIntentId == null) {
			return "redirect:/items";
		}

		model.addAttribute("clientSecret", clientSecret);
		model.addAttribute("paymentIntentId", paymentIntentId);
		model.addAttribute("stripePublicKey", stripePublicKey);

		return "payment_confirmation";
	}

	/* =====================
	 * 決済完了（注文確定）
	 * ===================== */
	@GetMapping("/complete-purchase")
	public String completePurchase(
			@RequestParam(value = "payment_intent", required = false) String paymentIntentId,
			RedirectAttributes redirectAttributes) {

		try {
			appOrderService.completePurchase(paymentIntentId);
			redirectAttributes.addFlashAttribute("successMessage", "商品を購入しました！");
			return "redirect:/my-page/orders";

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", "決済処理に失敗しました");
			return "redirect:/items";
		}
	}

	/* =====================
	 * 販売履歴（出品者）
	 * ===================== */
	@GetMapping("/sales")
	public String sales(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User seller = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		List<AppOrder> mySales = appOrderService.findOrdersBySeller(seller);
		model.addAttribute("mySales", mySales);

		return "seller_app_orders";
	}

	/* =====================
	 * 発送済みにする（出品者）
	 * ===================== */
	@PostMapping("/{id}/ship")
	public String shipOrder(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable("id") Long orderId,
			RedirectAttributes redirectAttributes) {

		String sellerEmail = userDetails.getUsername();

		try {
			// ✅ service側は (orderId, sellerEmail) を受け取る実装になってる
			appOrderService.markOrderAsShipped(orderId, sellerEmail);

			redirectAttributes.addFlashAttribute("successMessage", "発送済みにしました");
			return "redirect:/orders/sales";

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/orders/sales";
		}
	}

	/* =====================
	 * 到着・評価機能
	 * ===================== */
	@PostMapping("/{id}/receive-and-review")
	public String receiveAndReview(
			@AuthenticationPrincipal UserDetails userDetails,
			@PathVariable Long id,
			@RequestParam("result") ReviewResult result,
			@RequestParam("comment") String comment,
			RedirectAttributes ra) {
		try {
			appOrderService.completeOrderWithReview(
					id,
					userDetails.getUsername(),
					result,
					comment);
			ra.addFlashAttribute("successMessage", "取引が完了しました！");
			return "redirect:/my-page/orders";
		} catch (Exception e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/my-page/orders";
		}
	}

}
