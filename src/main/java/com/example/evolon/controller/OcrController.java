package com.example.evolon.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.evolon.dto.ParsedCardNumber;
import com.example.evolon.service.CardMasterService;
import com.example.evolon.service.OcrService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

	private final OcrService ocrService;
	private final CardMasterService cardMasterService;

	@PostMapping
	public ResponseEntity<?> ocr(@RequestParam("image") MultipartFile image)
			throws IOException {

		ParsedCardNumber parsed = ocrService.extractCardNumberOnly(image);

		if (!parsed.isValid()) {
			Map<String, Object> body = new HashMap<>();
			body.put("parsed", null);
			body.put("card", null);
			body.put("message", "カード番号を検出できませんでした");
			return ResponseEntity.ok(body);
		}

		return cardMasterService.findByParsedNumber(parsed)
				.map(card -> {
					Map<String, Object> body = new HashMap<>();
					body.put("parsed", parsed);
					body.put("card", card);
					return ResponseEntity.ok(body);
				})
				.orElseGet(() -> {
					Map<String, Object> body = new HashMap<>();
					body.put("parsed", parsed);
					body.put("card", null);
					body.put("message", "カードマスタ未登録");
					return ResponseEntity.ok(body);
				});
	}

}
