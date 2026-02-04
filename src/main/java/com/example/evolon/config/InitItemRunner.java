/*
package com.example.evolon.config;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.evolon.domain.enums.CardCondition;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;
import com.example.evolon.domain.enums.ShippingDuration;
import com.example.evolon.domain.enums.ShippingFeeBurden;
import com.example.evolon.domain.enums.ShippingMethod;
import com.example.evolon.domain.enums.ShippingRegion;
import com.example.evolon.entity.CardInfo;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.ItemStatus;
import com.example.evolon.entity.User;
import com.example.evolon.repository.CardInfoRepository;
import com.example.evolon.repository.ItemRepository;
import com.example.evolon.repository.UserRepository;

@Configuration
public class InitItemRunner {

	@Bean

	CommandLineRunner initItems(
			UserRepository users,
			ItemRepository items,
			CardInfoRepository cardInfos) {

		return args -> {

			User seller = users.findByEmailIgnoreCase("member1@evolon.com").orElse(null);
			if (seller == null) {
				System.out.println("⚠️ member1 がいないのでスキップ");
				return;
			}

			// 既に商品があれば何もしない
			if (!items.findBySeller(seller).isEmpty()) {
				System.out.println("⚠️ 既に商品があるのでスキップ");
				return;
			}

			// =========================
			// ★ テスト商品定義
			// =========================
			List<TestItem> testItems = List.of(
					new TestItem("ピカチュウ SR", 4500, Rarity.SR),
					new TestItem("リザードン SAR", 9800, Rarity.SAR),
					new TestItem("ミュウツー AR", 2200, Rarity.RR),
					new TestItem("イーブイ RR", 1200, Rarity.RR),
					new TestItem("ゲンガー HR", 5600, Rarity.RR));

			for (TestItem t : testItems) {

				// ---------- Item ----------
				Item item = new Item();
				item.setSeller(seller);
				item.setName(t.name());
				item.setDescription("InitItemRunner で投入したテスト商品");
				item.setPrice(BigDecimal.valueOf(t.price()));
				item.setStatus(ItemStatus.SELLING);

				item.setShippingDuration(ShippingDuration.ONE_TO_TWO_DAYS);
				item.setShippingFeeBurden(ShippingFeeBurden.SELLER);
				item.setShippingRegion(ShippingRegion.TOKYO);
				item.setShippingMethod(ShippingMethod.JAPAN_POST);

				item = items.save(item);

				// ---------- CardInfo ----------
				CardInfo card = new CardInfo();
				card.setItem(item);
				card.setCardName(t.name());
				card.setPackName("拡張パック");
				card.setRarity(t.rarity());
				card.setRegulation(Regulation.STANDARD);
				card.setCondition(CardCondition.NEW);

				cardInfos.save(card);
			}

			System.out.println("✅ テスト商品を複数件作成しました");
		};
	}

	// =========================
	// 内部用レコード（Java17+）
	// =========================
	private record TestItem(
			String name,
			int price,
			Rarity rarity) {
	}
}
*/
