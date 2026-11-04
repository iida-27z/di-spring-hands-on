package com.example;

import javax.sql.DataSource;

/**
 * アプリケーション起動用のエントリーポイント。
 */
public class Main {

    /**
     * アプリケーションを起動し、誕生日管理コンソールを開始する。
     *
     * @param args 実行時引数（未使用）
     */
    public static void main(String[] args) {

        // ------------------------------------------------------------------------
        // 1. 依存関係の組み立て (配線)
        //    依存される部品（低レイヤー）から順にインスタンス化して依存関係は注入する
        // ------------------------------------------------------------------------
        DataSource dataSource = DatabaseConfig.createDataSource();
        JdbcBirthdayRepository jdbcBirthdayRepository = new JdbcBirthdayRepository(dataSource);
        AppLogger logger = new AppLogger();
        BirthdayConsole birthdayConsole = new BirthdayConsole(logger, jdbcBirthdayRepository);
        
        // ------------------------------------------------------------------------
        // 2. アプリケーションの実行
        //    組み立て完了後、ビジネスロジックのエントリーポイントを呼び出す
        // ------------------------------------------------------------------------
        birthdayConsole.run();
    }
}
