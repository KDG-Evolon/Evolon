package com.example.evolon.controller;

import org.springframework.beans.factory.annotation.Value;

// 設定値の読み込み

// 認証中のユーザ情報取得
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
// Spring MVC のアノテーション
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
// リダイレクト時のメッセージ用
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.evolon.entity.User;
import com.example.evolon.service.AppOrderService;
import com.example.evolon.service.ItemService;
import com.example.evolon.service.UserService;
// Stripe 例外と Intent
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

@Controller
@RequestMapping("/orders")
public class AppOrderController {
	// 依存サービス
	private final AppOrderService appOrderService;
	private final UserService userService;
	private final ItemService itemService;
	// 公開キー（Stripe Elements 用）
	@Value("${stripe.public.key}")
	private String stripePublicKey;

	// 依存を受け取るコンストラクタ
	public AppOrderController(AppOrderService appOrderService, UserService userService,
			ItemService itemService) {
		// フィールドに代入
		this.appOrderService = appOrderService;
		this.userService = userService;
		this.itemService = itemService;
	}

	// 購入開始：PaymentIntent 作成→client_secret を Flash で渡す
	@PostMapping("/initiate-purchase")
	public String initiatePurchase(
			// 認証済みユーザを受け取る
			@AuthenticationPrincipal UserDetails userDetails,
			// フォームから商品 ID
			@RequestParam("itemId") Long itemId,
			// リダイレクト先へ一時メッセージを渡す
			RedirectAttributes redirectAttributes) {
		// 買い手ユーザをメールから取得
		User buyer = userService.getUserByEmail(userDetails.getUsername())
				.orElseThrow(() -> new RuntimeException("Buyer not found"));
		try {
			// Stripe に Intent 作成→注文はサービス側で“決済待ち”作成
			PaymentIntent paymentIntent = appOrderService.initiatePurchase(itemId, buyer);
			// クライアント用に client_secret と itemId を Flash に積む
			redirectAttributes.addFlashAttribute("clientSecret",
					paymentIntent.getClientSecret());
			redirectAttributes.addFlashAttribute("itemId", itemId);
			// 確認画面へ
			return "redirect:/orders/confirm-payment";
		} catch (IllegalStateException | IllegalArgumentException | StripeException e) {
			// 失敗時はエラーメッセージを載せて商品詳細に戻す
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
			return "redirect:/items/" + itemId;
		}
	}

	// Stripe Elements での支払い確認画面
	@GetMapping("/confirm-payment")
	public String confirmPayment(
			// Flash から clientSecret（無ければ一覧へ）
			@ModelAttribute("clientSecret") String clientSecret,
			// Flash から itemId（無ければ一覧へ）
			@ModelAttribute("itemId") Long itemId,
			// 画面に値を渡す
			Model model) {
		// データ欠如時は一覧に戻す
		if (clientSecret == null || itemId == null) {
			return "redirect:/items";
		}
		// テンプレートに値を積む
		model.addAttribute("clientSecret", clientSecret);
		model.addAttribute("itemId", itemId);
		model.addAttribute("stripePublicKey", stripePublicKey);
		// 確認用テンプレートを返す
		return "payment_confirmation";
	}

	// フロント(Stripe.js)で決済成功後の完了処理
	@GetMapping("/complete-purchase")
	public String completePurchase(
			// Stripe から受け取った PaymentIntent ID
			@RequestParam("paymentIntentId") String paymentIntentId,
			// リダイレクト用のフラッシュ属性
			RedirectAttributes redirectAttributes) {
		try {
			// サービスで“安全に”購入確定
			appOrderService.completePurchase(paymentIntentId);
			// 成功メッセージ
			redirectAttributes.addFlashAttribute("successMessage", "商品を購入しました！");
			// 直近の購入済 ID が取れればレビュー画面へ
			return appOrderService.getLatestCompletedOrderId()
					.map(orderId -> "redirect:/reviews/new/" + orderId)
					.orElseGet(() -> {
						// 取れない場合は注文履歴へ
						redirectAttributes.addFlashAttribute("errorMessage", "購入は完了しましたが、評価ページへのリダイレクトに失敗しました。");
						return "redirect:/my-page/orders";
					});
		} catch (StripeException | IllegalStateException e) {
			// 例外時はメッセージを積んで一覧へ
			redirectAttributes.addFlashAttribute("errorMessage", "決済処理中にエラーが発生しました: " + e.getMessage());
			return "redirect:/items";
		}
	}

	// Stripe Webhook エンドポイント（署名検証は本番で必須：ここでは受信確認のみ）
	@PostMapping("/stripe-webhook")
	public void handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
		// 本番では sigHeader で署名検証を必ず行う
		System.out.println("Received Stripe Webhook: " + payload);
		// 例: event type ごとに処理を分岐
	}

	// 出品者の発送操作
	@PostMapping("/{id}/ship")
	public String shipOrder(@PathVariable("id") Long orderId, RedirectAttributes redirectAttributes) {
		try {
			// サービスで発送済みに更新＋通知
			appOrderService.markOrderAsShipped(orderId);
			// 成功メッセージ
			redirectAttributes.addFlashAttribute("successMessage", "商品を発送済みにしました。");
		} catch (IllegalArgumentException e) {
			// エラーメッセージ
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		// マイページ売上へ戻る
		return "redirect:/my-page/sales";
	}
}