// src/main/java/com/example/evolon/entity/UserComplaint.java
package com.example.evolon.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_complaint")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserComplaint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 通報されたユーザー */
	@ManyToOne
	@JoinColumn(name = "reported_user_id", nullable = false)
	private User reportedUser;

	/** 通報したユーザー */
	@ManyToOne
	@JoinColumn(name = "reporter_user_id", nullable = false)
	private User reporterUser;

	/** 通報理由 */
	@Column(nullable = false)
	private String reason;

	/** 通報日時 */
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
