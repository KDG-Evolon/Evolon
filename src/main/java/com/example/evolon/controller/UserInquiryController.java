package com.example.evolon.controller;

import java.security.Principal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.evolon.entity.Inquiry;
import com.example.evolon.entity.User;
import com.example.evolon.repository.UserRepository;
import com.example.evolon.service.InquiryService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/inquiry")
public class UserInquiryController {

	private final InquiryService inquiryService;
	private final UserRepository userRepository; // User検索用

	// フォーム表示
	@GetMapping("/new")
	public String showForm(Model model) {
		model.addAttribute("inquiry", new Inquiry());
		return "inquiry_new"; // 修正済み
	}

	@PostMapping("/new")
	public String submitInquiry(@ModelAttribute Inquiry inquiry, Principal principal) {
		// Principalからユーザー名（email）を取得
		String email = principal.getName();

		// DBからUserを取得（大文字小文字無視）
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		// InquiryにUserをセット
		inquiry.setUser(user);

		// 念のためステータスと作成日時もセット
		inquiry.setStatus("OPEN");
		inquiry.setCreatedAt(LocalDateTime.now());

		// ServiceにInquiryを渡す
		inquiryService.createInquiry(inquiry);

		return "redirect:/inquiry/complete";
	}

	// 完了ページ
	@GetMapping("/complete")
	public String complete() {
		return "inquiry_complete"; // 修正済み
	}
}
