package com.example.evolon.service;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.evolon.dto.ParsedCardNumber;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OcrService {

	/**
	 * カード番号専用OCR（下部領域のみ）
	 * 例: HMC 299/742 → setCode=MC, cardNumber=299/742
	 */
	public ParsedCardNumber extractCardNumberOnly(MultipartFile imageFile) throws IOException {

		BufferedImage original = ImageIO.read(imageFile.getInputStream());
		if (original == null) {
			throw new IllegalArgumentException("画像の読み込みに失敗しました");
		}

		// ① 下部領域を切り出し
		BufferedImage cropped = cropBottomArea(original);

		// ② グレースケール化
		BufferedImage gray = new BufferedImage(
				cropped.getWidth(),
				cropped.getHeight(),
				BufferedImage.TYPE_BYTE_GRAY);

		Graphics g = gray.getGraphics();
		g.drawImage(cropped, 0, 0, null);
		g.dispose();

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(gray, "png", baos);
		ByteString imgBytes = ByteString.copyFrom(baos.toByteArray());

		// ③ Vision API（TEXT_DETECTION）
		Image image = Image.newBuilder()
				.setContent(imgBytes)
				.build();

		Feature feature = Feature.newBuilder()
				.setType(Feature.Type.TEXT_DETECTION)
				.build();

		AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
				.setImage(image)
				.addFeatures(feature)
				.build();

		try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {

			BatchAnnotateImagesResponse response = client.batchAnnotateImages(List.of(request));

			AnnotateImageResponse res = response.getResponses(0);

			if (res.hasError()) {
				throw new RuntimeException(
						"Vision API Error: " + res.getError().getMessage());
			}

			if (res.getTextAnnotationsList().isEmpty()) {
				log.warn("カード番号OCR結果なし");
				return ParsedCardNumber.invalid();
			}

			String ocrText = res.getTextAnnotations(0).getDescription();
			log.info("===== CARD NUMBER OCR =====\n{}", ocrText);

			return parseCardNumber(ocrText);
		}
	}

	/* =========================
	 * 下部約28%を切り出す（カード番号領域）
	 * ========================= */
	private BufferedImage cropBottomArea(BufferedImage original) {

		int w = original.getWidth();
		int h = original.getHeight();

		int cropY = (int) (h * 0.72);
		int cropHeight = h - cropY;

		return original.getSubimage(0, cropY, w, cropHeight);
	}

	/* =========================
	 * OCR文字列 → setCode / cardNumber 抽出（安全版）
	 * ========================= */
	private ParsedCardNumber parseCardNumber(String text) {
		if (text == null || text.isBlank()) {
			log.warn("❌ カード番号抽出失敗: 空文字");
			return ParsedCardNumber.invalid();
		}
		String cleaned = text
				// Hsv8a / Isv8a / Jsv8a → sv8a
				.replaceAll("(?i)\\b[HIJ](sv|m)", "$1")
				// 従来の単独 H/I/J 行削除
				.replaceAll("(?m)^\\s*[HIJ]\\s+", "")
				// OCR対策
				.replaceAll("\\s+", " ");

		// 先頭の H/I/J を単語単位で削除（必要なら複数行対応）
		//String cleaned = text.replaceAll("(?m)^\\s*[HIJ]\\s+", "");

		// sv/m で始まるセットコードを全体から検索
		Pattern setCodePattern = Pattern.compile("\\b(sv|m)[a-z0-9]{1,4}\\b", Pattern.CASE_INSENSITIVE);
		Matcher setCodeMatcher = setCodePattern.matcher(cleaned);

		if (setCodeMatcher.find()) {
			String setCode = setCodeMatcher.group().toLowerCase();

			// セットコードの後ろ 200文字以内にカード番号があるか探す
			int start = setCodeMatcher.end();
			String tail = cleaned.substring(start, Math.min(start + 200, cleaned.length()));
			Pattern numberPattern = Pattern.compile("(\\d{1,3}/\\d{1,3})");
			Matcher numberMatcher = numberPattern.matcher(tail);

			if (numberMatcher.find()) {
				String cardNumber = numberMatcher.group();

				// v8a → sv8a 補正
				if (setCode.matches("^v\\d")) {
					setCode = "s" + setCode;
				}

				log.info("🎯 抽出成功 setCode={}, cardNumber={}", setCode, cardNumber);
				return new ParsedCardNumber(setCode, cardNumber);
			}
		}

		log.warn("❌ カード番号抽出失敗");
		return ParsedCardNumber.invalid();
	}

}
