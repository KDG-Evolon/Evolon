package com.example.evolon.service;

//アップロード結果を受け取る Map を import
import java.util.Map;

//設定値を外部から注入するためのアノテーションを import
import org.springframework.beans.factory.annotation.Value;
//DI 対象のサービスであることを示すアノテーションを import
import org.springframework.stereotype.Service;
//Spring のファイルアップロード表現を import
import org.springframework.web.multipart.MultipartFile;

//Cloudinary の Java SDK のエントリポイントを import
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

//サービス層として登録
@Service
public class CloudinaryService {
	//Cloudinary クライアントの参照
	private final Cloudinary cloudinary;
	private final boolean enabled;

	//必要な認証情報をコンストラクタインジェクションで受け取る
	public CloudinaryService(
			//クラウド名を application.properties から注入
			@Value("${cloudinary.cloud_name:}") String cloudName,
			//API キーを注入
			@Value("${cloudinary.api_key:}") String apiKey,
			//API シークレットを注入
			@Value("${cloudinary.api_secret:}") String apiSecret,
			@Value("${cloudinary.enabled:false}") boolean enabled) {

		this.enabled = enabled;

		//渡された資格情報で Cloudinary クライアントを初期化
		if (enabled) {
			this.cloudinary = new Cloudinary(ObjectUtils.asMap(
					"cloud_name", cloudName,
					"api_key", apiKey,
					"api_secret", apiSecret));
		} else {
			this.cloudinary = null;
		}
	}

	//画像をアップロードして公開 URL を返す（空ファイルは null）
	public String uploadFile(MultipartFile file) {
		if (!enabled || file == null || file.isEmpty()) {
			return null;
		}
		try {
			Map uploadResult = cloudinary.uploader().upload(
					file.getBytes(),
					ObjectUtils.emptyMap());
			return uploadResult.get("url").toString();
		} catch (Exception e) {
			System.err.println("Cloudinary upload failed: " + e.getMessage());
			return null;
		}
	}

	//Cloudinary 上のリソースを削除（URL から public_id を推定）
	public void deleteFile(String publicId) {
		if (!enabled || publicId == null)
			return;
		try {
			String[] parts = publicId.split("/");
			String fileName = parts[parts.length - 1];
			String publicIdWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
			cloudinary.uploader().destroy(
					publicIdWithoutExtension,
					ObjectUtils.emptyMap());
		} catch (Exception e) {
			System.err.println("Cloudinary delete failed: " + e.getMessage());
		}
	}
}
