package com.example;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 誕生日情報の永続化を担当するクラス。
 */
public class JdbcBirthdayRepository {

    private static final String DB_HOST_ENV_KEY = "POSTGRES_HOST";
    private static final String DB_PORT_ENV_KEY = "POSTGRES_PORT";
    private static final String DB_NAME_ENV_KEY = "POSTGRES_DB";
    private static final String DB_USER_ENV_KEY = "APP_USER";
    private static final String DB_PASSWORD_ENV_KEY = "APP_PASSWORD";
    private final JdbcClient jdbcClient;

    /**
     * コンストラクタ。
     */
    public JdbcBirthdayRepository() {
        String dbHost = System.getenv(DB_HOST_ENV_KEY);
        String dbPort = System.getenv(DB_PORT_ENV_KEY);
        String dbName = System.getenv(DB_NAME_ENV_KEY);
        String dbUser = System.getenv(DB_USER_ENV_KEY);
        String dbPassword = System.getenv(DB_PASSWORD_ENV_KEY);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        this.jdbcClient = JdbcClient.create(new HikariDataSource(config));
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
