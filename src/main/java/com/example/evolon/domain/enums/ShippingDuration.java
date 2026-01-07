package com.example.evolon.domain.enums;

public enum ShippingDuration {

	ONE_TO_TWO_DAYS("1~2日で発送"), TWO_TO_THREE_DAYS("2~3日で発送"), FOUR_TO_SEVEN_DAYS("4~7日で発送");

	private final String label;

	ShippingDuration(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;

	}

}
