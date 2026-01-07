package com.example.evolon.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "review", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
@Data
@NoArgsConstructor
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// order_id
	@OneToOne
	@JoinColumn(name = "order_id", nullable = false)
	private AppOrder order;

	// reviewer_id（購入者）
	@ManyToOne
	@JoinColumn(name = "reviewer_id", nullable = false)
	private User reviewer;

	// ✅ 新：reviewee_id（出品者の評価対象）
	@ManyToOne
	@JoinColumn(name = "reviewee_id", nullable = false)
	private User reviewee;

	// ✅ 旧：seller_id（DB互換のため残す）
	@ManyToOne
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	// ✅ 旧：item_id（DB互換のため残す）
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// ✅ 新：result（varchar でも Enum でもOK。今は Enumで扱う想定）
	@Enumerated(EnumType.STRING)
	@Column(name = "result", nullable = false, length = 10)
	private ReviewResult result; // GOOD / BAD

	// ✅ 旧：rating（DBが NOT NULL なら必須）
	@Column(name = "rating", nullable = false)
	private Integer rating;

	// comment（DBは text で NULL許容っぽいけど、アプリ側で必須にするなら nullable=false でOK）
	@Column(name = "comment", nullable = false)
	private String comment;

	// created_at（DBで DEFAULT CURRENT_TIMESTAMP ならDBに任せるのが安全）
	@Column(name = "created_at", insertable = false, updatable = false)
	private LocalDateTime createdAt;
}
