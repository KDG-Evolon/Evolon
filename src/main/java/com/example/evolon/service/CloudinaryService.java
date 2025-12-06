package com.example.evolon.service;

//I/O 例外処理のための import
import java.io.IOException;
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

	//必要な認証情報をコンストラクタインジェクションで受け取る
	public CloudinaryService(
			//クラウド名を application.properties から注入
			@Value("${cloudinary.cloud-name}") String cloudName,
			//API キーを注入
			@Value("${cloudinary.api-key}") String apiKey,
			//API シークレットを注入
			@Value("${cloudinary.api-secret}") String apiSecret) {
		//渡された資格情報で Cloudinary クライアントを初期化
		cloudinary = new Cloudinary(ObjectUtils.asMap(
				"cloud_name", cloudName,
				"api_key", apiKey,
				"api_secret", apiSecret));
	}

	//画像をアップロードして公開 URL を返す（空ファイルは null）
	public String uploadFile(MultipartFile file) throws IOException {
		//アップロードなしのケースは null を返す
		if (file.isEmpty()) {
			return null;
		}
		//バイト配列をそのままアップロード（オプションは既定）
		Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
				ObjectUtils.emptyMap());
		//返却 Map から公開 URL を取り出して返す
		return uploadResult.get("url").toString();
	}

	//Cloudinary 上のリソースを削除（URL から public_id を推定）
	public void deleteFile(String publicId) throws IOException {
		//URL を/で分割して末尾のファイル名を取り出す
		String[] parts = publicId.split("/");
		//配列末尾＝ファイル名部分を取得
		String fileName = parts[parts.length - 1];
		//拡張子を除いた public_id を推定
		String publicIdWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
		//public_id を指定して削除 API を呼び出す
		cloudinary.uploader().destroy(publicIdWithoutExtension, ObjectUtils.emptyMap());
	}
}
