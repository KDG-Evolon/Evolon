package com.example.evolon.entity;

//日時型を利用するためのインポート
import java.time.LocalDateTime;

//JPA アノテーションのインポート
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

//Lombok でボイラープレート削減
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//JPA エンティティであることを示す
@Entity
//対応テーブル名 chat を指定
@Table(name = "chat")
//Lombok：getter/setter 等を自動生成
@Data
//Lombok：デフォルトコンストラクタ
@NoArgsConstructor
//Lombok：全フィールドコンストラクタ
@AllArgsConstructor
public class Chat {
	//主キー
	@Id
	//IDENTITY 戦略で自動採番
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	//対象商品の参照（外部キー item_id）。NULL 禁止
	@ManyToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;
	//送信者ユーザーの参照（外部キー sender_id）。NULL 禁止
	@ManyToOne
	@JoinColumn(name = "sender_id", nullable = false)
	private User sender;
	//本文は長文になる可能性があるため TEXT 型
	@Column(columnDefinition = "TEXT")
	private String message;
	//作成日時。列名を created_at にし、NULL を許可しない
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
