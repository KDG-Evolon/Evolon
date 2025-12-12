package com.example.evolon.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "inquiry")
public class Inquiry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 問い合わせしたユーザー
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	// 件名
	@Column(nullable = false, length = 100)
	private String subject;

	// 問い合わせ内容（ユーザー側入力）
	@Column(nullable = false, length = 1000)
	private String message;

	// 管理者の返信内容
	@Column(length = 1000)
	private String adminReply;

	// OPEN / CLOSED
	@Column(nullable = false, length = 20)
	private String status;

	// 作成日時
	@Column(nullable = false)
	private LocalDateTime createdAt;

	// 管理者が返信した日時
	private LocalDateTime repliedAt;

	// ======= Constructor =======
	public Inquiry() {
		// デフォルトコンストラクタ
	}

	// ======= Getter / Setter =======
	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getAdminReply() {
		return adminReply;
	}

	public void setAdminReply(String adminReply) {
		this.adminReply = adminReply;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getRepliedAt() {
		return repliedAt;
	}

	public void setRepliedAt(LocalDateTime repliedAt) {
		this.repliedAt = repliedAt;
	}

	// ======= Lifecycle Callback =======
	@PrePersist
	protected void onCreate() {
		this.createdAt = LocalDateTime.now();
		if (this.status == null) {
			this.status = "OPEN";
		}
	}
}
