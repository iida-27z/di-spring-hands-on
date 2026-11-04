package com.example;

import java.time.LocalDate;
import java.util.List;

/**
 * モックの誕生日リポジトリ実装。
 * 実際のデータベースにはアクセスせず、常に固定の結果を返す。
 */
public class MockBirthdayRepository implements BirthdayRepository {

    @Override
    public List<Person> findAll() {
        return List.of(); // 空リストを返すモック
    }

    @Override
    public int insert(String name, LocalDate birthDate) {
        return 0; // 登録失敗のモック
    }

    @Override
    public int deleteById(int id) {
        return 0; // 削除失敗のモック
    }

}
