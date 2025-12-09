package com.example.evolon.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.evolon.entity.User;
import com.example.evolon.repository.UserRepository;
import com.example.evolon.service.AppOrderService;
import com.example.evolon.service.ItemService;

/**
 * ダッシュボード画面を表示するコントローラ
 */
@Controller
public class DashboardController {

	/** ユーザー情報取得用 */
	private final UserRepository userRepository;

	/** 商品情報取得用 */
	private final ItemService itemService;

	/** 注文情報取得用 */
	private final AppOrderService appOrderService;

	/** コンストラクタインジェクション */
	public DashboardController(
			UserRepository userRepository,
			ItemService itemService,
			AppOrderService appOrderService) {
		this.userRepository = userRepository;
		this.itemService = itemService;
		this.appOrderService = appOrderService;
	}

	/**
	 * ダッシュボード表示
	 */
	@GetMapping("/dashboard")
	public String dashboard(
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		// ログインユーザー取得
		User currentUser = userRepository
				.findByEmailIgnoreCase(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 管理者の場合
		if ("ADMIN".equals(currentUser.getRole())) {

			model.addAttribute("recentItems", itemService.getAllItems());
			model.addAttribute("recentOrders", appOrderService.getAllOrders());

			return "admin/dashboard";
		}

		// 一般ユーザーは商品一覧へ
		return "redirect:/items";
	}
}
