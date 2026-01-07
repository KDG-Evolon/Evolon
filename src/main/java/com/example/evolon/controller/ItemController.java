package com.example.evolon.controller;

//入出力例外に備えるための import
import java.io.IOException;
//金額を正確に扱うための BigDecimal の import
import java.math.BigDecimal;
//一覧描画などで使うコレクションの import
import java.util.List;
//Optional で存在チェックを簡潔にするための import
import java.util.Optional;

//ページング機能を使うための import
import org.springframework.data.domain.Page;
//認証ユーザ取得用アノテーションの import
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//認証ユーザの型の import
import org.springframework.security.core.userdetails.UserDetails;
//MVC のコントローラアノテーションの import
import org.springframework.stereotype.Controller;
//画面へデータを渡すための Model の import
import org.springframework.ui.Model;
//HTTP GET を扱うための import
import org.springframework.web.bind.annotation.GetMapping;
//パス変数を扱うための import
import org.springframework.web.bind.annotation.PathVariable;
//HTTP POST を扱うための import
import org.springframework.web.bind.annotation.PostMapping;
//コントローラ全体のベースパス指定用 import
import org.springframework.web.bind.annotation.RequestMapping;
//クエリ/フォームのパラメタ取得用 import
import org.springframework.web.bind.annotation.RequestParam;
//画像アップロードのための MultipartFile の import
import org.springframework.web.multipart.MultipartFile;
//リダイレクト時にメッセージを渡すための import
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

//カテゴリエンティティの import
import com.example.evolon.entity.Category;
//商品エンティティの import
import com.example.evolon.entity.Item;
//ユーザエンティティの import
import com.example.evolon.entity.User;
//カテゴリ関連サービスの import
import com.example.evolon.service.CategoryService;
//チャット関連サービスの import
import com.example.evolon.service.ChatService;
//お気に入り関連サービスの import
import com.example.evolon.service.FavoriteService;
//商品関連サービスの import
import com.example.evolon.service.ItemService;
//レビュー関連サービスの import
import com.example.evolon.service.ReviewService;
//ユーザ関連サービスの import
import com.example.evolon.service.UserService;

//MVC コントローラであることを示すアノテーション
@Controller
///items 配下のリクエストを受け付ける
@RequestMapping("/items")
public class ItemController {

	//商品サービスへの参照
	private final ItemService itemService;
	//カテゴリサービスへの参照
	private final CategoryService categoryService;
	//ユーザサービスへの参照
	private final UserService userService;
	//チャットサービスへの参照
	private final ChatService chatService;
	//お気に入りサービスへの参照
	private final FavoriteService favoriteService;
	//レビューサービスへの参照
	private final ReviewService reviewService;

	//依存関係をコンストラクタインジェクションで受け取る
	public ItemController(
			ItemService itemService,
			CategoryService categoryService,
			UserService userService,
			ChatService chatService,
			FavoriteService favoriteService,
			ReviewService reviewService) {

		this.itemService = itemService;
		this.categoryService = categoryService;
		this.userService = userService;
		this.chatService = chatService;
		this.favoriteService = favoriteService;
		this.reviewService = reviewService;
	}

	//商品一覧を表示する GET エンドポイント
	@GetMapping
	public String listItems(
			//検索キーワード（任意）
			@RequestParam(value = "keyword", required = false) String keyword,
			//カテゴリ ID（任意）
			@RequestParam(value = "categoryId", required = false) Long categoryId,
			//ページ番号（0 始まり、デフォルト 0）
			@RequestParam(value = "page", defaultValue = "0") int page,
			//1 ページ件数（デフォルト 10）
			@RequestParam(value = "size", defaultValue = "10") int size,
			//画面へデータを渡すモデル
			Model model) {

		//条件に応じて商品を検索（出品中のみ）
		Page<Item> items = itemService.searchItems(keyword, categoryId, page, size);
		//カテゴリ一覧を取得
		List<Category> categories = categoryService.getAllCategories();

		//商品一覧をテンプレートへ渡す
		model.addAttribute("items", items);
		//カテゴリ一覧をテンプレートへ渡す
		model.addAttribute("categories", categories);

		//一覧画面のテンプレート名を返す
		return "item_list";
	}

	//商品詳細表示の GET エンドポイント
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

	//出品フォーム表示の GET エンドポイント
	@GetMapping("/new")
	public String showAddItemForm(Model model) {
		//空の Item をフォームのバインド用に渡す
		model.addAttribute("item", new Item());
		//カテゴリの選択肢を渡す
		model.addAttribute("categories", categoryService.getAllCategories());
		//入力フォームのテンプレート名
		return "item_form";
	}

	//出品登録の POST エンドポイント
	@PostMapping
	public String addItem(
			//認証済みユーザを取得
			@AuthenticationPrincipal UserDetails userDetails,
			//商品名
			@RequestParam("name") String name,
			//商品説明
			@RequestParam("description") String description,
			//価格
			@RequestParam("price") BigDecimal price,
			//カテゴリ ID
			@RequestParam("categoryId") Long categoryId,
			//画像ファイル（任意）
			@RequestParam(value = "image", required = false) MultipartFile imageFile,
			//リダイレクトメッセージ用
			RedirectAttributes redirectAttributes) {

		//出品者ユーザを取得（存在しなければ例外）
		User seller = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Seller not found"));

		//カテゴリを ID で取得（存在しなければ 400 相当）
		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));

		//新規 Item を作成して各項目を設定
		Item item = new Item();
		item.setSeller(seller);
		item.setName(name);
		item.setDescription(description);
		item.setPrice(price);
		item.setCategory(category);

		//画像があればアップロードして保存、なければそのまま保存
		try {
			itemService.saveItem(item, imageFile);
			redirectAttributes.addFlashAttribute("successMessage", "商品を出品しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/new";
		}

		return "redirect:/items";
	}

	//出品編集フォーム表示の GET エンドポイント
	@GetMapping("/{id}/edit")
	public String showEditItemForm(@PathVariable("id") Long id, Model model) {
		Optional<Item> item = itemService.getItemById(id);
		if (item.isEmpty()) {
			return "redirect:/items";
		}

		model.addAttribute("item", item.get());
		model.addAttribute("categories", categoryService.getAllCategories());
		return "item_form";
	}

	//出品更新の POST エンドポイント（簡便のため POST を使用）
	@PostMapping("/{id}")
	public String updateItem(
			@PathVariable("id") Long id,
			@AuthenticationPrincipal UserDetails userDetails,
			@RequestParam("name") String name,
			@RequestParam("description") String description,
			@RequestParam("price") BigDecimal price,
			@RequestParam("categoryId") Long categoryId,
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

		try {
			itemService.saveItem(existingItem, imageFile);
			redirectAttributes.addFlashAttribute("successMessage", "商品を更新しました！");
		} catch (IOException e) {
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			return "redirect:/items/{id}/edit";
		}

		return "redirect:/items/{id}";
	}

	// 出品削除の POST エンドポイント
	@PostMapping("/{id}/delete")
	public String deleteItem(
			@PathVariable("id") Long id,
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

	// お気に入り登録の POST
	@PostMapping("/{id}/favorite")
	public String addFavorite(
			@PathVariable("id") Long itemId,
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

	// お気に入り解除の POST
	@PostMapping("/{id}/unfavorite")
	public String removeFavorite(
			@PathVariable("id") Long itemId,
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
