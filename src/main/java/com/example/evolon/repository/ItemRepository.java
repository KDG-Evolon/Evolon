// パッケージ宣言
package com.example.evolon.repository;

// 一覧取得で使用
import java.util.List;

// ページング用の型
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;
// リポジトリアノテーション
import org.springframework.stereotype.Repository;

// エンティティのインポート
import com.example.evolon.entity.Item;
import com.example.evolon.entity.User;

// Item エンティティのリポジトリ
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
	// 名前の部分一致 + ステータスでページング検索（大文字小文字無視）
	Page<Item> findByNameContainingIgnoreCaseAndStatus(String name, String status, Pageable pageable);

	// カテゴリ ID + ステータスでページング検索
	Page<Item> findByCategoryIdAndStatus(Long categoryId, String status, Pageable pageable);

	// 名前の部分一致 + カテゴリ ID + ステータスでページング検索
	Page<Item> findByNameContainingIgnoreCaseAndCategoryIdAndStatus(String name, Long categoryId, String status,
			Pageable pageable);

	// ステータスのみでページング取得（公開中一覧など）
	Page<Item> findByStatus(String status, Pageable pageable);

	// 出品者ごとの商品一覧
	List<Item> findBySeller(User seller);

	// 最近の出品商品を取得（管理者ダッシュボード用）
	List<Item> findTop5ByOrderByCreatedAtDesc();

}