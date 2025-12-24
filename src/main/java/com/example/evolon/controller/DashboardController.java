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
 * 管理者ダッシュボード表示用コントローラ
 */
@Controller
public class DashboardController {

	private final UserRepository userRepository;
	private final ItemService itemService;
	private final AppOrderService appOrderService;

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

		// 管理者のみ表示
		if ("ADMIN".equals(currentUser.getRole())) {

			model.addAttribute("currentUser", currentUser);

			// 最近の商品・注文（管理用）
			model.addAttribute("recentItems", itemService.getAllItems());
			model.addAttribute("recentOrders", appOrderService.getAllOrders());

			return "admin/dashboard";
		}

		// 一般ユーザーは商品一覧へ
		return "redirect:/items";
	}
}
