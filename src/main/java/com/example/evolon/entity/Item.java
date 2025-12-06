package com.example.evolon.entity;

// 金額を表す BigDecimal と日時を利用
import java.math.BigDecimal;
import java.time.LocalDateTime; // Add this import

// JPA アノテーションの読み込み
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Lombok でボイラープレート削減
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
// JPA エンティティ指定

@Entity
// テーブル名 item を明示
@Table(name = "item")
// Lombok：getter/setter 等
@Data
// Lombok：デフォルトコンストラクタ
@NoArgsConstructor
// Lombok：全フィールドコンストラクタ
@AllArgsConstructor
public class Item {
	// 主キー
	@Id
	// 自動採番（IDENTITY）
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	// 出品者（users テーブルへの外部キー）。NULL 禁止
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User seller;
	// 商品名。NULL 禁止
	@Column(nullable = false)
	private String name;
	// 商品説明。長文想定で TEXT
	@Column(columnDefinition = "TEXT")
	private String description;
	// 価格。NULL 禁止（小数を扱うため BigDecimal）
	@Column(nullable = false)
	private BigDecimal price;
	//カテゴリ（外部キー）。NULL 可（未分類を許容）
	@ManyToOne
	@JoinColumn(name = "category_id")
	private Category category;
	//出品ステータス。初期値は「出品中」
	private String status = "出品中"; // default status
	//画像 URL（Cloudinary にアップロードした結果を格納）
	//For image URLs (Cloudinary)
	private String imageUrl;
	//作成日時。列名を created_at に固定、初期値は現在時刻
	@Column(name = "created_at", nullable = false) // New field
	private LocalDateTime createdAt = LocalDateTime.now();
}
