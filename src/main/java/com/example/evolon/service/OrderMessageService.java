package com.example.evolon.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.OrderMessage;
import com.example.evolon.entity.User;
import com.example.evolon.repository.OrderMessageRepository;

@Service
public class OrderMessageService {

	private final OrderMessageRepository repo;

	public OrderMessageService(OrderMessageRepository repo) {
		this.repo = repo;
	}

	public List<OrderMessage> getMessages(AppOrder order) {
		return repo.findWithSenderByOrderOrderByCreatedAtAsc(order);
	}

	public void send(AppOrder order, User sender, String message) {
		OrderMessage m = new OrderMessage();
		m.setOrder(order);
		m.setSender(sender);
		m.setMessage(message);
		repo.save(m);
	}
}
