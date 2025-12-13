package com.example.evolon.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.evolon.service.AppOrderService;
import com.example.evolon.service.ItemService;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

	private final ItemService itemService;
	private final AppOrderService appOrderService;

	public AdminDashboardController(ItemService itemService,
			AppOrderService appOrderService) {
		this.itemService = itemService;
		this.appOrderService = appOrderService;
	}

	/** 管理者ダッシュボード */
	@GetMapping({ "", "/dashboard" })
	public String dashboard(Model model) {

		// 最近の出品（例：最新5件）
		model.addAttribute("recentItems",
				itemService.getAllItems().stream().limit(5).toList());

		// 最近の注文（Serviceにメソッドがあれば差し替え）
		model.addAttribute("recentOrders",
				appOrderService.getRecentOrders(5));

		return "admin_dashboard";
	}
}
