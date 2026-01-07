package com.example.evolon.domain.enums;

public enum ShippingFeeBurden {
	SELLER("送料込み(出品者負担)"), BUYER("着払い(購入者負担)");

	private final String label;

	ShippingFeeBurden(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

}
