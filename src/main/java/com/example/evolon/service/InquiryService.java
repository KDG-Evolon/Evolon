package com.example.evolon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.evolon.entity.Inquiry;
import com.example.evolon.entity.User;
import com.example.evolon.repository.InquiryRepository;

@Service
public class InquiryService {

	private final InquiryRepository inquiryRepository;

	public InquiryService(InquiryRepository inquiryRepository) {
		this.inquiryRepository = inquiryRepository;
	}

	// 管理者用：全件取得（Userもロード）
	public List<Inquiry> getAllInquiries() {
		return inquiryRepository.findAllWithUser();
	}

	// 管理者用：未対応のみ
	public List<Inquiry> getOpenInquiries() {
		return inquiryRepository.findByStatusWithUser("OPEN");
	}

	// 管理者用：詳細取得（Userもロード）
	public Inquiry getInquiryById(Long id) {
		return inquiryRepository.findByIdWithUser(id).orElse(null);
	}

	// 管理者の返信
	@Transactional
	public Inquiry replyToInquiry(Long inquiryId, String adminReply) {
		Inquiry inquiry = inquiryRepository.findByIdWithUser(inquiryId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid inquiry ID"));

		inquiry.setAdminReply(adminReply);
		inquiry.setStatus("CLOSED");
		inquiry.setRepliedAt(LocalDateTime.now());

		return inquiryRepository.save(inquiry);
	}

	// 対応済みにする（返信なし）
	@Transactional
	public Inquiry closeInquiry(Long inquiryId) {
		Inquiry inquiry = inquiryRepository.findByIdWithUser(inquiryId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid inquiry ID"));

		inquiry.setStatus("CLOSED");
		inquiry.setRepliedAt(LocalDateTime.now());

		return inquiryRepository.save(inquiry);
	}

	// ユーザー向け一覧
	public List<Inquiry> getInquiriesByUser(Long userId) {
		return inquiryRepository.findByUserId(userId);
	}

	// ユーザー向け：新規問い合わせ
	@Transactional
	public Inquiry createInquiry(int userId, String subject, String message) {
		Inquiry inquiry = new Inquiry();

		User user = new User();
		user.setId((long) userId);
		inquiry.setUser(user);

		inquiry.setSubject(subject);
		inquiry.setMessage(message);
		inquiry.setStatus("OPEN");
		inquiry.setCreatedAt(LocalDateTime.now());

		return inquiryRepository.save(inquiry);
	}

	@Transactional
	public Inquiry createInquiry(Inquiry inquiry) {
		// user はすでにセットされている想定
		if (inquiry.getUser() == null) {
			throw new IllegalArgumentException("User must be set");
		}

		inquiry.setStatus("OPEN");
		inquiry.setCreatedAt(LocalDateTime.now());

		return inquiryRepository.save(inquiry);
	}

}
