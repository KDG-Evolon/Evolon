package com.example.evolon.dto;

import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;

public class CardAutoFillResponse {

	private String cardName;
	private Rarity rarity;
	private String packName;
	private Regulation regulation;

	public CardAutoFillResponse(
			String cardName,
			Rarity rarity,
			String packName,
			Regulation regulation) {
		this.cardName = cardName;
		this.rarity = rarity;
		this.packName = packName;
		this.regulation = regulation;
	}

	public String getCardName() {
		return cardName;
	}

	public Rarity getRarity() {
		return rarity;
	}

	public String getPackName() {
		return packName;
	}

	public Regulation getRegulation() {
		return regulation;
	}
}
