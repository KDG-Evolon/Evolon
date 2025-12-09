package com.example.evolon.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.User;
import com.example.evolon.repository.UserRepository;

/**
 * ユーザーに関するビジネスロジックを提供するサービス
 */
@Service
public class UserService {

	/** ユーザーリポジトリ */
	private final UserRepository userRepository;

	/** コンストラクタインジェクション */
	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/** 全ユーザー取得 */
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	/** ID でユーザー取得 */
	public Optional<User> getUserById(Long id) {
		return userRepository.findById(id);
	}

	/** メールアドレスでユーザー取得 */
	public Optional<User> getUserByEmail(String email) {
		return userRepository.findByEmail(email);
	}

	/** 新規作成・更新 */
	@Transactional
	public User saveUser(User user) {
		return userRepository.save(user);
	}

	/** ユーザー削除 */
	@Transactional
	public void deleteUser(Long id) {
		userRepository.deleteById(id);
	}

	/** 有効 / 無効フラグ切り替え */
	@Transactional
	public void toggleUserEnabled(Long userId) {

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		user.setEnabled(!user.isEnabled());

		userRepository.save(user);
	}
}
