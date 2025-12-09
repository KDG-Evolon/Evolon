package com.example.evolon.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注文情報を表す JPA エンティティ
 */
@Entity
@Table(name = "app_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppOrder {

	/** 主キー */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 商品（多対一） */
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	/** 購入者（多対一） */
	@ManyToOne
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	/** 支払金額 */
	@Column(nullable = false)
	private BigDecimal price;

	/** 注文ステータス（例：購入済 / 発送済） */
	@Column(nullable = false)
	private String status = "購入済";

	/** 注文日時（作成日時） */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	/** Stripe PaymentIntent ID */
	@Column(name = "payment_intent_id", unique = true)
	private String paymentIntentId;

	// 以下の getter/setter は Lombok の @Data ですでに生成されているが、
	// 明示的に書いても OK（そのまま残してある）
	public String getPaymentIntentId() {
		return paymentIntentId;
	}

	public void setPaymentIntentId(String paymentIntentId) {
		this.paymentIntentId = paymentIntentId;
	}
}
