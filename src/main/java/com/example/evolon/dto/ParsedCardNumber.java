package com.example.evolon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ParsedCardNumber {

	private String setCode;
	private String cardNumber;

	public static ParsedCardNumber invalid() {
		return new ParsedCardNumber(null, null);
	}

	public boolean isValid() {
		return setCode != null
				&& cardNumber != null
				&& !setCode.isBlank()
				&& !cardNumber.isBlank();
	}
}
