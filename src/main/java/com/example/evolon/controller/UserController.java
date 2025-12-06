package com.example.evolon.controller;

//認証ユーザをパラメータ注入するアノテーション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//Spring Security のユーザ詳細型
import org.springframework.security.core.userdetails.UserDetails;
//MVC コントローラのアノテーション
import org.springframework.stereotype.Controller;
//画面へ値を渡す Model
import org.springframework.ui.Model;
//ルーティング関連のアノテーション
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

//ユーザエンティティ（Model へ載せるため）
import com.example.evolon.entity.User;
//注文サービス（購入/売上一覧で使用）
import com.example.evolon.service.AppOrderService;
//お気に入りサービス（お気に入り一覧で使用）
import com.example.evolon.service.FavoriteService;
//商品サービス（出品一覧で使用）
import com.example.evolon.service.ItemService;
//レビューサービス（自分のレビュー一覧で使用）
import com.example.evolon.service.ReviewService;
//ユーザサービス（ログインユーザの取得で使用）
import com.example.evolon.service.UserService;

//MVC コントローラとして登録
@Controller
//ベースパス /my-page にマッピング
@RequestMapping("/my-page")
public class UserController {
	//ユーザサービスの参照
	private final UserService userService;
	//商品サービスの参照
	private final ItemService itemService;
	//注文サービスの参照
	private final AppOrderService appOrderService;
	//お気に入りサービスの参照
	private final FavoriteService favoriteService;
	//レビューサービスの参照
	private final ReviewService reviewService;

	//依存関係をコンストラクタで受け取る
	public UserController(UserService userService, ItemService itemService, AppOrderService appOrderService,
			FavoriteService favoriteService, ReviewService reviewService) {
		// フィールドへ代入
		this.userService = userService;
		// フィールドへ代入
		this.itemService = itemService;
		// フィールドへ代入
		this.appOrderService = appOrderService;
		// フィールドへ代入
		this.favoriteService = favoriteService;
		// フィールドへ代入
		this.reviewService = reviewService;
	}

	// マイページのトップ（プロフィール）
	@GetMapping
	public String myPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 現在のユーザを取得（見つからなければ例外）
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 画面へユーザ情報を渡す
		model.addAttribute("user", currentUser);
		// プロフィール画面を返却
		return "my_page";
	}

	// 自分の出品一覧
	@GetMapping("/selling")
	public String mySellingItems(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 現在のユーザ取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 出品中/過去出品を取得
		model.addAttribute("sellingItems", itemService.getItemsBySeller(currentUser));
		// 出品者向け一覧テンプレート
		return "seller_items";
	}

	// 自分が購入した注文一覧
	@GetMapping("/orders")
	public String myOrders(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 現在のユーザ取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 自分の注文を取得
		model.addAttribute("myOrders", appOrderService.getOrdersByBuyer(currentUser));
		// 購入者向け一覧テンプレート
		return "buyer_app_orders";
	}

	// 自分が販売した注文一覧
	@GetMapping("/sales")
	public String mySales(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 現在のユーザ取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 自分の売上（自分の商品に紐づく注文）を取得
		model.addAttribute("mySales", appOrderService.getOrdersBySeller(currentUser));
		// 出品者向け売上一覧テンプレート
		return "seller_app_orders";
	}

	// 自分のお気に入り商品一覧
	@GetMapping("/favorites")
	public String myFavorites(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 現在のユーザ取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// お気に入り商品を取得
		model.addAttribute("favoriteItems",
				favoriteService.getFavoriteItemsByUser(currentUser));
		// お気に入り一覧テンプレート
		return "my_favorites";
	}

	// 自分が書いたレビュー一覧
	@GetMapping("/reviews")
	public String myReviews(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		// 現在のユーザ取得
		User currentUser = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		// 自分が投稿したレビュー一覧を取得
		model.addAttribute("reviews", reviewService.getReviewsByReviewer(currentUser));
		// レビュー一覧テンプレート
		return "user_reviews";
	}
}
