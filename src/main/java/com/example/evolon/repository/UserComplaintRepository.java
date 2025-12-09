// src/main/java/com/example/fleamarketsystem/repository/UserComplaintRepository.java
package com.example.evolon.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.evolon.entity.UserComplaint;

// UserComplaint テーブルへアクセスする Repository
public interface UserComplaintRepository extends JpaRepository<UserComplaint, Long> {
	// 指定ユーザーに対する通報件数を取得
	long countByReportedUserId(Long reportedUserId);

	// 通報対象ユーザーの通報履歴を作成日時降順で取得
	List<UserComplaint> findByReportedUserIdOrderByCreatedAtDesc(Long reportedUserId);
}