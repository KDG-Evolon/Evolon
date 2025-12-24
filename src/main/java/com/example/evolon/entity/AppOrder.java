package com.example.evolon.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

	/** 商品 */
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	/** 購入者 */
	@ManyToOne
	@JoinColumn(name = "buyer_id", nullable = false)
	private User buyer;

	/** 支払金額 */
	@Column(nullable = false)
	private BigDecimal price;

	/** 旧：文字列ステータス（互換用） */
	@Column(name = "status", nullable = false)
	private String status = OrderStatus.PAYMENT_PENDING.getLabel();

	/** 新：enum ステータス（本命） */
	@Enumerated(EnumType.STRING)
	@Column(name = "order_status", nullable = false)
	private OrderStatus orderStatus = OrderStatus.PAYMENT_PENDING;

	/** 注文作成日時 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	/** 発送日時 */
	@Column(name = "shipped_at")
	private LocalDateTime shippedAt;

	/** 到着確認日時 */
	@Column(name = "delivered_at")
	private LocalDateTime deliveredAt;

	/** キャンセル日時 */
	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	/** キャンセル実行者 */
	@Enumerated(EnumType.STRING)
	@Column(name = "cancelled_by")
	private CancelledBy cancelledBy;

	/** Stripe PaymentIntent ID */
	@Column(name = "payment_intent_id", unique = true)
	private String paymentIntentId;

	// ==============================
	// 状態遷移メソッド
	// ==============================

	/** 決済完了 → 購入済 */
	public void completePurchase() {
		if (this.orderStatus != OrderStatus.PAYMENT_PENDING) {
			throw new IllegalStateException("決済完了できない状態です");
		}
		this.orderStatus = OrderStatus.PURCHASED;
		this.status = OrderStatus.PURCHASED.getLabel();
	}

	/** 発送 */
	public void ship(LocalDateTime shippedAt) {
		if (this.orderStatus != OrderStatus.PURCHASED) {
			throw new IllegalStateException("発送できない状態です");
		}
		this.orderStatus = OrderStatus.SHIPPED;
		this.status = OrderStatus.SHIPPED.getLabel();
		this.shippedAt = shippedAt;
	}

	/** 到着確認 */
	public void deliver(LocalDateTime deliveredAt) {
		if (this.orderStatus != OrderStatus.SHIPPED) {
			throw new IllegalStateException("到着確認できない状態です");
		}
		this.orderStatus = OrderStatus.DELIVERED;
		this.status = OrderStatus.DELIVERED.getLabel();
		this.deliveredAt = deliveredAt;
	}

	/** キャンセル */
	public void cancel(CancelledBy cancelledBy, LocalDateTime cancelledAt) {
		if (this.orderStatus != OrderStatus.PURCHASED) {
			throw new IllegalStateException("キャンセルできない状態です");
		}
		this.orderStatus = OrderStatus.CANCELLED;
		this.status = OrderStatus.CANCELLED.getLabel();
		this.cancelledBy = cancelledBy;
		this.cancelledAt = cancelledAt;
	}

}
