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

import com.example.evolon.entity.User;
import com.example.evolon.service.ChatService;
import com.example.evolon.service.FavoriteService;
import com.example.evolon.service.ItemService;
import com.example.evolon.service.UserService;

@Controller
@RequestMapping("/chat")
public class ChatController {

	private final ChatService chatService;
	private final ItemService itemService;
	private final UserService userService;
	private final FavoriteService favoriteService;

	public ChatController(
			ChatService chatService,
			ItemService itemService,
			UserService userService,
			FavoriteService favoriteService) {
		this.chatService = chatService;
		this.itemService = itemService;
		this.userService = userService;
		this.favoriteService = favoriteService;
	}

	@GetMapping("/{itemId}")
	public String showChatScreen(
			@PathVariable("itemId") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		var item = itemService.getItemById(itemId)
				.orElseThrow(() -> new RuntimeException("Item not found"));
		model.addAttribute("item", item);

		boolean isOwner = false;
		if (userDetails != null && item.getSeller() != null && item.getSeller().getEmail() != null) {
			isOwner = item.getSeller().getEmail().equals(userDetails.getUsername());
		}
		model.addAttribute("isOwner", isOwner);

		model.addAttribute("chats", chatService.getChatMessagesByItem(itemId));

		boolean isFavorited = false;
		if (userDetails != null) {
			User user = userService.getUserByEmail(userDetails.getUsername()).orElse(null);
			if (user != null) {
				isFavorited = favoriteService.isFavorited(user, itemId);
			}
		}
		model.addAttribute("isFavorited", isFavorited);

		return "item_detail";
	}

	@PostMapping("/{itemId}")
	public String sendMessage(
			@PathVariable("itemId") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("message") String message) {

		if (userDetails == null) {
			throw new RuntimeException("未ログインユーザーによる送信は許可されていません");
		}

		User sender = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Sender not found"));

		// ★ 例外は GlobalExceptionHandler が拾う（try/catch 不要）
		chatService.sendMessage(itemId, sender, message);

		return "redirect:/items/{itemId}";
	}
}
