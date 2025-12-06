package com.example.evolon.service;

//コレクション操作のための import
import java.util.List;
//Stream で変換するための import
import java.util.stream.Collectors;

//サービスアノテーションの import
import org.springframework.stereotype.Service;
//トランザクション境界を宣言するための import
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.FavoriteItem;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.User;
import com.example.evolon.repository.FavoriteItemRepository;
import com.example.evolon.repository.ItemRepository;

//サービス層としての宣言
@Service
public class FavoriteService {
	//リポジトリの参照（お気に入り）
	private final FavoriteItemRepository favoriteItemRepository;
	//リポジトリの参照（商品）
	private final ItemRepository itemRepository;

	//依存性をコンストラクタで注入
	public FavoriteService(FavoriteItemRepository favoriteItemRepository, ItemRepository itemRepository) {
		//お気に入りリポジトリを設定
		this.favoriteItemRepository = favoriteItemRepository;
		//商品リポジトリを設定
		this.itemRepository = itemRepository;
	}

	//お気に入り追加（同一ユーザ×商品は一意）
	@Transactional
	public FavoriteItem addFavorite(User user, Long itemId) {
		//商品存在チェック
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));
		//既に登録済みならエラー
		if (favoriteItemRepository.existsByUserAndItem(user, item)) {
			//2 重登録を防止
			throw new IllegalStateException("Item is already favorited by this user.");
		}
		//新規のお気に入りエンティティ作成
		FavoriteItem favoriteItem = new FavoriteItem();
		//ユーザを設定
		favoriteItem.setUser(user);
		//商品を設定
		favoriteItem.setItem(item);
		//保存して返す
		return favoriteItemRepository.save(favoriteItem);
	}

	//お気に入り解除
	@Transactional
	public void removeFavorite(User user, Long itemId) {
		//商品取得（存在チェック）
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));
		//ユーザ×商品でお気に入りを取得（なければエラー）
		FavoriteItem favoriteItem = favoriteItemRepository.findByUserAndItem(user, item)
				.orElseThrow(() -> new IllegalStateException("Favorite not found."));
		//削除実行
		favoriteItemRepository.delete(favoriteItem);
	}

	//お気に入りかどうかの判定
	public boolean isFavorited(User user, Long itemId) {
		//商品取得（存在チェック）
		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("Item not found"));
		//存在するかどうかを返す
		return favoriteItemRepository.existsByUserAndItem(user, item);
	}

	//ユーザのお気に入り商品一覧を返す
	public List<Item> getFavoriteItemsByUser(User user) {
		//FavoriteItem から Item へマッピング
		return favoriteItemRepository.findByUser(user).stream()
				.map(FavoriteItem::getItem)
				.collect(Collectors.toList());
	}
}
