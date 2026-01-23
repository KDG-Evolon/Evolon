package com.example.evolon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.OrderMessage;

public interface OrderMessageRepository
		extends JpaRepository<OrderMessage, Long> {

	// ★ sender を join fetch して安全に view へ渡す
	@Query("""
			    select om
			    from OrderMessage om
			    join fetch om.sender
			    where om.order = :order
			    order by om.createdAt asc
			""")
	List<OrderMessage> findWithSenderByOrderOrderByCreatedAtAsc(
			@Param("order") AppOrder order);
}
