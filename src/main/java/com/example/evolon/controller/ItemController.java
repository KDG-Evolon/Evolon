package com.example.evolon.controller;

// 入出力例外に備えるための import
import java.io.IOException;
// 金額を正確に扱うための BigDecimal の import
import java.math.BigDecimal;
// 一覧描画などで使うコレクションの import
import java.util.List;
// Optional で存在チェックを簡潔にするための import
import java.util.Optional;

// ページング機能を使うための import
import org.springframework.data.domain.Page;
// 認証ユーザ取得用アノテーションの import
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// 認証ユーザの型の import
import org.springframework.security.core.userdetails.UserDetails;
// MVC のコントローラアノテーションの import
import org.springframework.stereotype.Controller;
// 画面へデータを渡すための Model の import
import org.springframework.ui.Model;
// HTTP GET を扱うための import
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
// パス変数を扱うための import
import org.springframework.web.bind.annotation.PathVariable;
// HTTP POST を扱うための import
import org.springframework.web.bind.annotation.PostMapping;
// コントローラ全体のベースパス指定用 import
import org.springframework.web.bind.annotation.RequestMapping;
// クエリ/フォームのパラメタ取得用 import
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
// 画像アップロードのための MultipartFile の import
import org.springframework.web.multipart.MultipartFile;
// リダイレクト時にメッセージを渡すための import
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.evolon.domain.enums.CardCondition;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;
// ★ enum（選択肢用）
import com.example.evolon.domain.enums.ShippingDuration;
import com.example.evolon.domain.enums.ShippingFeeBurden;
import com.example.evolon.domain.enums.ShippingMethod;
import com.example.evolon.domain.enums.ShippingRegion;
import com.example.evolon.dto.CardAutoFillResponse;
import com.example.evolon.dto.ParsedCardNumber;
// カテゴリエンティティの import
import com.example.evolon.entity.Category;
// 商品エンティティの import
import com.example.evolon.entity.Item;
// ユーザエンティティの import
import com.example.evolon.entity.User;
import com.example.evolon.service.CardMasterService;
import com.example.evolon.service.CardNumberParserService;
// カテゴリ関連サービスの import
import com.example.evolon.service.CategoryService;
// チャット関連サービスの import
import com.example.evolon.service.ChatService;
// お気に入り関連サービスの import
import com.example.evolon.service.FavoriteService;
// 商品関連サービスの import
import com.example.evolon.service.ItemService;
import com.example.evolon.service.RegulationService;
// レビュー関連サービスの import
import com.example.evolon.service.ReviewService;
// ユーザ関連サービスの import
import com.example.evolon.service.UserService;

// MVC コントローラであることを示すアノテーション
@Controller
// /items 配下のリクエストを受け付ける
@RequestMapping("/items")
public class ItemController {

	// 商品サービスへの参照
	private final ItemService itemService;
	// カテゴリサービスへの参照
	private final CategoryService categoryService;
	// ユーザサービスへの参照
	private final UserService userService;
	// チャットサービスへの参照
	private final ChatService chatService;
	// お気に入りサービスへの参照
	private final FavoriteService favoriteService;
	// レビューサービスへの参照
	private final ReviewService reviewService;

	private final CardNumberParserService cardNumberParserService;
	private final CardMasterService cardMasterService;
	private final RegulationService regulationService;

	// 依存関係をコンストラクタインジェクションで受け取る
	public ItemController(
			ItemService itemService,
			CategoryService categoryService,
			UserService userService,
			ChatService chatService,
			FavoriteService favoriteService,
			ReviewService reviewService,
			CardNumberParserService cardNumberParserService,
			CardMasterService cardMasterService,
			RegulationService regulationService) {

		this.itemService = itemService;
		this.categoryService = categoryService;
		this.userService = userService;
		this.chatService = chatService;
		this.favoriteService = favoriteService;
		this.reviewService = reviewService;
		this.cardNumberParserService = cardNumberParserService;
		this.cardMasterService = cardMasterService;
		this.regulationService = regulationService;
	}

	/* =========================================================
	 * 商品一覧（通常一覧） GET /items
	 * ========================================================= */
	@GetMapping
	public String listItems(
			// 検索キーワード（任意）※この画面では主に商品名/カテゴリ向け
			@RequestParam(value = "keyword", required = false) String keyword,
			// カテゴリ ID（任意）
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			// ページ番号（0 始まり、デフォルト 0）
			@RequestParam(value = "page", defaultValue = "0") int page,
			// 1 ページ件数（デフォルト 10）
			@RequestParam(value = "size", defaultValue = "10") int size,
			// 画面へデータを渡すモデル
			Model model) {

		// 条件に応じて商品を検索（出品中のみ）
		Page<Item> items = itemService.searchItems(keyword, categoryId, page, size);

		// カテゴリ一覧を取得
		List<Category> categories = categoryService.getAllCategories();

		// ★ item_list.html が検索フォームで enum を参照するなら、通常一覧でも必ず渡す
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("regulations", Regulation.values());
		model.addAttribute("conditions", CardCondition.values());

		// 商品一覧をテンプレートへ渡す
		model.addAttribute("items", items);
		// カテゴリ一覧をテンプレートへ渡す
		model.addAttribute("categories", categories);

		// 一覧画面のテンプレート名を返す
		return "item_list";
	}

	/* =========================================================
	 * ★ カード条件検索 GET /items/search
	 * （cardName/rarity/regulation/condition/packName/price/sort を反映）
	 *
	 * ★ポイント：
	 *  - rarity/regulation/condition は String で受けて enum に変換
	 *    → HTML の value が壊れていても例外になりにくい
	 * ========================================================= */
	@GetMapping("/search")
	public String search(
			// カード名（部分一致想定）
			@RequestParam(required = false) String cardName,
			// レアリティ（enum名を想定：例 "UR"）
			@RequestParam(required = false) String rarity,
			// レギュレーション（enum名を想定）
			@RequestParam(required = false) String regulation,
			// 状態（enum名を想定）
			@RequestParam(required = false) String condition,
			// 封入パック（部分一致想定）
			@RequestParam(required = false) String packName,
			// 最低価格
			@RequestParam(required = false) BigDecimal minPrice,
			// 最高価格
			@RequestParam(required = false) BigDecimal maxPrice,
			// 並び替え（new / priceAsc / priceDesc）
			@RequestParam(defaultValue = "new") String sort,
			// ページ
			@RequestParam(defaultValue = "0") int page,
			// 1ページ件数
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		// ===== enum 安全変換（空や不正値なら null 扱いにする）=====
		Rarity rarityEnum = parseEnumSafely(rarity, Rarity.class);
		Regulation regEnum = parseEnumSafely(regulation, Regulation.class);
		CardCondition condEnum = parseEnumSafely(condition, CardCondition.class);

		// 検索
		Page<Item> items = itemService.searchByCardFilters(
				cardName, rarityEnum, regEnum, condEnum, packName,
				minPrice, maxPrice, sort, page, size);

		// ★ item_list.html で enum/カテゴリを参照するので常に渡す
		model.addAttribute("items", items);
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("regulations", Regulation.values());
		model.addAttribute("conditions", CardCondition.values());
		model.addAttribute("categories", categoryService.getAllCategories());

		return "item_list";
	}

	/* =========================================================
	 * 商品詳細表示 GET /items/{id}
	 * ========================================================= */
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

		// チャット
		model.addAttribute("chats", chatService.getChatMessagesByItem(id));

		// 出品者評価（GOOD/BAD件数）
		if (item.getSeller() != null) {
			model.addAttribute("sellerGoodCount", reviewService.countGoodForSeller(item.getSeller()));
			model.addAttribute("sellerBadCount", reviewService.countBadForSeller(item.getSeller()));
		}

		boolean isOwner = false;
		boolean isFavorited = false;

		// ログインしている場合だけ、所有者判定・お気に入り判定を行う
		if (userDetails != null) {
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));

			isOwner = item.getSeller() != null
					&& item.getSeller().getId().equals(currentUser.getId());

			isFavorited = favoriteService.isFavorited(currentUser, id);
		}

		model.addAttribute("isOwner", isOwner);
		model.addAttribute("isFavorited", isFavorited);

		return "item_detail";
	}

	/* =========================================================
	 * 出品フォーム表示 GET /items/new
	 * ========================================================= */
	@GetMapping("/new")
	public String showAddItemForm(Model model) {

		// 空の Item をフォームのバインド用に渡す
		model.addAttribute("item", new Item());
		// カテゴリの選択肢を渡す
		model.addAttribute("categories", categoryService.getAllCategories());

		// ★ セレクトボックス用 enum
		model.addAttribute("shippingDurations", ShippingDuration.values());
		model.addAttribute("shippingFeeBurdens", ShippingFeeBurden.values());
		model.addAttribute("shippingRegions", ShippingRegion.values());
		model.addAttribute("shippingMethods", ShippingMethod.values());

		// ★ カード情報用 enum
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("conditions", CardCondition.values());
		model.addAttribute("regulations", Regulation.values());

		// 入力フォームのテンプレート名
		return "item_form";
	}

	@GetMapping("/auto-fill")
	@ResponseBody
	public CardAutoFillResponse autoFill(@RequestParam String text) {

		ParsedCardNumber parsed = cardNumberParserService.parse(text);

		return cardMasterService.findByParsedNumber(parsed)
				.map(cm -> {
					Regulation reg = regulationService.resolve(cm.getPrintedRegulation());

					return new CardAutoFillResponse(
							cm.getCardName(),
							cm.getRarity(),
							cm.getPackName(),
							reg);
				})
				.orElse(null);
	}

	/* =========================================================
	 * 出品登録 POST /items
	 * ========================================================= */
	@PostMapping
	public String addItem(
			// 認証済みユーザ（未ログインなら null）
			@AuthenticationPrincipal UserDetails userDetails,

			// ★ フォーム全体を Item として受け取る
			@ModelAttribute Item item,

			// カテゴリ ID（select は entity 直 bind しない方が安全）
			@RequestParam("categoryId") Long categoryId,

			// 画像
			@RequestParam(value = "image", required = false) MultipartFile imageFile,

			RedirectAttributes redirectAttributes) {

		// =========================
		// 未ログイン防止
		// =========================
		if (userDetails == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログインしてください。");
			return "redirect:/login";
		}

		// =========================
		// 出品者設定
		// =========================
		User seller = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Seller not found"));
		item.setSeller(seller);

		// =========================
		// カテゴリ設定
		// =========================
		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));
		item.setCategory(category);

		// =========================
		// ★ カードカテゴリ以外なら CardInfo を無視する（サーバ側安全対策）
		// =========================
		if (category.getName() == null || !"カード".equals(category.getName())) {
			item.setCardInfo(null);
		} else {
			// =========================
			// OneToOne の owner は CardInfo（カードカテゴリのみ必須）
			// =========================
			if (item.getCardInfo() == null
					|| item.getCardInfo().getCardName() == null || item.getCardInfo().getCardName().isBlank()
					|| item.getCardInfo().getPackName() == null || item.getCardInfo().getPackName().isBlank()
					|| item.getCardInfo().getRarity() == null
					|| item.getCardInfo().getCondition() == null
					|| item.getCardInfo().getRegulation() == null) {

				redirectAttributes.addFlashAttribute(
						"errorMessage",
						"カード名・レアリティ・封入パック・状態・レギュレーションはすべて必須です。");
				return "redirect:/items/new";
			}

			// =========================
			// ★ 超重要：CardInfo 側に Item をセット
			// =========================
			item.getCardInfo().setItem(item);
		}

		// =========================
		// 保存
		// =========================
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

	/* =========================================================
	 * 出品編集フォーム表示 GET /items/{id}/edit
	 * ========================================================= */
	@GetMapping("/{id}/edit")
	public String showEditItemForm(@PathVariable("id") Long id, Model model) {

		Optional<Item> item = itemService.getItemById(id);
		if (item.isEmpty()) {
			return "redirect:/items";
		}

		model.addAttribute("item", item.get());
		model.addAttribute("categories", categoryService.getAllCategories());

		// ★ 編集時も必要（発送）
		model.addAttribute("shippingDurations", ShippingDuration.values());
		model.addAttribute("shippingFeeBurdens", ShippingFeeBurden.values());
		model.addAttribute("shippingRegions", ShippingRegion.values());
		model.addAttribute("shippingMethods", ShippingMethod.values());

		// ★ 編集時も必要（カード）
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("conditions", CardCondition.values());
		model.addAttribute("regulations", Regulation.values());

		return "item_form";
	}

	/* =========================================================
	 * 出品更新 POST /items/{id}
	 * ========================================================= */
	@PostMapping("/{id}")
	public String updateItem(
			@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,

			@RequestParam("name") String name,
			@RequestParam("description") String description,
			@RequestParam("price") BigDecimal price,
			@RequestParam("categoryId") Long categoryId,

			// 画像ファイル（任意）
			@RequestParam(value = "image", required = false) MultipartFile imageFile,

			// 発送系（フォームにあるなら受け取って更新する）
			@RequestParam("shippingDuration") ShippingDuration shippingDuration,
			@RequestParam("shippingFeeBurden") ShippingFeeBurden shippingFeeBurden,
			@RequestParam("shippingRegion") ShippingRegion shippingRegion,
			@RequestParam("shippingMethod") ShippingMethod shippingMethod,

			// ★ ここを required=false にして、カード以外でも 400 にならないようにする
			@RequestParam(value = "cardInfo.cardName", required = false) String cardName,
			@RequestParam(value = "cardInfo.packName", required = false) String packName,
			@RequestParam(value = "cardInfo.rarity", required = false) Rarity rarity,
			@RequestParam(value = "cardInfo.condition", required = false) CardCondition condition,
			@RequestParam(value = "cardInfo.regulation", required = false) Regulation regulation,

			RedirectAttributes redirectAttributes) {

		// 未ログインなら更新不可
		if (userDetails == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログインしてください。");
			return "redirect:/login";
		}

		Item existingItem = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 所有者以外は編集不可
		if (existingItem.getSeller() == null || !existingItem.getSeller().getId().equals(currentUser.getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は編集できません。");
			return "redirect:/items";
		}

		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));

		// 基本情報更新
		existingItem.setName(name);
		existingItem.setDescription(description);
		existingItem.setPrice(price);
		existingItem.setCategory(category);

		// 発送情報更新
		existingItem.setShippingDuration(shippingDuration);
		existingItem.setShippingFeeBurden(shippingFeeBurden);
		existingItem.setShippingRegion(shippingRegion);
		existingItem.setShippingMethod(shippingMethod);

		// =========================
		// ★ カードカテゴリ以外なら CardInfo を消す（サーバ側安全対策）
		// =========================
		if (category.getName() == null || !"カード".equals(category.getName())) {
			existingItem.setCardInfo(null);
		} else {
			// =========================
			// ★ カードカテゴリなら cardInfo 必須（サーバ側でも守る）
			// =========================
			if (cardName == null || cardName.isBlank()
					|| packName == null || packName.isBlank()
					|| rarity == null
					|| condition == null
					|| regulation == null) {
				redirectAttributes.addFlashAttribute(
						"errorMessage",
						"カード名・レアリティ・封入パック・状態・レギュレーションはすべて必須です。");
				return "redirect:/items/{id}/edit";
			}

			// null なら作る
			if (existingItem.getCardInfo() == null) {
				existingItem.setCardInfo(new com.example.evolon.entity.CardInfo());
			}

			existingItem.getCardInfo().setCardName(cardName);
			existingItem.getCardInfo().setPackName(packName);
			existingItem.getCardInfo().setRarity(rarity);
			existingItem.getCardInfo().setCondition(condition);
			existingItem.getCardInfo().setRegulation(regulation);

			// ownerセット（OneToOne）
			existingItem.getCardInfo().setItem(existingItem);
		}

		try {
			itemService.saveItem(existingItem, imageFile);
			redirectAttributes.addFlashAttribute("successMessage", "商品を更新しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/{id}/edit";
		}

		return "redirect:/items/{id}";
	}

	/* =========================================================
	 * 出品削除 POST /items/{id}/delete
	 * ========================================================= */
	@PostMapping("/{id}/delete")
	public String deleteItem(
			@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {

		// 未ログインなら削除不可
		if (userDetails == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログインしてください。");
			return "redirect:/login";
		}

		Item itemToDelete = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));

		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 所有者以外は削除不可
		if (itemToDelete.getSeller() == null || !itemToDelete.getSeller().getId().equals(currentUser.getId())) {
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は削除できません。");
			return "redirect:/items";
		}

		itemService.deleteItem(id);
		redirectAttributes.addFlashAttribute("successMessage", "商品を削除しました。");
		return "redirect:/items";
	}

	/* =========================================================
	 * お気に入り登録 POST /items/{id}/favorite
	 * ========================================================= */
	@PostMapping("/{id}/favorite")
	public String addFavorite(
			@PathVariable("id") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {

		// 未ログインなら不可
		if (userDetails == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログインしてください。");
			return "redirect:/login";
		}

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

	/* =========================================================
	 * お気に入り解除 POST /items/{id}/unfavorite
	 * ========================================================= */
	@PostMapping("/{id}/unfavorite")
	public String removeFavorite(
			@PathVariable("id") Long itemId,
			@AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {

		// 未ログインなら不可
		if (userDetails == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログインしてください。");
			return "redirect:/login";
		}

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

	private <E extends Enum<E>> E parseEnumSafely(String value, Class<E> enumClass) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException ex) {
			// ★ 変換できない値が来たら null 扱い（検索条件なし扱い）
			return null;
		}
	}

}
