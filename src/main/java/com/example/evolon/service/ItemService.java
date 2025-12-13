package com.example.evolon.service;

//I/O 例外処理に必要な import
import java.io.IOException;
//一覧返却で使う List を import
import java.util.List;
//Optional で安全に扱うための import
import java.util.Optional;

//ページング条件の型を import
import org.springframework.data.domain.Page;
//ページングリクエスト生成用の型を import
import org.springframework.data.domain.PageRequest;
//ページングインターフェイスの型を import
import org.springframework.data.domain.Pageable;
//DI 対象のサービスであることを示すアノテーションを import
import org.springframework.stereotype.Service;
//ファイルアップロードに使う MultipartFile を import
import org.springframework.web.multipart.MultipartFile;

//Item エンティティを扱うための import
import com.example.evolon.entity.Item;
//User エンティティを扱うための import
import com.example.evolon.entity.User;
//ページング付き検索で利用するリポジトリを import
import com.example.evolon.repository.ItemRepository;

//サービス層として登録
@Service
public class ItemService {
	//商品リポジトリの参照
	private final ItemRepository itemRepository;
	//カテゴリ関連のユースケースに備えてサービス参照を保持
	private final CategoryService categoryService;
	//画像アップロード/削除のための Cloudinary サービス参照
	private final CloudinaryService cloudinaryService;

	//依存性はコンストラクタで注入
	public ItemService(ItemRepository itemRepository, CategoryService categoryService,
			CloudinaryService cloudinaryService) {
		//フィールドへ商品リポジトリを設定
		this.itemRepository = itemRepository;
		//フィールドへカテゴリサービスを設定
		this.categoryService = categoryService;
		//フィールドへ Cloudinary サービスを設定
		this.cloudinaryService = cloudinaryService;
	}

	//商品検索：キーワード/カテゴリ/ページングを組み合わせ、公開中のみ返す
	public Page<Item> searchItems(String keyword, Long categoryId, int page, int size) {
		//ページング指定を生成
		Pageable pageable = PageRequest.of(page, size);
		//キーワードとカテゴリ両方指定時の検索
		if (keyword != null && !keyword.isEmpty() && categoryId != null) {
			//名前 LIKE×カテゴリ×出品中で検索
			return itemRepository.findByNameContainingIgnoreCaseAndCategoryIdAndStatus(keyword, categoryId, "出品中",
					pageable);
			//キーワードのみ指定時の検索
		} else if (keyword != null && !keyword.isEmpty()) {
			//名前 LIKE×出品中で検索
			return itemRepository.findByNameContainingIgnoreCaseAndStatus(keyword, "出品中",
					pageable);
			//カテゴリのみ指定時の検索
		} else if (categoryId != null) {
			//カテゴリ×出品中で検索
			return itemRepository.findByCategoryIdAndStatus(categoryId, "出品中", pageable);
			//条件未指定時のデフォルト検索
		} else {
			//出品中のみ全件ページングで返す
			return itemRepository.findByStatus("出品中", pageable);
		}
	}

	//全商品一覧を返す（管理用など）
	public List<Item> getAllItems() {
		//リポジトリの全件取得を委譲
		return itemRepository.findAll();
	}

	//主キーで商品を取得
	public Optional<Item> getItemById(Long id) {
		//Optional をそのまま返却
		return itemRepository.findById(id);
	}

	//商品保存：必要なら画像を Cloudinary へアップロードして URL を保存
	public Item saveItem(Item item, MultipartFile imageFile) throws IOException {
		//画像が添付されている場合にのみアップロード処理を実行
		if (imageFile != null && !imageFile.isEmpty()) {
			//Cloudinary へアップロードし URL を受け取る
			String imageUrl = cloudinaryService.uploadFile(imageFile);
			//画像 URL をエンティティへ設定
			item.setImageUrl(imageUrl);
		}
		//商品を保存して返す
		return itemRepository.save(item);
	}

	//商品削除：Cloudinary 上の画像も可能なら削除してから DB 削除
	public void deleteItem(Long id) {
		//まず対象商品を取得し、存在する場合のみ削除処理を進める
		itemRepository.findById(id).ifPresent(item -> {
			//画像 URL がある場合は Cloudinary 側の削除を試みる
			if (item.getImageUrl() != null) {
				// 例外処理は CloudinaryService 側に任せる
				cloudinaryService.deleteFile(item.getImageUrl());
			}
			//最後に DB から商品レコードを削除
			itemRepository.deleteById(id);
		});
	}

	//出品者の出品一覧を取得
	public List<Item> getItemsBySeller(User seller) {
		//seller 条件で検索
		return itemRepository.findBySeller(seller);
	}

	//売却確定：商品ステータスを売却済へ変更
	public void markItemAsSold(Long itemId) {
		//商品を取得して存在する場合のみ更新
		itemRepository.findById(itemId).ifPresent(item -> {
			//ステータスを売却済に変更
			item.setStatus("売却済");
			//変更を保存
			itemRepository.save(item);
		});
	}

	// 管理者ダッシュボード用：最近の出品
	public List<Item> getRecentItems() {
		return itemRepository.findTop5ByOrderByCreatedAtDesc();
	}

}
