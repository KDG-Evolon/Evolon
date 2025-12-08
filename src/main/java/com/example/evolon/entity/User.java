package com.example.evolon.entity;

//JPA インポート
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//Lombok インポート
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//JPA エンティティ宣言
@Entity
//テーブル名 users を使用
@Table(name = "users")
//Lombok：getter/setter 等
@Data
//Lombok：デフォルトコンストラクタ
@NoArgsConstructor
//Lombok：全フィールドコンストラクタ
@AllArgsConstructor
public class User {
	//主キー
	@Id
	//自動採番（IDENTITY）
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	//表示名（必須）
	@Column(nullable = false)
	private String name;
	//ログイン ID に使用。ユニーク制約
	@Column(unique = true)
	private String email;
	//ハッシュ化されたパスワード（必須）
	@Column(nullable = false)
	private String password;
	//役割（USER / ADMIN）（必須）
	@Column(nullable = false)
	private String role;
	//LINE Notify のアクセストークン（任意）
	@Column(name = "line_notify_token")
	private String lineNotifyToken;
	//アカウントの有効/無効フラグ。初期値は true（有効）
	@Column(nullable = false)
	private boolean enabled = true; // New field

	public boolean isBanned() {
		// TODO 自動生成されたメソッド・スタブ
		return false;
	}
}
