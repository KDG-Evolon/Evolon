package com.example.evolon.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.evolon.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	// ログイン用
	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByResetToken(String token);

}
