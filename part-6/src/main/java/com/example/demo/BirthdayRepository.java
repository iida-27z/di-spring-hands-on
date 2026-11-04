package com.example.demo;

import java.time.LocalDate;
import java.util.List;

/**
 * 誕生日情報の永続化を抽象化するインターフェース。
 */
public interface BirthdayRepository {

    /**
     * 登録されている誕生日情報を ID の昇順ですべて取得する。
     *
     * @return 誕生日情報の一覧
     */
    List<Person> findAll();

    /**
     * 新しい誕生日情報を保存する。
     *
     * @param name ユーザー名
     * @param birthDate 誕生日
     * @return 保存された行数
     */
    int insert(String name, LocalDate birthDate);

    /**
     * 指定した ID の誕生日情報を削除する。
     *
     * @param id 削除対象の識別子
     * @return 削除された行数
     */
    int deleteById(int id);
}
