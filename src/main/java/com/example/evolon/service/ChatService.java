package com.example.evolon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

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

	public List<Chat> getChatMessagesByItem(Long itemId) {
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));
		return chatRepository.findByItemOrderByCreatedAtAsc(item);
	}

	public Chat sendMessage(Long itemId, User sender, String message) {
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));

		Chat chat = new Chat();
		chat.setItem(item);
		chat.setSender(sender);
		chat.setMessage(message);
		chat.setCreatedAt(LocalDateTime.now());

		Chat savedChat = chatRepository.save(chat);

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
