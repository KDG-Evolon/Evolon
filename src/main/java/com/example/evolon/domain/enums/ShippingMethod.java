package com.example.evolon.domain.enums;

public enum ShippingMethod {

	YAMATO("ヤマト運輸"), SAGAWA("佐川急便"), JAPAN_POST("日本郵便"), OTHER("その他");

	private final String label;

	ShippingMethod(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
