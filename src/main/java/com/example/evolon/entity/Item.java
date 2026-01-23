package com.example.evolon.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.example.evolon.domain.enums.ShippingDuration;
import com.example.evolon.domain.enums.ShippingFeeBurden;
import com.example.evolon.domain.enums.ShippingMethod;
import com.example.evolon.domain.enums.ShippingRegion;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

	// =========================
	// 主キー
	// =========================
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// =========================
	// 出品者
	// =========================
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User seller;

	// =========================
	// 基本情報
	// =========================
	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private BigDecimal price;

	@ManyToOne
	@JoinColumn(name = "category_id")
	private Category category;

	// =========================
	// 発送情報
	// =========================
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ShippingDuration shippingDuration;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ShippingFeeBurden shippingFeeBurden;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ShippingRegion shippingRegion;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ShippingMethod shippingMethod;

	// =========================
	// ★ カード情報（逆側）
	// =========================
	@OneToOne(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
	private CardInfo cardInfo;

	// =========================
	// ステータス
	// =========================
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ItemStatus status = ItemStatus.SELLING;

	private String imageUrl;
	@Column(name = "image_url2")
	private String imageUrl2;

	@Column(name = "image_url3")
	private String imageUrl3;

	@Column(name = "image_url4")
	private String imageUrl4;

	@Column(name = "image_url5")
	private String imageUrl5;

	@Column(name = "image_url6")
	private String imageUrl6;

	@Column(name = "image_url7")
	private String imageUrl7;

	@Column(name = "image_url8")
	private String imageUrl8;

	@Column(nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	// =========================
	// ドメインロジック
	// =========================
	/**
	 * 購入可能かどうか	 */
	public boolean canBePurchased() {
		return status.isPurchasable();
	}

	/**
	 * 購入済み（売り切れ）かどうか
	 */
	public boolean isSold() {
		return this.status == ItemStatus.SOLD;
	}

	/**
	 * 売り切れにする
	 */
	public void markAsSold() {
		this.status = ItemStatus.SOLD;
	}
}
