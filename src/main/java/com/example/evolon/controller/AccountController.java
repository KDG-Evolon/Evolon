package com.example.evolon.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.evolon.service.UserService;

@Controller
@RequestMapping("/account")
public class AccountController {

	private final UserService userService;

	public AccountController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping("/delete")
	public String deleteAccount(
			@AuthenticationPrincipal org.springframework.security.core.userdetails.User principal,
			HttpServletRequest request) {

		userService.deactivateAccountByEmail(principal.getUsername());

		try {
			request.logout();
		} catch (Exception ignored) {
		}

		return "redirect:/login?deleted";
	}
}
