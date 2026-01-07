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
import com.example.evolon.service.ItemService;
import com.example.evolon.service.UserService;

// このクラスが Web リクエストを処理するコントローラであることを示す
@Controller
// このクラスで扱う URL パスの先頭を /chat に固定する
@RequestMapping("/chat")
public class ChatController {
	// チャットメッセージに関するビジネスロジックを扱うサービス
	private final ChatService chatService;
	// 商品情報取得などを行うサービス
	private final ItemService itemService;
	// ユーザー情報取得などを行うサービス
	private final UserService userService;

	// 必要なサービスをコンストラクタインジェクションで受け取る
	public ChatController(ChatService chatService, ItemService itemService, UserService userService) {
		// 引数で受け取った ChatService をフィールドに設定
		this.chatService = chatService;
		// 引数で受け取った ItemService をフィールドに設定
		this.itemService = itemService;
		// 引数で受け取った UserService をフィールドに設定
		this.userService = userService;
	}

	// 指定された商品に紐づくチャット画面を表示するハンドラ（GET /chat/{itemId}）
	@GetMapping("/{itemId}")
	public String showChatScreen(@PathVariable("itemId") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {
		// 商品 ID から商品情報を取得し、存在しなければ例外を投げる
		var item = itemService.getItemById(itemId)
				.orElseThrow(() -> new RuntimeException("Item not found"));
		model.addAttribute("item", item);

		// 商品の出品者とログインユーザーを比較して isOwner をセット
		boolean isOwner = false;
		if (userDetails != null && item.getSeller() != null && item.getSeller().getEmail() != null) {
			isOwner = item.getSeller().getEmail().equals(userDetails.getUsername());
		}
		model.addAttribute("isOwner", isOwner);

		// 対象商品のチャットメッセージ一覧を取得して Model に登録
		model.addAttribute("chats", chatService.getChatMessagesByItem(itemId));

		// 商品詳細画面テンプレート（item_detail.html）を再利用してチャットを表示
		return "item_detail"; // Re-use item_detail for chat display
	}

	// 指定された商品に対するチャットメッセージ送信を処理するハンドラ（POST /chat/{itemId}）
	@PostMapping("/{itemId}")
	public String sendMessage(
			// パスから対象商品 ID を取得
			@PathVariable("itemId") Long itemId,
			// ログイン中のユーザー情報を Spring Security から取得
			@AuthenticationPrincipal UserDetails userDetails,
			// フォームから送信されたメッセージ本文を取得
			@RequestParam("message") String message) {

		// userDetails が null の場合は送信を防ぐ
		if (userDetails == null) {
			throw new RuntimeException("未ログインユーザーによる送信は許可されていません");
		}

		// ログインユーザーのメールアドレスから User エンティティを取得（存在しなければ例外）
		User sender = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Sender not found"));

		// サービスを通じてチャットメッセージを保存・送信処理
		chatService.sendMessage(itemId, sender, message);

		// 同じ商品のチャット画面へリダイレクトし、最新のメッセージ一覧を再表示
		return "redirect:/chat/{itemId}";
	}
}
