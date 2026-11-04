package com.example;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * データベース接続の設定を責務とするクラス。
 */
public class DatabaseConfig {

    private static final String DB_HOST_ENV_KEY = "POSTGRES_HOST";
    private static final String DB_PORT_ENV_KEY = "POSTGRES_PORT";
    private static final String DB_NAME_ENV_KEY = "POSTGRES_DB";
    private static final String DB_USER_ENV_KEY = "APP_USER";
    private static final String DB_PASSWORD_ENV_KEY = "APP_PASSWORD";

    /**
     * 環境変数からデータベース接続情報を取得し、DataSource を生成する。
     * 
     * @return DataSource
     */
    public static DataSource createDataSource() {
        String dbHost = System.getenv(DB_HOST_ENV_KEY);
        String dbPort = System.getenv(DB_PORT_ENV_KEY);
        String dbName = System.getenv(DB_NAME_ENV_KEY);
        String dbUser = System.getenv(DB_USER_ENV_KEY);
        String dbPassword = System.getenv(DB_PASSWORD_ENV_KEY);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);

        return new HikariDataSource(config);
    }
}
