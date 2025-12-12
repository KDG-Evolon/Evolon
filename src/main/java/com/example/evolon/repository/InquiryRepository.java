package com.example.evolon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.evolon.entity.Inquiry;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    // ユーザーIDで検索（ユーザー向け一覧）
    List<Inquiry> findByUserId(Long userId);

    // ステータスで検索（未対応・対応済を絞り込み）
    List<Inquiry> findByStatus(String status);

    // 管理者用：User情報をJOIN FETCHして全件取得（Lazy回避）
    @Query("SELECT i FROM Inquiry i JOIN FETCH i.user")
    List<Inquiry> findAllWithUser();

    // 管理者用：ステータスで絞り込みつつUserをJOIN FETCH
    @Query("SELECT i FROM Inquiry i JOIN FETCH i.user WHERE i.status = :status")
    List<Inquiry> findByStatusWithUser(@Param("status") String status);

    // 管理者用：ID指定でUser情報も一緒に取得（詳細画面向け）
    @Query("SELECT i FROM Inquiry i JOIN FETCH i.user WHERE i.id = :id")
    Optional<Inquiry> findByIdWithUser(@Param("id") Long id);
}
