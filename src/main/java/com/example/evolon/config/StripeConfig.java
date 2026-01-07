package com.example.evolon.config;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.stripe.Stripe;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class StripeConfig {

	@Value("${stripe.secret-key}")
	private String secretKey;

	@PostConstruct
	public void init() {
		if ("dummy".equals(secretKey)) {
			log.warn("Stripe secret key is not set. Stripe features are disabled.");
			return;
		}
		Stripe.apiKey = secretKey;
	}
}
