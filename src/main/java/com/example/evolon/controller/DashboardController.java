package com.example.evolon.controller;

//認証ユーザを受け取るためのアノテーション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//認証ユーザの詳細型
import org.springframework.security.core.userdetails.UserDetails;
//MVC コントローラのアノテーション
import org.springframework.stereotype.Controller;
//画面へ値を渡す Model
import org.springframework.ui.Model;
//GET ハンドラのアノテーション
import org.springframework.web.bind.annotation.GetMapping;

//// ユーザエンティティの取得に使うリポジトリ
import com.example.evolon.repository.UserRepository;
//注文の一覧取得に使うサービス
import com.example.evolon.service.AppOrderService;
//商品の一覧取得に使うサービス
import com.example.evolon.service.ItemService;

//MVC コントローラとして登録
@Controller
public class DashboardController {
	//ユーザ検索に使うリポジトリ
	private final UserRepository userRepository;
	//商品サービスの参照
	private final ItemService itemService;
	//注文サービスの参照
	private final AppOrderService appOrderService;

	//依存をコンストラクタ注入
	public DashboardController(UserRepository userRepository, ItemService itemService,
			AppOrderService appOrderService) {
		//フィールドへ設定
		this.userRepository = userRepository;
		//フィールドへ設定
		this.itemService = itemService;
		//フィールドへ設定
		this.appOrderService = appOrderService;
	}

	//ダッシュボード画面のハンドラ
	@GetMapping("/dashboard")
	public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
		//ログインユーザをメールで検索
		com.example.evolon.entity.User currentUser = userRepository.findByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		//管理者であれば管理ダッシュボード表示
		if (currentUser.getRole().equals("ADMIN")) {
			//最近の商品を表示（ここでは全件）
			model.addAttribute("recentItems", itemService.getAllItems());
			//最近の注文を表示（ここでは全件）
			model.addAttribute("recentOrders", appOrderService.getAllOrders());
			//管理者用テンプレートへ
			return "admin_dashboard";
		} else {
			//一般ユーザは商品一覧へ誘導
			return "redirect:/items";
		}
	}
}
