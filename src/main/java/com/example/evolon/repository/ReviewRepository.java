package com.example.evolon.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.evolon.entity.Review;
import com.example.evolon.entity.ReviewResult;
import com.example.evolon.entity.User;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

	List<Review> findByReviewee(User reviewee);

	Optional<Review> findByOrder_Id(Long orderId);

	boolean existsByOrder_Id(Long orderId);

	List<Review> findByReviewer(User reviewer);

	// GOOD/BAD件数
	long countByRevieweeAndResult(User reviewee, ReviewResult result);
}
