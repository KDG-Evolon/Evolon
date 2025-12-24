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

	public StripeService(
			@Value("${stripe.secret-key}") String secretKey) {
		Stripe.apiKey = secretKey;
	}

	public PaymentIntent createPaymentIntent(
			BigDecimal amount,
			String currency,
			String description) throws StripeException {

		long value = "jpy".equalsIgnoreCase(currency)
				? amount.longValue()
				: amount.multiply(BigDecimal.valueOf(100)).longValue();

		PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
				.setAmount(value)
				.setCurrency(currency)
				.setDescription(description)
				.setAutomaticPaymentMethods(
						PaymentIntentCreateParams.AutomaticPaymentMethods
								.builder()
								.setEnabled(true)
								.build())
				.build();

		return PaymentIntent.create(params);
	}

	/** 決済が成功しているか */
	public boolean isPaymentSucceeded(String paymentIntentId)
			throws StripeException {

		PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
		return "succeeded".equals(paymentIntent.getStatus());
	}

	/** 生の PaymentIntent が必要な場合用 */
	public PaymentIntent getPaymentIntent(String paymentIntentId)
			throws StripeException {

		return PaymentIntent.retrieve(paymentIntentId);
	}
}
