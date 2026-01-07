package com.example.evolon.entity;

public enum OrderStatus {

	PAYMENT_PENDING("決済待ち"), PURCHASED("購入済"), SHIPPED("発送済"), DELIVERED("到着確認"), COMPLETED("取引完了"), CANCELLED("キャンセル");

	private final String label;

	OrderStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
