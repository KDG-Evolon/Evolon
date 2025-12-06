package com.example.evolon.repository;

//取得結果のリスト型
import java.util.List;

//Spring Data JPA
import org.springframework.data.jpa.repository.JpaRepository;
//リポジトリ・ステレオタイプ
import org.springframework.stereotype.Repository;

//エンティティのインポート
import com.example.evolon.entity.Chat;
import com.example.evolon.entity.Item;

//Chat エンティティ用のリポジトリ
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
	//指定商品のチャット履歴を作成日時昇順で取得
	List<Chat> findByItemOrderByCreatedAtAsc(Item item);
}
