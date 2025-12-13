package com.example.evolon.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.evolon.entity.Category;
import com.example.evolon.repository.CategoryRepository;

@Configuration
public class InitCategoryRunner {

	@Bean
	CommandLineRunner initCategories(CategoryRepository categories) {
		return args -> {

			// すでにカテゴリがある場合は何もしない
			if (categories.count() > 0) {
				return;
			}

			categories.save(new Category(null, "カード"));
			categories.save(new Category(null, "サプライ"));
			categories.save(new Category(null, "デッキ・構築済み"));
			categories.save(new Category(null, "その他"));

			System.out.println("✅ 初期カテゴリを登録しました");
		};
	}
}
