package com.example.evolon.dto;

public class ParsedCardNumber {

	private final String setCode;
	private final String cardNumber;

	public ParsedCardNumber(String setCode, String cardNumber) {
		this.setCode = setCode;
		this.cardNumber = cardNumber;
	}

	public String getSetCode() {
		return setCode;
	}

	public String getCardNumber() {
		return cardNumber;
	}

	public boolean isValid() {
		return setCode != null && cardNumber != null;
	}
}
