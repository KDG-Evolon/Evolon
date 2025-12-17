package com.example.evolon.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.evolon.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	// ログイン用
	Optional<User> findByEmailIgnoreCase(String email);

	@Query("""
			    SELECT AVG(r.rating)
			    FROM Review r
			    WHERE r.seller.id = :userId
			""")
	Double averageRatingForUser(@Param("userId") Long userId);

	Optional<User> findByResetToken(String token);

}
