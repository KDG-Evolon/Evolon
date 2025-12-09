package com.example.evolon.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.evolon.entity.Category;
import com.example.evolon.repository.CategoryRepository;

/**
 * カテゴリに関するビジネスロジックを提供するサービス
 */
@Service
public class CategoryService {

	/** カテゴリリポジトリ */
	private final CategoryRepository categoryRepository;

	/** コンストラクタインジェクション */
	public CategoryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	/** すべてのカテゴリを取得 */
	public List<Category> getAllCategories() {
		return categoryRepository.findAll();
	}

	/** ID でカテゴリを取得 */
	public Optional<Category> getCategoryById(Long id) {
		return categoryRepository.findById(id);
	}

	/** 名前でカテゴリを取得（名称は一意想定） */
	public Optional<Category> getCategoryByName(String name) {
		return categoryRepository.findByName(name);
	}

	/** 新規作成・更新 */
	public Category saveCategory(Category category) {
		return categoryRepository.save(category);
	}

	/** カテゴリ削除 */
	public void deleteCategory(Long id) {
		categoryRepository.deleteById(id);
	}
}
