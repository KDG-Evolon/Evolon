package com.example.evolon.entity;

//日時型
import java.time.LocalDateTime;

//JPA インポート
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

//Lombok インポート
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//JPA エンティティ宣言
@Entity
//テーブル名 review を指定
@Table(name = "review")
//Lombok：getter/setter 等自動生成
@Data
//Lombok：引数なしコンストラクタ
@NoArgsConstructor
//Lombok：全フィールドコンストラクタ
@AllArgsConstructor
public class Review {
	//主キー
	@Id
	//IDENTITY 戦略
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	//一つの注文に対してレビューは 1 件（ユニーク制約）→ OneToOne
	@OneToOne
	@JoinColumn(name = "order_id", nullable = false, unique = true)
	private AppOrder order;
	//レビューワ（購入者）。複数レビューに同一ユーザーが存在し得るため ManyToOne
	@ManyToOne
	@JoinColumn(name = "reviewer_id", nullable = false)
	private User reviewer;
	//出品者（被評価者）
	@ManyToOne
	@JoinColumn(name = "seller_id", nullable = false)
	private User seller;
	//対象商品
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;
	//評価点（1〜5 を想定）
	@Column(nullable = false)
	private Integer rating;
	//コメント本文（任意）→TEXT 型
	@Column(columnDefinition = "TEXT")
	private String comment;
	//作成日時（既定は現在時刻）
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
