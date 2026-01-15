package com.example.evolon.domain.enums;

public enum Regulation {
	STANDARD("スタンダード"), EXTRA("エクストラ");

	private final String label;

	Regulation(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
