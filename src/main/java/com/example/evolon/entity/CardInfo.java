package com.example.evolon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import com.example.evolon.domain.enums.CardCondition;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * カード固有情報エンティティ
 */
@Entity
@Table(name = "card_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardInfo {

	// =========================
	// 主キー
	// =========================
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// =========================
	// ★ Item 側が逆なので、ここが owner
	// =========================
	@OneToOne
	@JoinColumn(name = "item_id", nullable = false)
	private Item item;

	// =========================
	// カード情報
	// =========================
	@Column(nullable = false)
	private String cardName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Rarity rarity;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Regulation regulation;

	@Column
	private String packName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CardCondition condition;
}
