package com.example.evolon.service;

// =========================
// import
// =========================

// 画像アップロード（MultipartFile#getBytes）で IOException が起こりうる
import java.io.IOException;
// 価格検索用
import java.math.BigDecimal;
// コレクション
import java.util.List;
import java.util.Optional;

// Spring Data（ページング）
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
// Service
import org.springframework.stereotype.Service;
// ファイルアップロード
import org.springframework.web.multipart.MultipartFile;

// Enum（ポケモンカード検索用）
import com.example.evolon.domain.enums.CardCondition;
import com.example.evolon.domain.enums.ListingType;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;
// Entity
import com.example.evolon.entity.Item;
import com.example.evolon.entity.User;
// Repository
import com.example.evolon.repository.ItemRepository;

@Service
public class ItemService {

	// =========================
	// フィールド
	// =========================
	private final ItemRepository itemRepository;

	// プロジェクト構成によっては他機能で必要になる可能性があるため残す
	// （未使用なら IDE が警告を出すだけで動作には影響しない）
	private final CategoryService categoryService;

	private final CloudinaryService cloudinaryService;

	// =========================
	// コンストラクタ
	// =========================
	public ItemService(
			ItemRepository itemRepository,
			CategoryService categoryService,
			CloudinaryService cloudinaryService) {

		this.itemRepository = itemRepository;
		this.categoryService = categoryService;
		this.cloudinaryService = cloudinaryService;
	}

	// =========================
	// 通常の商品検索（一覧画面用）
	// ・ステータス「出品中」のみを対象に絞る
	// =========================
	public Page<Item> searchItems(String keyword, Long categoryId, int page, int size) {

		Pageable pageable = PageRequest.of(page, size);

		// キーワード＋カテゴリ
		if (keyword != null && !keyword.isEmpty() && categoryId != null) {
			return itemRepository.findByNameContainingIgnoreCaseAndCategoryIdAndStatus(
					keyword, categoryId, "出品中", pageable);
		}

		// キーワードのみ
		if (keyword != null && !keyword.isEmpty()) {
			return itemRepository.findByNameContainingIgnoreCaseAndStatus(
					keyword, "出品中", pageable);
		}

		// カテゴリのみ
		if (categoryId != null) {
			return itemRepository.findByCategoryIdAndStatus(
					categoryId, "出品中", pageable);
		}

		// 条件なし（出品中の全件）
		return itemRepository.findByStatus("出品中", pageable);
	}

	// =========================
	// ★ ポケモンカード詳細検索
	// ・null / 空文字は Repository 側のJPQLで無視する想定
	// =========================
	public Page<Item> searchPokemonCards(
			String cardName,
			Rarity rarity,
			Regulation regulation,
			String packName,
			CardCondition condition,
			ListingType listingType,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			Pageable pageable) {

		return itemRepository.searchPokemonCards(
				cardName,
				rarity,
				regulation,
				packName,
				condition,
				listingType,
				minPrice,
				maxPrice,
				pageable);
	}

	// =========================
	// 全商品取得（管理者用）
	// =========================
	public List<Item> getAllItems() {
		return itemRepository.findAll();
	}

	// =========================
	// 商品ID取得（通常）
	// =========================
	public Optional<Item> getItemById(Long id) {
		return itemRepository.findById(id);
	}

	// =========================
	// 商品ID取得（互換用）
	// ※ origin/main 側で findById を呼んでいる可能性があるため残す
	// =========================
	public Optional<Item> findById(Long id) {
		return itemRepository.findById(id);
	}

	// =========================
	// 商品保存（画像対応）
	// ・画像がある場合は Cloudinary にアップロードして URL を保存
	// =========================
	public Item saveItem(Item item, MultipartFile imageFile) throws IOException {

		// 画像がある場合のみアップロード
		if (imageFile != null && !imageFile.isEmpty()) {
			String imageUrl = cloudinaryService.uploadFile(imageFile);
			item.setImageUrl(imageUrl);
		}

		// DBに保存
		return itemRepository.save(item);
	}

	// =========================
	// 商品削除
	// ・画像があれば Cloudinary からも削除してからDB削除
	// =========================
	public void deleteItem(Long id) {

		itemRepository.findById(id).ifPresent(item -> {

			// 画像があれば削除
			if (item.getImageUrl() != null) {
				cloudinaryService.deleteFile(item.getImageUrl());
			}

			itemRepository.deleteById(id);
		});
	}

	// =========================
	// 出品者別の商品一覧
	// =========================
	public List<Item> getItemsBySeller(User seller) {
		return itemRepository.findBySeller(seller);
	}

	// =========================
	// 売却確定
	// =========================
	public void markItemAsSold(Long itemId) {
		itemRepository.findById(itemId).ifPresent(item -> {
			item.setStatus("売却済");
			itemRepository.save(item);
		});
	}

	// =========================
	// 最近の出品（管理者ダッシュボード用）
	// =========================
	public List<Item> getRecentItems() {
		return itemRepository.findTop5ByOrderByCreatedAtDesc();
	}

	// =========================
	// 商品を非公開にする（管理者用）
	// =========================
	public void unpublishItem(Long itemId) {
		itemRepository.findById(itemId).ifPresent(item -> {
			item.setStatus("非公開");
			itemRepository.save(item);
		});
	}

	// =========================
	// 商品を再公開する（管理者用）
	// =========================
	public void publishItem(Long itemId) {
		itemRepository.findById(itemId).ifPresent(item -> {
			item.setStatus("出品中");
			itemRepository.save(item);
		});
	}
}
