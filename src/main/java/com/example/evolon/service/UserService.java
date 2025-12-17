package com.example.evolon.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
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

	/** パスワードエンコーダ */
	private final PasswordEncoder passwordEncoder;

	/** コンストラクタインジェクション（1つだけ！） */
	public UserService(UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
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
		return userRepository.findByEmailIgnoreCase(email);
	}

	/** 新規作成・更新（管理側などで使用） */
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

	/** 新規ユーザー登録専用メソッド */
	@Transactional
	public User register(String name, String email, String rawPassword) {

		// メール重複チェック
		if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
			throw new IllegalArgumentException("このメールアドレスは既に登録されています");
		}

		User user = new User();
		user.setName(name);
		user.setEmail(email);
		user.setPassword(passwordEncoder.encode(rawPassword)); // 暗号化必須
		user.setRole("USER");
		user.setEnabled(true);
		user.setBanned(false);

		return userRepository.save(user);
	}

	@Transactional
	public String createResetToken(String email) {
		User user = userRepository.findByEmailIgnoreCase(email)
				.orElseThrow(() -> new IllegalArgumentException("not found"));

		String token = UUID.randomUUID().toString();
		user.setResetToken(token);
		user.setResetTokenExpiresAt(LocalDateTime.now().plusMinutes(30));

		userRepository.save(user);
		return token;
	}

	@Transactional
	public void resetPassword(String token, String newPassword) {

		User user = userRepository.findByResetToken(token)
				.orElseThrow(() -> new IllegalArgumentException("トークンが無効です"));

		if (user.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("トークンの有効期限が切れています");
		}

		user.setPassword(passwordEncoder.encode(newPassword));
		user.setResetToken(null);
		user.setResetTokenExpiresAt(null);

		userRepository.save(user);
	}

}
