package com.example.evolon.controller;

//認証ユーザを引数で受けるためのアノテーション
import org.springframework.security.core.annotation.AuthenticationPrincipal;
//Spring Security のユーザ詳細型
import org.springframework.security.core.userdetails.UserDetails;
//MVC コントローラのアノテーション
import org.springframework.stereotype.Controller;
//画面へ値を渡す Model
import org.springframework.ui.Model;
//ルーティングアノテーション群
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
//リダイレクト先へ一時メッセージを渡すための型
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

//注文エンティティ（フォームに表示するため）
import com.example.evolon.entity.AppOrder;
//ユーザエンティティ（レビュワーの解決に使用）
import com.example.evolon.entity.User;
//注文サービス（注文取得に使用）
import com.example.evolon.service.AppOrderService;
//レビューサービス（バリデーションと保存に使用）
import com.example.evolon.service.ReviewService;
//ユーザサービス（ログインユーザの解決に使用）
import com.example.evolon.service.UserService;

//MVC コントローラとして登録
@Controller
//ベースパス /reviews にマッピング
@RequestMapping("/reviews")
public class ReviewController {
	//レビューサービスの参照
	private final ReviewService reviewService;
	//注文サービスの参照
	private final AppOrderService appOrderService;
	//ユーザサービスの参照
	private final UserService userService;

	//依存関係をコンストラクタで受け取る
	public ReviewController(ReviewService reviewService, AppOrderService appOrderService,
			UserService userService) {
		//フィールドへ代入
		this.reviewService = reviewService;
		//フィールドへ代入
		this.appOrderService = appOrderService;
		//フィールドへ代入
		this.userService = userService;
	}

	//新規レビューの入力フォーム表示
	@GetMapping("/new/{orderId}")
	public String showReviewForm(@PathVariable("orderId") Long orderId, Model model) {
		//注文を取得（見つからなければ例外）
		AppOrder order = appOrderService.getOrderById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));
		//テンプレートへ注文を渡す（商品名などの表示用）
		model.addAttribute("order", order);
		//フォームのテンプレートを返却
		return "review_form";
	}

	//レビュー投稿の送信
	@PostMapping
	public String submitReview(
			//認証ユーザを受け取る
			@AuthenticationPrincipal UserDetails userDetails,
			//評価対象の注文 ID
			@RequestParam("orderId") Long orderId,
			//評点（1〜5 想定）
			@RequestParam("rating") int rating,
			//任意コメント
			@RequestParam("comment") String comment,
			//リダイレクト先へメッセージを渡す
			RedirectAttributes redirectAttributes) {
		//ログインユーザを取得（見つからなければ例外）
		User reviewer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("User not found"));
		try {
			//サービス層で検証＋保存を実施
			reviewService.submitReview(orderId, reviewer, rating, comment);
			//成功メッセージを設定
			redirectAttributes.addFlashAttribute("successMessage", "評価を送信しました！");
		} catch (IllegalStateException | IllegalArgumentException e) {
			//ルール違反などのエラーをメッセージで返す
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		//購入者の注文履歴へ遷移
		return "redirect:/my-page/orders";
	}
}
