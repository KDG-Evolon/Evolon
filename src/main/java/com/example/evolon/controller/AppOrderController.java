package com.example.evolon.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.evolon.entity.User;
import com.example.evolon.service.AppOrderService;
import com.example.evolon.service.ItemService;
import com.example.evolon.service.UserService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

/**
 * 注文・決済（Stripe 連携含む）に関する画面制御コントローラー
 */
@Controller
@RequestMapping("/orders")
public class AppOrderController {

	private final AppOrderService appOrderService;
	private final UserService userService;
	private final ItemService itemService;

	@Value("${stripe.public.key}")
	private String stripePublicKey;

	public AppOrderController(
			AppOrderService appOrderService,
			UserService userService,
			ItemService itemService) {
		this.appOrderService = appOrderService;
		this.userService = userService;
		this.itemService = itemService;
	}

	/** 購入開始 */
	@PostMapping("/initiate-purchase")
	public String initiatePurchase(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("itemId") Long itemId,
			RedirectAttributes redirectAttributes) {

		User buyer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Buyer not found"));

		try {
			PaymentIntent paymentIntent = appOrderService.initiatePurchase(itemId, buyer);

			redirectAttributes.addFlashAttribute(
					"clientSecret",
					paymentIntent.getClientSecret());
			redirectAttributes.addFlashAttribute("itemId", itemId);

			return "redirect:/orders/confirm-payment";

		} catch (IllegalStateException | IllegalArgumentException | StripeException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/items/" + itemId;
		}
	}

	/** 決済確認画面 */
	@GetMapping("/confirm-payment")
	public String confirmPayment(
			@ModelAttribute("clientSecret") String clientSecret,
			@ModelAttribute("itemId") Long itemId,
			Model model) {

		if (clientSecret == null || itemId == null) {
			return "redirect:/items";
		}

		model.addAttribute("clientSecret", clientSecret);
		model.addAttribute("itemId", itemId);
		model.addAttribute("stripePublicKey", stripePublicKey);

		return "payment_confirmation";
	}

	/** 決済完了 */
	@GetMapping("/complete-purchase")
	public String completePurchase(
			@RequestParam("paymentIntentId") String paymentIntentId,
			RedirectAttributes redirectAttributes) {

		try {
			appOrderService.completePurchase(paymentIntentId);
			redirectAttributes.addFlashAttribute(
					"successMessage", "商品を購入しました！");

			return appOrderService.getLatestCompletedOrderId()
					.map(id -> "redirect:/reviews/new/" + id)
					.orElse("redirect:/my-page/orders");

		} catch (StripeException | IllegalStateException e) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"決済処理中にエラーが発生しました: " + e.getMessage());
			return "redirect:/items";
		}
	}

	/** Webhook */
	@PostMapping("/stripe-webhook")
	public void handleStripeWebhook(
			@RequestBody String payload,
			@RequestHeader("Stripe-Signature") String sigHeader) {
		System.out.println("Received Stripe Webhook: " + payload);
	}

	/** 発送処理 */
	@PostMapping("/{id}/ship")
	public String shipOrder(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes) {

		try {
			appOrderService.markOrderAsShipped(id);
			redirectAttributes.addFlashAttribute(
					"successMessage", "商品を発送済みにしました。");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}

		return "redirect:/my-page/sales";
	}
}
