package com.example.evolon.entity;

//日時型
import java.time.LocalDateTime;

//JPA 関連インポート
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

//Lombok でメソッド自動生成
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//JPA エンティティ指定
@Entity
//テーブル名 favorite_item を使用
@Table(name = "favorite_item")
//Lombok：getter/setter 等
@Data
//Lombok：デフォルトコンストラクタ
@NoArgsConstructor
//Lombok：全フィールドコンストラクタ
@AllArgsConstructor
public class FavoriteItem {
	//主キー
	@Id
	//IDENTITY で自動採番
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	//お気に入り登録したユーザー。NULL 禁止
	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
	//お気に入り対象の商品。NULL 禁止
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;
	//お気に入り登録日時。既定で現在時刻
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
