package com.example.evolon.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.evolon.dto.ParsedCardNumber;
import com.example.evolon.entity.CardMaster;
import com.example.evolon.repository.CardMasterRepository;

@Service
public class CardMasterService {

	private final CardMasterRepository cardMasterRepository;

	public CardMasterService(CardMasterRepository cardMasterRepository) {
		this.cardMasterRepository = cardMasterRepository;
	}

	/**
	 * ParsedCardNumber から card_master を検索
	 */
	public Optional<CardMaster> findByParsedNumber(ParsedCardNumber parsed) {

		if (parsed == null || !parsed.isValid()) {
			return Optional.empty();
		}

		// カード番号を正規化（空白削除）
		String cardNumberNormalized = parsed.getCardNumber().replaceAll("\\s+", "");

		return cardMasterRepository.findBySetCodeAndCardNumber(
				parsed.getSetCode(),
				cardNumberNormalized);
	}

}
