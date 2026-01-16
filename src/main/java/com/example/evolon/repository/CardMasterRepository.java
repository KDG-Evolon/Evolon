package com.example.evolon.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.evolon.entity.CardMaster;

@Repository
public interface CardMasterRepository extends JpaRepository<CardMaster, Long> {

	Optional<CardMaster> findBySetCodeAndCardNumber(
			String setCode,
			String cardNumber);

	boolean existsBySetCodeAndCardNumber(String setCode, String cardNumber);
}
