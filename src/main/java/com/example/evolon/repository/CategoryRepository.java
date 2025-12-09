// パッケージ宣言
package com.example.evolon.repository;

// Optional を返すメソッドで利用
import java.util.Optional;

// Spring Data JPA の基底インタフェース
import org.springframework.data.jpa.repository.JpaRepository;
// コンポーネントスキャン対象にするためのアノテーション
import org.springframework.stereotype.Repository;

// エンティティ Category を扱うためのインポート
import com.example.evolon.entity.Category;

// リポジトリ宣言：Category エンティティ + 主キー型 Long
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
	// カテゴリ名から一件検索（ユニーク想定のため Optional）
	Optional<Category> findByName(String name);
}