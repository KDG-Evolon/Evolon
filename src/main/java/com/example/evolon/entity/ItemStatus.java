package com.example.evolon.entity;

public enum ItemStatus {

	SELLING("出品中", true),

	PAYMENT_DONE("発送待ち", false),

	SOLD("取引完了", false), SUSPENDED("停止中", false);

	private final String label;
	private final boolean purchasable;

	ItemStatus(String label, boolean purchasable) {
		this.label = label;
		this.purchasable = purchasable;
	}

	public String getLabel() {
		return label;
	}

	public boolean isPurchasable() {
		return purchasable;
	}
}
