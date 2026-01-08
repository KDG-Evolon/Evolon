package com.example.evolon.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.evolon.domain.enums.CardCondition;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.ItemStatus;
import com.example.evolon.entity.User;
import com.example.evolon.repository.ItemRepository;

@Service
public class ItemService {

	private final ItemRepository itemRepository;
	private final CloudinaryService cloudinaryService;

	public ItemService(
			ItemRepository itemRepository,
			CloudinaryService cloudinaryService) {
		this.itemRepository = itemRepository;
		this.cloudinaryService = cloudinaryService;
	}

	/* =========================
	 * 商品検索（出品中のみ）
	 * ※ 旧：キーワード + カテゴリのみ
	 * ========================= */
	public Page<Item> searchItems(String keyword, Long categoryId, int page, int size) {

		Pageable pageable = PageRequest.of(page, size);

		if (hasText(keyword) && categoryId != null) {
			return itemRepository
					.findByNameContainingIgnoreCaseAndCategory_IdAndStatus(
							keyword, categoryId, ItemStatus.SELLING, pageable);

		} else if (hasText(keyword)) {
			return itemRepository
					.findByNameContainingIgnoreCaseAndStatus(
							keyword, ItemStatus.SELLING, pageable);

		} else if (categoryId != null) {
			return itemRepository
					.findByCategory_IdAndStatus(
							categoryId, ItemStatus.SELLING, pageable);

		} else {
			return itemRepository.findByStatus(ItemStatus.SELLING, pageable);
		}
	}

	/* =========================
	 * ★ カード条件検索（出品中のみ）
	 * （/items/search のフォーム条件をここで反映する）
	 * ========================= */
	public Page<Item> searchByCardFilters(
			String cardName,
			Rarity rarity,
			Regulation regulation,
			CardCondition condition,
			String packName,
			BigDecimal minPrice,
			BigDecimal maxPrice,
			String sort,
			int page,
			int size) {
		// 並び替えを反映した Pageable を作る
		Pageable pageable = PageRequest.of(page, size, ItemSortHelper.toSort(sort));

		// Repository の @Query に投げる
		return itemRepository.searchByCardFilters(
				ItemStatus.SELLING,
				hasText(cardName) ? cardName : null,
				rarity,
				regulation,
				condition,
				hasText(packName) ? packName : null,
				minPrice,
				maxPrice,
				pageable);
	}

	/* =========================
	 * 取得系
	 * ========================= */

	public List<Item> getAllItems() {
		return itemRepository.findAll();
	}

	public Optional<Item> getItemById(Long id) {
		return itemRepository.findById(id);
	}

	/** AdminController 互換：findById を呼んでるならこれを用意 */
	public Optional<Item> findById(Long id) {
		return itemRepository.findById(id);
	}

	public List<Item> getItemsBySeller(User seller) {
		return itemRepository.findBySeller(seller);
	}

	public List<Item> getRecentItems() {
		return itemRepository.findTop5ByOrderByCreatedAtDesc();
	}

	/* =========================
	 * 保存・削除
	 * ========================= */

	@Transactional
	public Item saveItem(Item item, MultipartFile imageFile) throws IOException {

		// 画像がある場合は Cloudinary へアップロードし、URLを保存する
		if (imageFile != null && !imageFile.isEmpty()) {
			item.setImageUrl(cloudinaryService.uploadFile(imageFile));
		}

		// DBへ保存
		return itemRepository.save(item);
	}

	@Transactional
	public void deleteItem(Long itemId) {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

		// 画像がある場合は Cloudinary からも削除
		if (item.getImageUrl() != null) {
			cloudinaryService.deleteFile(item.getImageUrl());
		}

		itemRepository.delete(item);
	}

	/* =========================
	 * 状態変更
	 * ========================= */

	/** ★決済完了 → 発送待ち */
	@Transactional
	public void markAsPaymentDone(Long itemId) {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

		item.setStatus(ItemStatus.PAYMENT_DONE);
		itemRepository.save(item);
	}

	/** 売却確定（取引完全終了などで使う用：必要なら） */
	@Transactional
	public void markAsSold(Long itemId) {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

		item.setStatus(ItemStatus.SOLD);
		itemRepository.save(item);
	}

	/* =========================
	 * 管理者用（AdminController 互換）
	 * ========================= */

	/** 公開（= 出品中に戻す） */
	@Transactional
	public void publishItem(Long itemId) {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

		item.setStatus(ItemStatus.SELLING);
		itemRepository.save(item);
	}

	/** 非公開（= 停止） */
	@Transactional
	public void unpublishItem(Long itemId) {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

		item.setStatus(ItemStatus.SUSPENDED);
		itemRepository.save(item);
	}

	/* =========================
	 * private helper
	 * ========================= */

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
