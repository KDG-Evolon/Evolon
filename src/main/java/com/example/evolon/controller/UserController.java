package com.example.evolon.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.evolon.entity.User;
import com.example.evolon.service.AppOrderService;
import com.example.evolon.service.FavoriteService;
import com.example.evolon.service.ItemService;
import com.example.evolon.service.ReviewService;
import com.example.evolon.service.UserService;

@Controller
@RequestMapping("/my-page")
public class UserController {

	private final UserService userService;
	private final ItemService itemService;
	private final AppOrderService appOrderService;
	private final FavoriteService favoriteService;
	private final ReviewService reviewService;

	public UserController(
			UserService userService,
			ItemService itemService,
			AppOrderService appOrderService,
			FavoriteService favoriteService,
			ReviewService reviewService) {

		this.userService = userService;
		this.itemService = itemService;
		this.appOrderService = appOrderService;
		this.favoriteService = favoriteService;
		this.reviewService = reviewService;
	}

	/* =====================
	 * マイページ TOP
	 * ===================== */
	@GetMapping
	public String myPage(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User user = getLoginUser(userDetails);

		model.addAttribute("user", user);

		// ✅ 最近の購入履歴（確定済のみ）
		model.addAttribute(
				"orders",
				appOrderService.findPurchasedOrdersByBuyer(user));

		return "my_page";
	}

	/* =====================
	 * 購入履歴
	 * ===================== */
	@GetMapping("/orders")
	public String myOrders(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User user = getLoginUser(userDetails);

		// ★ここが超重要
		model.addAttribute(
				"myOrders",
				appOrderService.findPurchasedOrdersByBuyer(user));

		return "buyer_app_orders";
	}

	/* =====================
	 * 販売履歴（出品者）
	 * ===================== */
	@GetMapping("/sales")
	public String mySales(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User user = getLoginUser(userDetails);

		model.addAttribute(
				"mySales",
				appOrderService.findOrdersBySeller(user));

		return "seller_app_orders";
	}

	/* =====================
	 * 出品中商品
	 * ===================== */
	@GetMapping("/selling")
	public String mySellingItems(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User user = getLoginUser(userDetails);

		model.addAttribute(
				"items",
				itemService.getItemsBySeller(user));

		return "seller_items";
	}

	/* =====================
	 * お気に入り
	 * ===================== */
	@GetMapping("/favorites")
	public String myFavorites(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User user = getLoginUser(userDetails);

		model.addAttribute(
				"favoriteItems",
				favoriteService.getFavoriteItemsByUser(user));

		return "my_favorites";
	}

	/* =====================
	 * 自分のレビュー
	 * ===================== */
	@GetMapping("/reviews")
	public String myReviews(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		User user = getLoginUser(userDetails);

		model.addAttribute(
				"reviews",
				reviewService.getReviewsByReviewer(user));

		return "user_reviews";
	}

	/* =====================
	 * 共通：ログインユーザ取得
	 * ===================== */
	private User getLoginUser(UserDetails userDetails) {
		return userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
	}
}
