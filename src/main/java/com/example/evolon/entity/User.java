package com.example.evolon.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity // JPA 管理対象のエンティティ
@Table(name = "users") // DB テーブル名
@Data // Getter/Setter, toString など自動生成
@NoArgsConstructor
@AllArgsConstructor
public class User {
	@Id // 主キー
	@GeneratedValue(strategy = GenerationType.IDENTITY) // Auto Increment
	private Long id; // ユーザーID
	@Column(nullable = false)
	private String name; // ユーザー名（表示名）
	@Column(unique = true, nullable = false)
	private String email; // ログイン ID として使用するメールアドレス
	@Column(nullable = false)
	private String password; // パスワード（暗号化保存）
	@Column(nullable = false)
	private String role; // ロール（USER / ADMIN）
	@Column(name = "line_notify_token")
	private String lineNotifyToken; // LINE 通知用トークン（任意）
	@Column(nullable = false)
	private boolean enabled = true; // アカウント有効フラグ（false = 退会/停止）
	@Column(nullable = false)
	private boolean banned = false; // 強制 BAN フラグ（true=停止）
	@Column(name = "ban_reason")
	private String banReason; // BAN 理由
	@Column(name = "banned_at")
	private LocalDateTime bannedAt; // BAN 日時
	@Column(name = "banned_by_admin_id")
	private Integer bannedByAdminId; // BAN 実行管理者の ID
}