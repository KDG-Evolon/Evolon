package com.example.evolon.service;

import org.springframework.data.domain.Sort;

/**
 * 並び替え（sort）文字列を Spring Data の Sort に変換するユーティリティ。
 *
 * item_list.html の select が
 *   new / priceAsc / priceDesc
 * を送る想定で対応している。
 */
public class ItemSortHelper {

	/**
	 * sort文字列を Sort に変換する
	 *
	 * @param sort "new" / "priceAsc" / "priceDesc" など
	 * @return Sort
	 */
	public static Sort toSort(String sort) {

		// 未指定や空は新着順に寄せる
		if (sort == null || sort.isBlank()) {
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}

		switch (sort) {
		case "priceAsc":
			return Sort.by(Sort.Direction.ASC, "price");
		case "priceDesc":
			return Sort.by(Sort.Direction.DESC, "price");
		case "new":
		default:
			// createdAt が無いなら "id" に変えてOK
			return Sort.by(Sort.Direction.DESC, "createdAt");
		}
	}

	// インスタンス化させない（ユーティリティクラス）
	private ItemSortHelper() {
	}
}
