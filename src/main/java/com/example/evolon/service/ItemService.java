package com.example.evolon.service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
	 * ========================= */
	public Page<Item> searchItems(String keyword, Long categoryId, int page, int size) {

		Pageable pageable = PageRequest.of(page, size);

		if (hasText(keyword) && categoryId != null) {
			return itemRepository
					.findByNameContainingIgnoreCaseAndCategoryIdAndStatus(
							keyword, categoryId, ItemStatus.SELLING, pageable);

		} else if (hasText(keyword)) {
			return itemRepository
					.findByNameContainingIgnoreCaseAndStatus(
							keyword, ItemStatus.SELLING, pageable);

		} else if (categoryId != null) {
			return itemRepository
					.findByCategoryIdAndStatus(
							categoryId, ItemStatus.SELLING, pageable);

		} else {
			return itemRepository.findByStatus(ItemStatus.SELLING, pageable);
		}
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

	public List<Item> getItemsBySeller(User seller) {
		return itemRepository.findBySeller(seller);
	}

	public List<Item> getRecentItems() {
		return itemRepository.findTop5ByOrderByCreatedAtDesc();
	}

	/* =========================
	 * 保存・削除
	 * ========================= */

	public Item saveItem(Item item, MultipartFile imageFile) throws IOException {

		if (imageFile != null && !imageFile.isEmpty()) {
			item.setImageUrl(cloudinaryService.uploadFile(imageFile));
		}

		return itemRepository.save(item);
	}

	public void deleteItem(Long itemId) {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

		if (item.getImageUrl() != null) {
			cloudinaryService.deleteFile(item.getImageUrl());
		}

		itemRepository.delete(item);
	}

	/* =========================
	 * 状態変更
	 * ========================= */

	/** 売却確定 */
	public void markAsSold(Long itemId) {

		Item item = itemRepository.findById(itemId)
				.orElseThrow(() -> new IllegalArgumentException("商品が見つかりません"));

		item.setStatus(ItemStatus.SOLD);
		itemRepository.save(item);
	}

	/* =========================
	 * private helper
	 * ========================= */

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}
}
