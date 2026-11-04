package com.example;

import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * 誕生日情報の永続化を担当するクラス。
 */
public class BirthdayRepository {

    private final JdbcClient jdbcClient;

    /**
     * コンストラクタ。
     * 
     * @param dataSource データベース接続情報を提供する DataSource のインスタンス
     */
    public BirthdayRepository(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    /**
     * 登録されている誕生日情報を ID の昇順ですべて取得する。
     *
     * @return 誕生日情報の一覧
     */
    public List<Person> findAll() {
        return jdbcClient.sql("SELECT id, name, birth_date FROM birthdays ORDER BY id")
                .query(Person.class)
                .list();
    }

    /**
     * 新しい誕生日情報をデータベースに登録する。
     *
     * @param name ユーザー名
     * @param birthDate 誕生日
     * @return 登録された行数
     */
    public int insert(String name, LocalDate birthDate) {
        return jdbcClient.sql("INSERT INTO birthdays (name, birth_date) VALUES (:name, :birthDate)")
                .param("name", name)
                .param("birthDate", birthDate)
                .update();
    }

    /**
     * 指定した ID の誕生日情報を削除する。
     *
     * @param id 削除対象の識別子
     * @return 削除された行数
     */
    public int deleteById(int id) {
        return jdbcClient.sql("DELETE FROM birthdays WHERE id = :id")
                .param("id", id)
                .update();
    }
}
