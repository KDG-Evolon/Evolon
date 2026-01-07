package com.example.evolon.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.Review;
import com.example.evolon.entity.ReviewResult;
import com.example.evolon.entity.User;
import com.example.evolon.repository.ReviewRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

	private final ReviewRepository reviewRepository;

	public long countGoodForSeller(User seller) {
		return reviewRepository.countByRevieweeAndResult(seller, ReviewResult.GOOD);
	}

	public long countBadForSeller(User seller) {
		return reviewRepository.countByRevieweeAndResult(seller, ReviewResult.BAD);
	}

	@Transactional
	public Review createBuyerToSellerReview(AppOrder order, User reviewer, ReviewResult result, String comment) {

		if (order == null || order.getId() == null) {
			throw new IllegalArgumentException("order が不正です");
		}
		if (reviewer == null) {
			throw new IllegalArgumentException("reviewer が不正です");
		}
		if (result == null) {
			throw new IllegalArgumentException("result が不正です");
		}
		if (comment == null || comment.isBlank()) {
			throw new IllegalArgumentException("comment は必須です");
		}

		// 二重投稿防止（DBの unique(order_id) に到達する前に弾く）
		if (reviewRepository.existsByOrder_Id(order.getId())) {
			throw new IllegalStateException("この注文はすでにレビュー済みです");
		}

		if (order.getItem() == null || order.getItem().getSeller() == null) {
			throw new IllegalStateException("商品または出品者が取得できません");
		}

		User seller = order.getItem().getSeller();

		Review review = new Review();
		review.setOrder(order);
		review.setReviewer(reviewer);

		// 新カラム（本命）
		review.setReviewee(seller);
		review.setResult(result);
		review.setComment(comment);

		// 旧カラム互換（DB NOT NULL を満たすため必須）
		review.setSeller(seller);
		review.setItem(order.getItem());
		review.setRating(result == ReviewResult.GOOD ? 5 : 1); // 暫定変換

		// createdAt は DB が CURRENT_TIMESTAMP を入れる（insertable=false なので触らない）
		return reviewRepository.save(review);
	}
}
