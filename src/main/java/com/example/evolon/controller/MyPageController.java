//package com.example.evolon.controller;
//
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//
//import com.example.evolon.entity.User;
//import com.example.evolon.service.AppOrderService;
//import com.example.evolon.service.UserService;
//
//@Controller
//@RequestMapping("/my-page")
//public class MyPageController {
//
//	private final AppOrderService appOrderService;
//	private final UserService userService;
//
//	public MyPageController(AppOrderService appOrderService, UserService userService) {
//		this.appOrderService = appOrderService;
//		this.userService = userService;
//	}
//
//	@GetMapping
//	public String myPage(
//			@AuthenticationPrincipal UserDetails userDetails,
//			Model model) {
//
//		User user = userService.getUserByEmail(userDetails.getUsername())
//				.orElseThrow(() -> new RuntimeException("User not found"));
//
//		model.addAttribute(
//				"orders",
//				appOrderService.findPurchasedOrdersByBuyer(user));
//
//		return "my-page";
//	}
//}
