package com.example.evolon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.evolon.entity.Chat;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.User;
import com.example.evolon.repository.ChatRepository;
import com.example.evolon.repository.ItemRepository;

@Service
public class ChatService {

	private final ChatRepository chatRepository;
	private final ItemRepository itemRepository;
	private final LineNotifyService lineNotifyService;

	public ChatService(ChatRepository chatRepository,
			ItemRepository itemRepository,
			LineNotifyService lineNotifyService) {
		this.chatRepository = chatRepository;
		this.itemRepository = itemRepository;
		this.lineNotifyService = lineNotifyService;
	}

	/**
	 * 商品に紐づくチャット一覧を取得（古い順）
	 */
	public List<Chat> getChatMessagesByItem(Long itemId) {
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));
		return chatRepository.findByItemOrderByCreatedAtAsc(item);
	}

	/**
	 * メッセージ送信
	 * ・購入済み（売り切れ）商品の場合は送信不可（購入後に制限する方針）
	 */
	public Chat sendMessage(Long itemId, User sender, String message) {

		// 商品存在チェック
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));

		// ===== 購入済みガード（最重要） =====
		// 購入済みの商品には質問（メッセージ送信）できない
		if (item.isSold()) {
			// IllegalStateException のままだと未ハンドル時に 500 になりやすいので、
			// 明示的に 409 Conflict を返す（状態的に操作できない）
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"購入済みの商品にはメッセージを送信できません。");
		}

		// チャット作成
		Chat chat = new Chat();
		chat.setItem(item);
		chat.setSender(sender);
		chat.setMessage(message);
		chat.setCreatedAt(LocalDateTime.now());

		// 保存
		Chat savedChat = chatRepository.save(chat);

		// 通知先（出品者）へ LINE 通知
		User receiver = item.getSeller();
		if (receiver != null && receiver.getLineNotifyToken() != null) {
			String notificationMessage = String.format(
					"\n 商品「%s」に関する新しいメッセージが届きました！\n 送信者: %s\n メッセージ: %s",
					item.getName(),
					sender.getName(),
					message);

			lineNotifyService.sendMessage(receiver.getLineNotifyToken(), notificationMessage);
		}

		return savedChat;
	}
}
