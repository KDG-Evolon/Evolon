package com.example.evolon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.OrderStatus;
import com.example.evolon.entity.User;

@Repository
public interface AppOrderRepository extends JpaRepository<AppOrder, Long> {

	List<AppOrder> findByBuyer(User buyer);

	List<AppOrder> findByItem_Seller(User seller);

	List<AppOrder> findByBuyerAndOrderStatus(User buyer, OrderStatus orderStatus);

	List<AppOrder> findByItem_SellerAndOrderStatus(User seller, OrderStatus orderStatus);

	List<AppOrder> findByOrderStatus(OrderStatus orderStatus);

	Optional<AppOrder> findByPaymentIntentId(String paymentIntentId);

	List<AppOrder> findTop5ByOrderByCreatedAtDesc();

	List<AppOrder> findByBuyerAndOrderStatusIn(User buyer, List<OrderStatus> statuses);
}
