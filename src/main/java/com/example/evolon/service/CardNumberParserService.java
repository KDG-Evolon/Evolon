package com.example.evolon.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.evolon.dto.ParsedCardNumber;

@Service
public class CardNumberParserService {

	private static final Pattern SET_CODE_PATTERN = Pattern.compile("sv[0-9]+[a-z]");

	private static final Pattern CARD_NUMBER_PATTERN = Pattern.compile("\\d{1,3}\\s*/\\s*\\d{1,3}");

	public ParsedCardNumber parse(String text) {

		String normalized = normalize(text);

		String setCode = extract(SET_CODE_PATTERN, normalized);
		String cardNumber = extract(CARD_NUMBER_PATTERN, normalized);

		return new ParsedCardNumber(setCode, cardNumber);
	}

	private String normalize(String text) {
		return text
				.toLowerCase()
				.replaceAll("[\\s　]+", "") // 半角・全角スペースをまとめて削除
				.replace("／", "/"); // 全角スラッシュを半角に
	}

	private String extract(Pattern pattern, String text) {
		Matcher matcher = pattern.matcher(text);
		return matcher.find() ? matcher.group() : null;
	}
}
