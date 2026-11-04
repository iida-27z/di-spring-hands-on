package com.example.demo;

import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * JDBC を使った誕生日リポジトリ実装。
 */
@Repository 
public class JdbcBirthdayRepository implements BirthdayRepository {

    private final JdbcClient jdbcClient;

    /**
     * コンストラクタ。
     * 
     * @param dataSource データベース接続情報を提供する DataSource のインスタンス
     */
    public JdbcBirthdayRepository(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public List<Person> findAll() {
        return jdbcClient.sql("SELECT id, name, birth_date FROM birthdays ORDER BY id")
                .query(Person.class)
                .list();
    }

    @Override
    public int insert(String name, LocalDate birthDate) {
        return jdbcClient.sql("INSERT INTO birthdays (name, birth_date) VALUES (:name, :birthDate)")
                .param("name", name)
                .param("birthDate", birthDate)
                .update();
    }

    @Override
    public int deleteById(int id) {
        return jdbcClient.sql("DELETE FROM birthdays WHERE id = :id")
                .param("id", id)
                .update();
    }
}
