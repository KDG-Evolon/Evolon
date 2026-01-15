package com.example.evolon.controller;

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

/**
 * レビュー（評価）画面に関するコントローラ
 */
@Controller
@RequestMapping("/reviews")
public class ReviewController {

	/** 注文サービス */
	private final AppOrderService appOrderService;

	/** ユーザーサービス */
	private final UserService userService;

	public ReviewController(
			AppOrderService appOrderService,
			UserService userService) {
		this.appOrderService = appOrderService;
		this.userService = userService;
	}

	/**
	 * 新規レビュー入力画面表示
	 * GET /reviews/new/{orderId}
	 */
	@GetMapping("/new/{orderId}")
	public String showReviewForm(
			@PathVariable("orderId") Long orderId,
			Model model) {

		AppOrder order = appOrderService.getOrderById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		model.addAttribute("order", order);

		// review_form.html
		return "review_form";
	}

	/**
	 * レビュー送信処理
	 * POST /reviews
	 */
	@PostMapping
	public String submitReview(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("orderId") Long orderId,
			@RequestParam("rating") int rating,
			@RequestParam("comment") String comment,
			RedirectAttributes redirectAttributes) {

		User reviewer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		try {
			ReviewResult result = (rating >= 4)
					? ReviewResult.GOOD
					: ReviewResult.BAD;

			appOrderService.submitReview(orderId, reviewer.getEmail(), result, comment);

			redirectAttributes.addFlashAttribute(
					"successMessage",
					"評価を送信しました！");
		} catch (IllegalStateException | IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					e.getMessage());
		}

		// 購入者の注文履歴へ
		return "redirect:/my-page/orders";
	}
}
