package com.example.evolon.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.evolon.entity.User;
import com.example.evolon.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class RegisterController {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	/**
	 * 新規登録画面表示
	 */
	@GetMapping("/register")
	public String showRegisterForm() {
		return "register";
	}

	/**
	 * 新規登録処理
	 */
	@PostMapping("/register")
	public String registerUser(
			@RequestParam String name,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam String confirmPassword,
			Model model) {

		// ① パスワード一致チェック
		if (!password.equals(confirmPassword)) {
			model.addAttribute("error", "パスワードが一致しません。");
			return "register";
		}

		// ② メールアドレス重複チェック
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			model.addAttribute("error", "このメールアドレスは既に登録されています。");
			return "register";
		}

		// ③ ユーザー作成
		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(password));
		user.setRole("USER");
		user.setEnabled(true);

		userRepository.save(user);

		// ④ 完了画面へ
		return "redirect:/register/complete";
	}

	/**
	 * 登録完了画面
	 */
	@GetMapping("/register/complete")
	public String registerComplete() {
		return "register_complete";
	}
}
