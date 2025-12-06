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
	public ItemController(ItemService itemService, CategoryService categoryService,
			UserService userService,
			ChatService chatService, FavoriteService favoriteService, ReviewService reviewService) {
		//商品サービスをフィールドに設定
		this.itemService = itemService;
		//カテゴリサービスをフィールドに設定
		this.categoryService = categoryService;
		//ユーザサービスをフィールドに設定
		this.userService = userService;
		//チャットサービスをフィールドに設定
		this.chatService = chatService;
		//お気に入りサービスをフィールドに設定
		this.favoriteService = favoriteService;
		//レビューサービスをフィールドに設定
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
	public String showItemDetail(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails,
			Model model) {
		//商品を ID で検索（存在しない場合の判定に Optional を使う）
		Optional<Item> item = itemService.getItemById(id);
		//見つからなければ一覧へリダイレクト
		if (item.isEmpty()) {
			//商品が見つからないため一覧へ
			return "redirect:/items";
		}
		//商品本体をテンプレートへ渡す
		model.addAttribute("item", item.get());
		//当該商品のチャット履歴を昇順で取得して渡す
		model.addAttribute("chats", chatService.getChatMessagesByItem(id));
		//出品者の平均評価があれば 1 桁小数で埋め込む
		reviewService.getAverageRatingForSeller(item.get().getSeller())
				.ifPresent(avg -> model.addAttribute("sellerAverageRating",
						String.format("%.1f", avg)));
		//ログイン済みであればお気に入り状態を判定して渡す
		if (userDetails != null) {
			//現在のログインユーザをメールで特定
			User currentUser = userService.getUserByEmail(userDetails.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));
			//お気に入り登録済みかどうかを判定
			model.addAttribute("isFavorited", favoriteService.isFavorited(currentUser, id));
		}
		//商品詳細テンプレートを返す
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
		//出品者を設定
		item.setSeller(seller);
		//商品名を設定
		item.setName(name);
		//説明を設定
		item.setDescription(description);
		//価格を設定
		item.setPrice(price);
		//カテゴリを設定
		item.setCategory(category);
		//画像があればアップロードして保存、なければそのまま保存
		try {
			//画像アップロードを含めて保存
			itemService.saveItem(item, imageFile);
			//成功メッセージをフラッシュ
			redirectAttributes.addFlashAttribute("successMessage", "商品を出品しました！");
		} catch (IOException e) {
			//画像アップロード失敗時のエラーメッセージ
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			//入力フォームへ戻す
			return "redirect:/items/new";
		}
		//一覧へリダイレクト
		return "redirect:/items";
	}

	//出品編集フォーム表示の GET エンドポイント
	@GetMapping("/{id}/edit")
	public String showEditItemForm(@PathVariable("id") Long id, Model model) {
		//対象商品を取得
		Optional<Item> item = itemService.getItemById(id);
		//なければ一覧へ
		if (item.isEmpty()) {
			//商品が存在しない
			return "redirect:/items";
		}
		//既存商品の内容をフォームへ
		model.addAttribute("item", item.get());
		//カテゴリ選択肢を用意
		model.addAttribute("categories", categoryService.getAllCategories());
		//入力フォームを返却
		return "item_form";
	}

	//出品更新の POST エンドポイント（簡便のため POST を使用）
	@PostMapping("/{id}")
	public String updateItem(
			//パスの ID
			@PathVariable("id") Long id,
			//現在のログインユーザ
			@AuthenticationPrincipal UserDetails userDetails,
			//更新後の商品名
			@RequestParam("name") String name,
			//更新後の説明
			@RequestParam("description") String description,
			//更新後の価格
			@RequestParam("price") BigDecimal price,
			//更新後のカテゴリ ID
			@RequestParam("categoryId") Long categoryId,
			//差し替え画像（任意）
			@RequestParam(value = "image", required = false) MultipartFile imageFile,
			//リダイレクトメッセージ
			RedirectAttributes redirectAttributes) {
		// 既存商品を取得（なければ 404 相当）
		Item existingItem = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));
		// 現在ユーザを取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 出品者以外の編集をブロック
		if (!existingItem.getSeller().getId().equals(currentUser.getId())) {
			// 権限エラーをフラッシュ
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は編集できません。");
			// 一覧へ戻す
			return "redirect:/items";
		}
		// カテゴリ取得（なければ 400）
		Category category = categoryService.getCategoryById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("Category not found"));
		// 値を上書き
		existingItem.setName(name);
		// 説明を上書き
		existingItem.setDescription(description);
		// 価格を上書き
		existingItem.setPrice(price);
		// カテゴリを上書き
		existingItem.setCategory(category);
		// 保存処理（画像差し替えがあればアップロード）
		try {
			// 保存実行
			itemService.saveItem(existingItem, imageFile);
			// 成功メッセージ
			redirectAttributes.addFlashAttribute("successMessage", "商品を更新しました！");
		} catch (IOException e) {
			// 画像アップロードの失敗を通知
			redirectAttributes.addFlashAttribute("errorMessage", "画像のアップロードに失敗しました: " + e.getMessage());
			// 編集画面へ戻す
			return "redirect:/items/{id}/edit";
		}
		// 詳細画面へリダイレクト
		return "redirect:/items/{id}";
	}

	// 出品削除の POST エンドポイント
	@PostMapping("/{id}/delete")
	public String deleteItem(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		// 削除対象の商品を取得
		Item itemToDelete = itemService.getItemById(id)
				.orElseThrow(() -> new RuntimeException("Item not found"));
		// 現在のユーザを取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 出品者以外は削除不可
		if (!itemToDelete.getSeller().getId().equals(currentUser.getId())) {
			// 権限エラーを通知
			redirectAttributes.addFlashAttribute("errorMessage", "この商品は削除できません。");
			// 一覧へ
			return "redirect:/items";
		}
		// サービスを通じて削除（画像削除も内包）
		itemService.deleteItem(id);
		// 成功メッセージ
		redirectAttributes.addFlashAttribute("successMessage", "商品を削除しました。");
		// 一覧へ
		return "redirect:/items";
	}

	// お気に入り登録の POST
	@PostMapping("/{id}/favorite")
	public String addFavorite(@PathVariable("id") Long itemId, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		// 現在ユーザを取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 例外をサービス側で投げ、ここでユーザに伝える
		try {
			// お気に入り追加
			favoriteService.addFavorite(currentUser, itemId);
			// 成功メッセージ
			redirectAttributes.addFlashAttribute("successMessage", "お気に入りに追加しました！");
		} catch (IllegalStateException e) {
			// エラーメッセージ
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		// 詳細へ戻る
		return "redirect:/items/{id}";
	}

	// お気に入り解除の POST
	@PostMapping("/{id}/unfavorite")
	public String removeFavorite(@PathVariable("id") Long itemId, @AuthenticationPrincipal UserDetails userDetails,
			RedirectAttributes redirectAttributes) {
		// 現在ユーザを取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 例外をユーザに伝える
		try {
			// お気に入り削除
			favoriteService.removeFavorite(currentUser, itemId);
			// 成功メッセージ
			redirectAttributes.addFlashAttribute("successMessage", "お気に入りから削除しました。");
		} catch (IllegalStateException e) {
			// エラーメッセージ
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		// 詳細へ戻る
		return "redirect:/items/{id}";
	}
}
