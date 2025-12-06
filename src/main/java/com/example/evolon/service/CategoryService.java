package com.example.evolon.service;

//一覧返却に使う List を import
import java.util.List;
//Optional を返すために import
import java.util.Optional;

//DI 対象サービスを示すアノテーションを import
import org.springframework.stereotype.Service;

import com.example.evolon.entity.Category;
import com.example.evolon.repository.CategoryRepository;

//サービス層として登録
@Service
public class CategoryService {
	//カテゴリリポジトリの参照
	private final CategoryRepository categoryRepository;

	//依存性をコンストラクタで注入
	public CategoryService(CategoryRepository categoryRepository) {
		//フィールドへ設定
		this.categoryRepository = categoryRepository;
	}

	//すべてのカテゴリを取得
	public List<Category> getAllCategories() {
		//全件取得を委譲
		return categoryRepository.findAll();
	}

	//主キーでカテゴリを取得
	public Optional<Category> getCategoryById(Long id) {
		//Optional をそのまま返す
		return categoryRepository.findById(id);
	}

	//名称でカテゴリを取得（名称は一意前提）
	public Optional<Category> getCategoryByName(String name) {
		//名称検索を委譲
		return categoryRepository.findByName(name);
	}

	//新規/更新保存
	public Category saveCategory(Category category) {
		//save に委譲
		return categoryRepository.save(category);
	}

	//削除
	public void deleteCategory(Long id) {
		//ID 指定で削除
		categoryRepository.deleteById(id);
	}
}
