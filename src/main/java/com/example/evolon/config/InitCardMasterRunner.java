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
	CommandLineRunner initCardMasters(CardMasterRepository repo) {
		return args -> {

			insertIfNotExists(
					repo,
					"sv8a",
					"212/187",
					"ニンフィアex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			insertIfNotExists(
					repo,
					"sv8a",
					"206/187",
					"グレイシアex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			insertIfNotExists(
					repo,
					"sv8a",
					"200/187",
					"リーフィアex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			insertIfNotExists(
					repo,
					"sv8a",
					"202/187",
					"ブースターex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			insertIfNotExists(
					repo,
					"sv8a",
					"205/187",
					"シャワーズex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			insertIfNotExists(
					repo,
					"sv8a",
					"209/187",
					"サンダースex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			insertIfNotExists(
					repo,
					"sv8a",
					"211/187",
					"エーフィex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			insertIfNotExists(
					repo,
					"sv8a",
					"217/187",
					"ブラッキーex",
					Rarity.SAR,
					"テラスタルフェスex",
					PrintedRegulation.H);

			System.out.println("✅ card_master 初期データ確認完了");
		};
	}

	private void insertIfNotExists(
			CardMasterRepository repo,
			String setCode,
			String cardNumber,
			String cardName,
			Rarity rarity,
			String packName,
			PrintedRegulation regulation) {

		if (repo.existsBySetCodeAndCardNumber(setCode, cardNumber)) {
			System.out.println("ℹ️ skip: " + setCode + " " + cardNumber);
			return;
		}

		repo.save(new CardMaster(
				null,
				setCode,
				cardNumber,
				cardName,
				rarity,
				packName,
				regulation));

		System.out.println("➕ insert: " + setCode + " " + cardNumber);
	}
}
