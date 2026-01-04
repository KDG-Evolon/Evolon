package com.example.evolon.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.evolon.domain.enums.ShippingDuration;
import com.example.evolon.domain.enums.ShippingFeeBurden;
import com.example.evolon.domain.enums.ShippingMethod;
import com.example.evolon.domain.enums.ShippingRegion;
import com.example.evolon.entity.Category;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.User;
import com.example.evolon.service.CategoryService;
import com.example.evolon.service.ChatService;
import com.example.evolon.service.FavoriteService;
import com.example.evolon.service.ItemService;
import com.example.evolon.service.ReviewService;
import com.example.evolon.service.UserService;

/**
 * 商品関連の MVC コントローラ
 */
@Controller
@RequestMapping("/items")
public class ItemController {

	private final ItemService itemService;
	private final CategoryService categoryService;
	private final UserService userService;
	private final ChatService chatService;
	private final FavoriteService favoriteService;
	private final ReviewService reviewService;

	public ItemController(ItemService itemService, CategoryService categoryService,
			UserService userService, ChatService chatService,
			FavoriteService favoriteService, ReviewService reviewService) {
		this.itemService = itemService;
		this.categoryService = categoryService;
		this.userService = userService;
		this.chatService = chatService;
		this.favoriteService = favoriteService;
		this.reviewService = reviewService;
	}

	// -------------------------------
	// 商品一覧表示
	// -------------------------------
	@GetMapping
	public String listItems(
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			Model model) {

		Page<Item> items = itemService.searchItems(keyword, categoryId, page, size);

		// ★ デバッグ用ログ（ここが超重要）
		System.out.println("=== 商品一覧デバッグ ===");
		System.out.println("keyword = " + keyword);
		System.out.println("categoryId = " + categoryId);
		System.out.println("取得件数 = " + items.getTotalElements());

		items.getContent().forEach(item -> {
			System.out.println(
					"itemId=" + item.getId()
							+ ", name=" + item.getName()
							+ ", seller=" + (item.getSeller() != null ? item.getSeller().getId() : "null"));
		});
		System.out.println("======================");

		List<Category> categories = categoryService.getAllCategories();

		model.addAttribute("items", items);
		model.addAttribute("categories", categories);

		return "item_list";
	}

	// -------------------------------
	// 商品詳細表示
	// -------------------------------
	@GetMapping("/{id}")
	public String showItemDetail(
			@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		Optional<Item> itemOpt = itemService.getItemById(id);
		if (itemOpt.isEmpty()) {
			return "redirect:/items";
		}

		Item item = itemOpt.get();
		model.addAttribute("item", item);

		// チャット一覧
		model.addAttribute("chats", chatService.getChatMessagesByItem(id));

		// 出品者評価
		reviewService.getAverageRatingForSeller(item.getSeller())
				.ifPresent(avg -> model.addAttribute("sellerAverageRating", String.format("%.1f", avg)));

		boolean isOwner = false;
		boolean isFavorited = false;

		// ★★★ 追加ここから ★★★
		String currentUserEmail = null;

		if (userDetails != null) {
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));

			currentUserEmail = currentUser.getEmail(); // ← チャット用
			isOwner = item.getSeller() != null
					&& item.getSeller().getId().equals(currentUser.getId());

			isFavorited = favoriteService.isFavorited(currentUser, id);
		}

		// View に渡す
		model.addAttribute("isOwner", isOwner);
		model.addAttribute("isFavorited", isFavorited);
		model.addAttribute("currentUserEmail", currentUserEmail);
		// ★★★ 追加ここまで ★★★

		return "item_detail";
	}

	// -------------------------------
	// 商品出品フォーム表示
	// -------------------------------
	@GetMapping("/new")
	public String showAddItemForm(Model model) {
		model.addAttribute("item", new Item());
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("shippingDurations", ShippingDuration.values());
		model.addAttribute("shippingFeeBurdens", ShippingFeeBurden.values());
		model.addAttribute("shippingRegions", ShippingRegion.values());
		model.addAttribute("shippingMethods", ShippingMethod.values());
		return "item_form";
	}

	// -------------------------------
	// 商品登録（POST）
	// -------------------------------
	@PostMapping
	public String addItem(
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("name") String name,
			@RequestParam("description") String description,
			@RequestParam("price") BigDecimal price,
			@RequestParam("categoryId") Long categoryId,
			@RequestParam("shippingDuration") ShippingDuration shippingDuration,
			@RequestParam("shippingFeeBurden") ShippingFeeBurden shippingFeeBurden,
			@RequestParam("shippingRegion") ShippingRegion shippingRegion,
			@RequestParam("shippingMethod") ShippingMethod shippingMethod,
			@RequestParam(value = "image", required = false) MultipartFile imageFile,
			RedirectAttributes redirectAttributes) {

		// 出品者取得
		User seller = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Seller not found"));

		// カテゴリ取得
		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));

		// ★ item は1回だけ new する
		Item item = new Item();

		// 基本情報
		item.setSeller(seller);
		item.setName(name);
		item.setDescription(description);
		item.setPrice(price);
		item.setCategory(category);
		item.setShippingDuration(shippingDuration);
		item.setShippingFeeBurden(shippingFeeBurden);
		item.setShippingRegion(shippingRegion);
		item.setShippingMethod(shippingMethod);

		// ★ 一覧表示に必要なステータス
		item.setStatus("出品中");

		try {
			itemService.saveItem(item, imageFile);
			redirectAttributes.addFlashAttribute("successMessage", "商品を出品しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute(
					"errorMessage",
					"画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/new";
		}

		return "redirect:/items";
	}

	// -------------------------------
	// 商品編集フォーム表示
	// -------------------------------
	@GetMapping("/{id}/edit")
	public String showEditItemForm(@PathVariable("id") Long id, Model model) {
		Optional<Item> item = itemService.getItemById(id);
		if (item.isEmpty())
			return "redirect:/items";

		model.addAttribute("item", item.get());
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("shippingDurations", ShippingDuration.values());
		model.addAttribute("shippingFeeBurdens", ShippingFeeBurden.values());
		model.addAttribute("shippingRegions", ShippingRegion.values());
		model.addAttribute("shippingMethods", ShippingMethod.values());
		return "item_form";
	}

	// -------------------------------
	// 商品更新（POST）
	// -------------------------------
	@PostMapping("/{id}")
	public String updateItem(@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("name") String name,
			@RequestParam("description") String description,
			@RequestParam("price") BigDecimal price,
			@RequestParam("categoryId") Long categoryId,
			@RequestParam("shippingDuration") ShippingDuration shippingDuration,
			@RequestParam("shippingFeeBurden") ShippingFeeBurden shippingFeeBurden,
			@RequestParam("shippingRegion") ShippingRegion shippingRegion,
			@RequestParam("shippingMethod") ShippingMethod shippingMethod,
			@RequestParam(value = "image", required = false) MultipartFile imageFile,
			RedirectAttributes redirectAttributes) {

		Item existingItem = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!existingItem.getSeller().getId().equals(currentUser.getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は編集できません。");
			return "redirect:/items";
		}

		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));

		existingItem.setName(name);
		existingItem.setDescription(description);
		existingItem.setPrice(price);
		existingItem.setCategory(category);
		existingItem.setShippingDuration(shippingDuration);
		existingItem.setShippingFeeBurden(shippingFeeBurden);
		existingItem.setShippingRegion(shippingRegion);
		existingItem.setShippingMethod(shippingMethod);

		try {
			itemService.saveItem(existingItem, imageFile);
			redirectAttributes.addFlashAttribute("successMessage", "商品を更新しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/{id}/edit";
		}

		return "redirect:/items/{id}";
	}

	// -------------------------------
	// 商品削除
	// -------------------------------
	@PostMapping("/{id}/delete")
	public String deleteItem(@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {

		Item itemToDelete = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		if (!itemToDelete.getSeller().getId().equals(currentUser.getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は削除できません。");
			return "redirect:/items";
		}

		itemService.deleteItem(id);
		redirectAttributes.addFlashAttribute("successMessage", "商品を削除しました。");

		return "redirect:/items";
	}

	// -------------------------------
	// お気に入り登録
	// -------------------------------
	@PostMapping("/{id}/favorite")
	public String addFavorite(@PathVariable("id") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		try {
			favoriteService.addFavorite(currentUser, itemId);
			redirectAttributes.addFlashAttribute("successMessage", "お気に入りに追加しました！");
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}

		return "redirect:/items/{id}";
	}

	// -------------------------------
	// お気に入り解除
	// -------------------------------
	@PostMapping("/{id}/unfavorite")
	public String removeFavorite(@PathVariable("id") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		try {
			favoriteService.removeFavorite(currentUser, itemId);
			redirectAttributes.addFlashAttribute("successMessage", "お気に入りから削除しました。");
		} catch (IllegalStateException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}

		return "redirect:/items/{id}";
	}
}
