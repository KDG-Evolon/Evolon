package com.example.evolon.entity;

//JPA アノテーションを利用するためのインポート
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//Lombok でゲッター/セッター等を自動生成
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

//このクラスが JPA エンティティであることを宣言
@Entity
//対応するテーブル名を明示（category）
@Table(name = "category")
//Lombok：getter/setter, toString, equals/hashCode を自動生成
@Data
//Lombok：引数なしコンストラクタを生成
@NoArgsConstructor
//Lombok：全フィールドを引数に持つコンストラクタを生成
@AllArgsConstructor
public class Category {
	//主キーを表すフィールド
	@Id
	//主キーの採番戦略：DB の IDENTITY(シリアル)に委譲
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	//NOT NULL かつ一意制約を付与したカテゴリ名
	@Column(nullable = false, unique = true)
	private String name;
}
