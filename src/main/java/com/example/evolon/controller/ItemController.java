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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.evolon.domain.enums.CardCondition;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;
import com.example.evolon.domain.enums.ShippingDuration;
import com.example.evolon.domain.enums.ShippingFeeBurden;
import com.example.evolon.domain.enums.ShippingMethod;
import com.example.evolon.domain.enums.ShippingRegion;
import com.example.evolon.dto.CardAutoFillResponse;
import com.example.evolon.dto.ParsedCardNumber;
import com.example.evolon.entity.CardInfo;
import com.example.evolon.entity.Category;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.User;
import com.example.evolon.service.CardMasterService;
import com.example.evolon.service.CardNumberParserService;
import com.example.evolon.service.CategoryService;
import com.example.evolon.service.ChatService;
import com.example.evolon.service.FavoriteService;
import com.example.evolon.service.ItemService;
import com.example.evolon.service.RegulationService;
import com.example.evolon.service.ReviewService;
import com.example.evolon.service.UserService;

@Controller
@RequestMapping("/items")
public class ItemController {

	private final ItemService itemService;
	private final CategoryService categoryService;
	private final UserService userService;
	private final ChatService chatService;
	private final FavoriteService favoriteService;
	private final ReviewService reviewService;
	private final CardNumberParserService cardNumberParserService;
	private final CardMasterService cardMasterService;
	private final RegulationService regulationService;

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
	 * 商品一覧 GET /items
	 * ========================================================= */
	@GetMapping
	public String listItems(
			@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "10") int size,
			Model model) {

		Page<Item> items = itemService.searchItems(keyword, categoryId, page, size);
		List<Category> categories = categoryService.getAllCategories();

		model.addAttribute("items", items);
		model.addAttribute("categories", categories);
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("regulations", Regulation.values());
		model.addAttribute("conditions", CardCondition.values());

		return "item_list";
	}

	/* =========================================================
	 * カード検索 GET /items/search
	 * ========================================================= */
	@GetMapping("/search")
	public String search(
			@RequestParam(required = false) String cardName,
			@RequestParam(required = false) String rarity,
			@RequestParam(required = false) String regulation,
			@RequestParam(required = false) String condition,
			@RequestParam(required = false) String packName,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@RequestParam(defaultValue = "new") String sort,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			Model model) {

		Rarity rarityEnum = parseEnumSafely(rarity, Rarity.class);
		Regulation regEnum = parseEnumSafely(regulation, Regulation.class);
		CardCondition condEnum = parseEnumSafely(condition, CardCondition.class);

		Page<Item> items = itemService.searchByCardFilters(
				cardName, rarityEnum, regEnum, condEnum, packName,
				minPrice, maxPrice, sort, page, size);

		model.addAttribute("items", items);
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("regulations", Regulation.values());
		model.addAttribute("conditions", CardCondition.values());

		return "item_list";
	}

	/* =========================================================
	 * 商品詳細 GET /items/{id}
	 * ========================================================= */
	@GetMapping("/{id}")
	public String showItemDetail(
			@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,
			Model model) {

		Optional<Item> itemOpt = itemService.getItemById(id);
		if (itemOpt.isEmpty())
			return "redirect:/items";

		Item item = itemOpt.get();
		model.addAttribute("item", item);
		model.addAttribute("chats", chatService.getChatMessagesByItem(id));

		if (item.getSeller() != null) {
			model.addAttribute("sellerGoodCount", reviewService.countGoodForSeller(item.getSeller()));
			model.addAttribute("sellerBadCount", reviewService.countBadForSeller(item.getSeller()));
		}

		boolean isOwner = false;
		boolean isFavorited = false;
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
	 * 出品フォーム GET /items/new
	 * ========================================================= */
	@GetMapping("/new")
	public String showAddItemForm(Model model) {
		model.addAttribute("item", new Item());
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("shippingDurations", ShippingDuration.values());
		model.addAttribute("shippingFeeBurdens", ShippingFeeBurden.values());
		model.addAttribute("shippingRegions", ShippingRegion.values());
		model.addAttribute("shippingMethods", ShippingMethod.values());
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("conditions", CardCondition.values());
		model.addAttribute("regulations", Regulation.values());
		return "item_form";
	}

	@GetMapping("/auto-fill")
	@ResponseBody
	public CardAutoFillResponse autoFill(@RequestParam String text) {
		ParsedCardNumber parsed = cardNumberParserService.parse(text);
		return cardMasterService.findByParsedNumber(parsed)
				.map(cm -> {
					Regulation reg = regulationService.resolve(cm.getPrintedRegulation());
					return new CardAutoFillResponse(cm.getCardName(), cm.getRarity(), cm.getPackName(), reg);
				})
				.orElse(null);
	}

	/* =========================================================
	 * 出品登録 POST /items
	 * ========================================================= */
	@PostMapping
	public String saveItem(
			@AuthenticationPrincipal UserDetails userDetails,
			@ModelAttribute Item item,
			@RequestParam("categoryId") Long categoryId,
			@RequestParam(value = "image", required = false) MultipartFile imageFile,
			RedirectAttributes redirectAttributes) {

		if (userDetails == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "ログインしてください。");
			return "redirect:/login";
		}

		User seller = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Seller not found"));
		item.setSeller(seller);

		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));
		item.setCategory(category);

		// カードカテゴリ以外は CardInfo 無視
		if (!"カード".equals(category.getName())) {
			item.setCardInfo(null);
		} else {
			CardInfo cardInfo = item.getCardInfo();
			if (cardInfo == null
					|| cardInfo.getCardName() == null || cardInfo.getCardName().isBlank()
					|| cardInfo.getPackName() == null || cardInfo.getPackName().isBlank()
					|| cardInfo.getRarity() == null
					|| cardInfo.getCondition() == null
					|| cardInfo.getRegulation() == null) {

				redirectAttributes.addFlashAttribute("errorMessage",
						"カード名・レアリティ・封入パック・状態・レギュレーションはすべて必須です。");
				return "redirect:/items/new";
			}

			cardInfo.setItem(item);

			// H/I/J → STANDARD, それ以外 → EXTRA
			cardInfo.setRegulation(mapToRegulation(cardInfo.getRegulation().name()));
		}

		// shipping デフォルト補完
		if (item.getShippingDuration() == null)
			item.setShippingDuration(ShippingDuration.values()[0]);
		if (item.getShippingFeeBurden() == null)
			item.setShippingFeeBurden(ShippingFeeBurden.values()[0]);
		if (item.getShippingMethod() == null)
			item.setShippingMethod(ShippingMethod.values()[0]);
		if (item.getShippingRegion() == null)
			item.setShippingRegion(ShippingRegion.values()[0]);

		try {
			itemService.saveItem(item, imageFile);
			redirectAttributes.addFlashAttribute("successMessage", "商品を出品しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/new";
		}

		return "redirect:/items";
	}

	/* =========================================================
	 * 出品編集フォーム GET /items/{id}/edit
	 * ========================================================= */
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
		model.addAttribute("rarities", Rarity.values());
		model.addAttribute("conditions", CardCondition.values());
		model.addAttribute("regulations", Regulation.values());

		return "item_form";
	}

	/* =========================================================
	 * レギュレーション自動マッピング
	 * H/I/J → STANDARD, それ以外 → EXTRA
	 * ========================================================= */
	private Regulation mapToRegulation(String printedReg) {
		if (printedReg == null)
			return Regulation.EXTRA;
		switch (printedReg.toUpperCase()) {
		case "H":
		case "I":
		case "J":
			return Regulation.STANDARD;
		default:
			return Regulation.EXTRA;
		}
	}

	/* =========================================================
	 * 安全な enum 変換
	 * ========================================================= */
	private <E extends Enum<E>> E parseEnumSafely(String value, Class<E> enumClass) {
		if (value == null || value.isBlank())
			return null;
		try {
			return Enum.valueOf(enumClass, value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

}
