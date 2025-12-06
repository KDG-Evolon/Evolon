package com.example.evolon.service;

//一覧返却に使う List を import
import java.util.List;
//Optional を返すための import
import java.util.Optional;

//DI 対象のサービスを表すアノテーションを import
import org.springframework.stereotype.Service;
//変更系でトランザクションを開始するためのアノテーションを import
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.User;
import com.example.evolon.repository.UserRepository;

//サービス層として登録
@Service
public class UserService {
	//ユーザリポジトリの参照
	private final UserRepository userRepository;

	//依存性をコンストラクタで注入
	public UserService(UserRepository userRepository) {
		//フィールドへ設定
		this.userRepository = userRepository;
	}

	//すべてのユーザを取得
	public List<User> getAllUsers() {
		//全件取得を委譲
		return userRepository.findAll();
	}

	//主キーでユーザを取得
	public Optional<User> getUserById(Long id) {
		//Optional を返す
		return userRepository.findById(id);
	}

	//メールアドレスでユーザを取得
	public Optional<User> getUserByEmail(String email) {
		//Optional を返す
		return userRepository.findByEmail(email);
	}

	//新規/更新保存
	@Transactional
	public User saveUser(User user) {
		//save に委譲
		return userRepository.save(user);
	}

	//削除
	@Transactional
	public void deleteUser(Long id) {
		//ID 指定で削除
		userRepository.deleteById(id);
	}

	//有効/無効フラグのトグル
	@Transactional
	public void toggleUserEnabled(Long userId) {
		//ID でユーザを取得（なければ 400 相当の例外）
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		//既存の属性は変更せず enabled だけ反転
		user.setEnabled(!user.isEnabled());
		//保存して確定
		userRepository.save(user);
	}
}
