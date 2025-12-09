package com.example.evolon.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * レビュー情報を表す JPA エンティティ
 */
@Entity
@Table(name = "review")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {

	/** 主キー */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 注文に対するレビュー（1対1） */
	@OneToOne
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private AppOrder order;

	/** レビュアー（多対一） */
	@ManyToOne
	@JoinColumn(name = "reviewer_id", nullable = false)
	private User reviewer;

	/** 出品者（多対一） */
	@ManyToOne
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;

	/** 商品（多対一） */
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	/** レビュー点数（1〜5） */
	@Column(nullable = false)
	private Integer rating;

	/** レビュー本文（長文対応） */
	@Column(columnDefinition = "TEXT")
	private String comment;

	/** レビュー作成日時 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
