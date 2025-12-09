package com.example.evolon.service;

import java.util.List;
import java.util.OptionalDouble;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.AppOrder;
import com.example.evolon.entity.Review;
import com.example.evolon.entity.User;
import com.example.evolon.repository.AppOrderRepository;
import com.example.evolon.repository.ReviewRepository;

@Service
public class ReviewService {

	private final ReviewRepository reviewRepository;
	private final AppOrderRepository appOrderRepository;

	public ReviewService(ReviewRepository reviewRepository,
			AppOrderRepository appOrderRepository) {
		this.reviewRepository = reviewRepository;
		this.appOrderRepository = appOrderRepository;
	}

	/**
	 * レビュー投稿（買い手のみ、1注文1レビュー）
	 */
	@Transactional
	public Review submitReview(Long orderId, User reviewer, int rating, String comment) {

		// 注文取得（存在しなければ例外）
		AppOrder order = appOrderRepository.findById(orderId)
				.orElseThrow(() -> new IllegalArgumentException("Order not found."));

		// 買い手本人かチェック
		if (!order.getBuyer().getId().equals(reviewer.getId())) {
			throw new IllegalStateException("Only the buyer can review this order.");
		}

		// 既にレビュー済みかチェック
		if (reviewRepository.findByOrderId(orderId).isPresent()) {
			throw new IllegalStateException("This order has already been reviewed.");
		}

		// 新規レビュー作成
		Review review = new Review();
		review.setOrder(order);
		review.setReviewer(reviewer);
		review.setSeller(order.getItem().getSeller());
		review.setItem(order.getItem());
		review.setRating(rating);
		review.setComment(comment);

		// 保存して返却
		return reviewRepository.save(review);
	}

	/**
	 * 出品者へのレビュー一覧取得
	 */
	public List<Review> getReviewsBySeller(User seller) {
		return reviewRepository.findBySeller(seller);
	}

	/**
	 * 出品者の平均評価
	 */
	public OptionalDouble getAverageRatingForSeller(User seller) {
		return reviewRepository.findBySeller(seller).stream()
				.mapToInt(Review::getRating)
				.average();
	}

	/**
	 * レビュワー自身のレビュー一覧
	 */
	public List<Review> getReviewsByReviewer(User reviewer) {
		return reviewRepository.findByReviewer(reviewer);
	}
}
