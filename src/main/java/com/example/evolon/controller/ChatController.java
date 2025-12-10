package com.example.evolon.controller;

//ログインユーザをメソッド引数で受けるためのアノテーション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//Spring Security のユーザ詳細型
import org.springframework.security.core.userdetails.UserDetails;
//MVC コントローラを示すアノテーション
import org.springframework.stereotype.Controller;
//画面に値を渡すための Model
import org.springframework.ui.Model;
//ルーティングアノテーション群
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.evolon.entity.User;
//チャット機能のビジネスロジックサービス
import com.example.evolon.service.ChatService;
//商品取得などで使うサービス
import com.example.evolon.service.ItemService;
//認証ユーザの実体を取得するためのサービス
import com.example.evolon.service.UserService;

//MVC コントローラとして登録
@Controller
//ベースパス /chat にマッピング
@RequestMapping("/chat")
public class ChatController {
	//チャットサービスの参照
	private final ChatService chatService;
	//商品サービスの参照
	private final ItemService itemService;
	//ユーザサービスの参照
	private final UserService userService;

	//必要な依存をコンストラクタで受け取る
	public ChatController(ChatService chatService, ItemService itemService, UserService userService) {
		//フィールドへ代入
		this.chatService = chatService;
		//フィールドへ代入
		this.itemService = itemService;
		//フィールドへ代入
		this.userService = userService;
	}

	//商品単位のチャット画面を表示
	@GetMapping("/{itemId}")
	public String showChatScreen(@PathVariable("itemId") Long itemId, Model model) {
		//商品を取得（見つからなければ例外送出）
		model.addAttribute("item", itemService.getItemById(itemId)
				.orElseThrow(() -> new RuntimeException("Item not found")));
		//チャット履歴を昇順で取得
		model.addAttribute("chats", chatService.getChatMessagesByItem(itemId));
		//チャットは商品詳細テンプレートに埋め込んで表示
		return "item_detail";
	}

	//チャットの新規メッセージ送信
	@PostMapping("/{itemId}")
	public String sendMessage(
			//パスから商品 ID を受け取る
			@PathVariable("itemId") Long itemId,
			//認証済みユーザを受け取る
			@AuthenticationPrincipal UserDetails userDetails,
			//送信するメッセージ本文を受け取る
			@RequestParam("message") String message) {
		//ログインユーザをメールで特定（見つからなければ例外）
		User sender = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Sender not found"));
		//サービス層に送信処理を委譲（通知などはサービス側で実施）
		chatService.sendMessage(itemId, sender, message);
		//送信後は同じチャット画面へ遷移
		return "redirect:/chat/{itemId}";
	}
}
