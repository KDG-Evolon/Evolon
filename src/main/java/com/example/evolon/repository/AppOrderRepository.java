package com.example.evolon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.User;

@Repository
public interface AppOrderRepository extends JpaRepository<AppOrder, Long> {

	// 買い手で注文一覧を取得
	List<AppOrder> findByBuyer(User buyer);

	// 出品者で注文一覧を取得（Item の seller 経由）
	List<AppOrder> findByItem_Seller(User seller);

	// PaymentIntent ID で注文を取得
	Optional<AppOrder> findByPaymentIntentId(String paymentIntentId);

	// 直近の注文を作成日時の降順で取得（上位5件）
	List<AppOrder> findTop5ByOrderByCreatedAtDesc();
}
