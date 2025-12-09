package com.example.evolon.service;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

@Service
public class StripeService {

	// コンストラクタでシークレットキーを設定
	public StripeService(@Value("${stripe.api.secretKey}") String secretKey) {
		Stripe.apiKey = secretKey;
	}

	/**
	 * 支払い意図 (PaymentIntent) の作成
	 */
	public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, String description)
			throws StripeException {

		// Stripe 金額は最小単位の整数（JPY は小数なし）
		long value = "jpy".equalsIgnoreCase(currency)
				? amount.longValue()
				: amount.multiply(new BigDecimal(100)).longValue();

		// パラメータ構築
		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
				.setAmount(value)
				.setCurrency(currency)
				.setDescription(description)
				.setAutomaticPaymentMethods(
						PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
								.setEnabled(true)
								.build())
				.build();

		// PaymentIntent を作成
		return PaymentIntent.create(params);
	}

	/**
	 * 既存の PaymentIntent を取得
	 */
	public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
		return PaymentIntent.retrieve(paymentIntentId);
	}
}
