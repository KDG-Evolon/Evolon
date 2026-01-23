package com.example.evolon.service;

// コレクション操作のための import
import java.util.List;
// Stream で変換するための import
import java.util.stream.Collectors;

// サービスアノテーションの import
import org.springframework.stereotype.Service;
// トランザクション境界を宣言するための import
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.FavoriteItem;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.User;
import com.example.evolon.repository.FavoriteItemRepository;
import com.example.evolon.repository.ItemRepository;

// サービス層としての宣言
@Service
public class FavoriteService {

	// お気に入りリポジトリ
	private final FavoriteItemRepository favoriteItemRepository;
	// 商品リポジトリ
	private final ItemRepository itemRepository;

	// 依存性をコンストラクタで注入
	public FavoriteService(
			FavoriteItemRepository favoriteItemRepository,
			ItemRepository itemRepository) {
		this.favoriteItemRepository = favoriteItemRepository;
		this.itemRepository = itemRepository;
	}

	/**
	 * お気に入り追加
	 * ・同一ユーザ × 同一商品は一意
	 * ・購入済み商品はお気に入り不可
	 */
	@Transactional
	public FavoriteItem addFavorite(User user, Long itemId) {

		// 商品存在チェック
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));

		// ===== 購入済みガード（最重要） =====
		// 購入済みの商品はお気に入りできない
		if (item.isSold()) {
			throw new IllegalStateException("購入済みの商品はお気に入りできません。");
		}

		// 既に登録済みならエラー（二重登録防止）
		if (favoriteItemRepository.existsByUserAndItem(user, item)) {
			throw new IllegalStateException("Item is already favorited by this user.");
		}

		// 新規お気に入りエンティティ作成
		FavoriteItem favoriteItem = new FavoriteItem();
		favoriteItem.setUser(user);
		favoriteItem.setItem(item);

		// 保存して返す
		return favoriteItemRepository.save(favoriteItem);
	}

	/**
	 * お気に入り解除
	 * ・購入済み商品は解除操作も不可（購入後は非表示/制限する方針）
	 */
	@Transactional
	public void removeFavorite(User user, Long itemId) {

		// 商品存在チェック
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));

		// ===== 購入済みガード =====
		// 購入済みの商品はお気に入り操作不可
		if (item.isSold()) {
			throw new IllegalStateException("購入済みの商品のお気に入りは変更できません。");
		}

		// ユーザ × 商品でお気に入り取得（存在しなければエラー）
		FavoriteItem favoriteItem = favoriteItemRepository.findByUserAndItem(user, item)
				.orElseThrow(() -> new IllegalStateException("Favorite not found."));

		// 削除実行
		favoriteItemRepository.delete(favoriteItem);
	}

	/**
	 * 指定ユーザが指定商品をお気に入りしているかどうか
	 */
	public boolean isFavorited(User user, Long itemId) {

		// 商品存在チェック
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));

		// お気に入り存在判定
		return favoriteItemRepository.existsByUserAndItem(user, item);
	}

	/**
	 * ユーザのお気に入り商品一覧を返す
	 */
	public List<Item> getFavoriteItemsByUser(User user) {

		// FavoriteItem → Item にマッピングして返却
		return favoriteItemRepository.findByUser(user).stream()
				.map(FavoriteItem::getItem)
				.collect(Collectors.toList());
	}
}
