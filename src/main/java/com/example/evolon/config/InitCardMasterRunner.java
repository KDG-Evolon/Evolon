package com.example.evolon.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.evolon.domain.enums.PrintedRegulation;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.entity.CardMaster;
import com.example.evolon.repository.CardMasterRepository;

@Configuration
public class InitCardMasterRunner {

	@Bean
	CommandLineRunner initCardMasters(CardMasterRepository cardMasters) {
		return args -> {

			String setCode = "sv8a";
			String cardNumber = "212/187";

			if (cardMasters.existsBySetCodeAndCardNumber(setCode, cardNumber)) {
				System.out.println("ℹ️ card_master already exists, skip insert");
				return;
			}
			cardMasters.save(
					new CardMaster(
							null,
							setCode,
							cardNumber,
							"ニンフィアex",
							Rarity.SAR,
							"テラスタルフェスex",
							PrintedRegulation.H));

			System.out.println("✅ 初期カードマスタを登録しました");
		};
	}

}
