package com.example.evolon.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.User;
import com.example.evolon.entity.UserComplaint;
import com.example.evolon.repository.UserComplaintRepository;
import com.example.evolon.repository.UserRepository;

@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final UserComplaintRepository complaintRepository;

	public AdminUserService(UserRepository userRepository,
			UserComplaintRepository complaintRepository) {
		this.userRepository = userRepository;
		this.complaintRepository = complaintRepository;
	}

	/** 全ユーザー一覧 */
	public List<User> listAllUsers() {
		return userRepository.findAll();
	}

	/** ID でユーザー取得。見つからなければ例外 */
	public User findUser(Long id) {
		return userRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("User not found: " + id));
	}

	/** ユーザー平均評価（null → 0.0） */
	public Double averageRating(Long userId) {
		Double avg = userRepository.averageRatingForUser(userId);
		return (avg == null) ? 0.0 : avg;
	}

	/** 通報件数 */
	public long complaintCount(Long userId) {
		return complaintRepository.countByReportedUserId(userId);
	}

	/** 通報詳細（新しい順） */
	public List<UserComplaint> complaints(Long userId) {
		return complaintRepository.findByReportedUserIdOrderByCreatedAtDesc(userId);
	}

	/** BAN 処理（ログイン無効化オプション付き） */
	@Transactional
	public void banUser(Long targetUserId, Long adminUserId, String reason, boolean disableLogin) {
		User u = findUser(targetUserId);

		u.setBanned(true);
		u.setBanReason(reason);
		u.setBannedAt(LocalDateTime.now());
		u.setBannedByAdminId(adminUserId == null ? null : adminUserId.intValue());

		if (disableLogin) {
			u.setEnabled(false);
		}

		userRepository.save(u);
	}

	/** BAN 解除 */
	@Transactional
	public void unbanUser(Long targetUserId) {
		User u = findUser(targetUserId);

		u.setBanned(false);
		u.setBanReason(null);
		u.setBannedAt(null);
		u.setBannedByAdminId(null);
		u.setEnabled(true);

		userRepository.save(u);
	}
}
