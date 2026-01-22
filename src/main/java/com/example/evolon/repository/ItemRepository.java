package com.example.evolon.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.evolon.domain.enums.CardCondition;
import com.example.evolon.domain.enums.Rarity;
import com.example.evolon.domain.enums.Regulation;
import com.example.evolon.entity.Item;
import com.example.evolon.entity.ItemStatus;
import com.example.evolon.entity.User;

public interface ItemRepository extends JpaRepository<Item, Long> {

	/* =========================
	 * 公開中（出品中）検索
	 * ========================= */

	// ステータス複数（一覧用）
	Page<Item> findByStatusIn(List<ItemStatus> statuses, Pageable pageable);

	// 名前 + ステータス複数
	Page<Item> findByNameContainingIgnoreCaseAndStatusIn(
	        String name,
	        List<ItemStatus> statuses,
	        Pageable pageable);

	// カテゴリ + ステータス複数
	Page<Item> findByCategory_IdAndStatusIn(
	        Long categoryId,
	        List<ItemStatus> statuses,
	        Pageable pageable);

	// 名前 + カテゴリ + ステータス複数
	Page<Item> findByNameContainingIgnoreCaseAndCategory_IdAndStatusIn(
	        String name,
	        Long categoryId,
	        List<ItemStatus> statuses,
	        Pageable pageable);

	/* =========================
	 * ★ カード条件検索（null は条件に入れない）
	 *
	 * - LEFT JOIN にしている理由：
	 *   cardInfo が null の商品が存在しても落ちないようにするため
	 * - cardName/rarity/condition... を指定した場合は、
	 *   実質 cardInfo がある商品がヒットする
	 * ========================= */
	@Query("""
		    SELECT i
		    FROM Item i
		    LEFT JOIN i.cardInfo ci
		    WHERE i.status IN :statuses
		      AND (:cardName IS NULL OR ci.cardName LIKE %:cardName%)
		      AND (:rarity IS NULL OR ci.rarity = :rarity)
		      AND (:regulation IS NULL OR ci.regulation = :regulation)
		      AND (:condition IS NULL OR ci.condition = :condition)
		      AND (:packName IS NULL OR ci.packName LIKE %:packName%)
		      AND (:minPrice IS NULL OR i.price >= :minPrice)
		      AND (:maxPrice IS NULL OR i.price <= :maxPrice)
		""")
		Page<Item> searchByCardFilters(
		    @Param("statuses") List<ItemStatus> statuses,
		    @Param("cardName") String cardName,
		    @Param("rarity") Rarity rarity,
		    @Param("regulation") Regulation regulation,
		    @Param("condition") CardCondition condition,
		    @Param("packName") String packName,
		    @Param("minPrice") BigDecimal minPrice,
		    @Param("maxPrice") BigDecimal maxPrice,
		    Pageable pageable);



	/* =========================
	 * 出品者
	 * ========================= */

	// 出品者の全商品
	List<Item> findBySeller(User seller);

	// 出品者 + ステータス（SELLING / SOLD / SUSPENDED など）
	List<Item> findBySellerAndStatus(
			User seller,
			ItemStatus status);

	/* =========================
	 * 管理・ダッシュボード用
	 * ========================= */

	// 最近の出品商品
	List<Item> findTop5ByOrderByCreatedAtDesc();

}
