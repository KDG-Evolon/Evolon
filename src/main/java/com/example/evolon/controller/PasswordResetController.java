package com.example.evolon.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.evolon.service.UserService;

@Controller
@RequestMapping("/password")
public class PasswordResetController {

	private final UserService userService;

	public PasswordResetController(UserService userService) {
		this.userService = userService;
	}

	// パスワード忘れた画面表示
	@GetMapping("/forgot")
	public String forgotForm() {
		return "password_forgot";
	}

	// メールアドレス送信 → 即リセット画面へ（課題用）
	@PostMapping("/forgot")
	public String sendReset(@RequestParam String email, Model model) {

		String token;
		try {
			token = userService.createResetToken(email);
		} catch (IllegalArgumentException e) {
			model.addAttribute("error", "メールアドレスが見つかりません。");
			return "password_forgot";
		}

		// そのままリセット画面へ遷移
		model.addAttribute("token", token);
		return "password_reset";
	}

	// （本来はメールリンク用だが残してOK）
	@GetMapping("/reset")
	public String resetForm(@RequestParam String token, Model model) {
		model.addAttribute("token", token);
		return "password_reset";
	}

	// パスワード更新処理
	@PostMapping("/reset")
	public String resetPassword(
			@RequestParam String token,
			@RequestParam String password,
			@RequestParam String confirmPassword,
			Model model) {

		if (!password.equals(confirmPassword)) {
			model.addAttribute("token", token);
			model.addAttribute("error", "パスワードが一致しません");
			return "password_reset";
		}

		userService.resetPassword(token, password);
		return "password_reset_complete";
	}
}
