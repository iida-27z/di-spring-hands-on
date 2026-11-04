package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 誕生日情報を管理するコンソールアプリケーション。
 * 環境変数からデータベース接続情報を取得し、登録済みの誕生日を一覧表示・追加・削除する。
 */
public class Main {

    /**
     * PostgreSQLのホスト名を取得する環境変数名。
     */
    private static final String DB_HOST_ENV_KEY = "POSTGRES_HOST";
    /**
     * PostgreSQLのポート番号を取得する環境変数名。
     */
    private static final String DB_PORT_ENV_KEY = "POSTGRES_PORT";
    /**
     * PostgreSQLのデータベース名を取得する環境変数名。
     */
    private static final String DB_NAME_ENV_KEY = "POSTGRES_DB";
    /**
     * アプリケーション接続ユーザー名を取得する環境変数名。
     */
    private static final String DB_USER_ENV_KEY = "APP_USER";
    /**
     * アプリケーション接続パスワードを取得する環境変数名。
     */
    private static final String DB_PASSWORD_ENV_KEY = "APP_PASSWORD";
    /**
     * データベース操作を行う SQL クライアント。
     */
    private static JdbcClient jdbcClient;

    /**
     * アプリケーションのログファイルの保存先。
     */
    private static final Path LOG_FILE = Paths.get("birthday_app.log");
    /**
     * 日付の表示・解析に使用するフォーマッター。
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * ログのタイムスタンプ生成に使用するフォーマッター。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * アプリケーションを起動し、メニュー選択に応じて誕生日情報を一覧表示・登録・削除する。
     *
     * @param args 実行時引数（未使用）
     */
    public static void main(String[] args) {

        try {
            // 環境変数からDB接続情報を取得
            String dbHost = System.getenv(DB_HOST_ENV_KEY);
            String dbPort = System.getenv(DB_PORT_ENV_KEY);
            String dbName = System.getenv(DB_NAME_ENV_KEY);
            String dbUser = System.getenv(DB_USER_ENV_KEY);
            String dbPassword = System.getenv(DB_PASSWORD_ENV_KEY);
            String dbUrl = "jdbc:postgresql://" + dbHost + ":" + dbPort + "/" + dbName;

            // 接続情報(DataSource)の設定
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            DataSource dataSource = new HikariDataSource(config);

            // SQLクライアント(JdbcClient)の初期化
            jdbcClient = JdbcClient.create(dataSource);

            // アプリケーション起動ログを出力
            writeLog("システムが起動しました");

            // ループ処理
            boolean running = true;
            while (running) {
                // メニューを表示
                IO.println("\n=== お誕生日登録メモ ===");
                IO.println("1: 一覧表示");
                IO.println("2: 情報登録");
                IO.println("3: 情報削除");
                IO.println("0: 終了");

                // ユーザーの入力を受け付ける
                String choice = IO.readln("操作番号を選択してください > ");
                if (choice == null) {
                    choice = ""; // nullの場合は空文字に置き換える
                }

                // 入力に応じて処理を分岐
                switch (choice) {
                    case "1" -> listBirthdays();
                    case "2" -> addBirthday();
                    case "3" -> deleteBirthday();
                    case "0" -> {
                        IO.println("アプリケーションを終了します。");
                        writeLog("システムを終了しました");
                        running = false;
                    }
                    default -> {
                        IO.println("無効な選択です。0〜3を入力してください。");
                        writeLog("無効な選択: " + choice);
                    }
                }
            }
        } catch (Exception e) {
            IO.println("予期せぬエラーが発生しました: " + e.getMessage());
            writeLog("エラー: " + e.getMessage());
        }
    }

    /**
     * 登録済みの誕生日情報をデータベースから取得し、一覧として表示する。
     */
    private static void listBirthdays() {
        // 画面に見出しを表示
        IO.println("\n--- 登録一覧 ---");

        // 処理開始ログを出力
        writeLog("一覧表示処理を開始");

        // データベースから登録情報を取得
        List<Person> rows = jdbcClient.sql("SELECT id, name, birth_date FROM birthdays ORDER BY id")
                .query(Person.class)
                .list();

        // 登録情報が存在しない場合
        if (rows.isEmpty()) {
            IO.println("データが登録されていません。");
            writeLog("一覧表示件数: 0件");
            return;
        }

        // 登録情報の表示
        for (var row : rows) {
            IO.println(String.format("ID: %-3s | 名前: %-10s | 誕生日: %-10s",
                    row.id(), row.name(), row.birthDate().format(DATE_FORMATTER)));
        }

        // ログに登録件数を出力
        writeLog("一覧表示件数: " + rows.size() + "件");
    }

    /**
     * ユーザー入力から新しい誕生日情報を登録する。
     */
    private static void addBirthday() {
        // 画面に見出しを表示
        IO.println("\n--- 新規登録 ---");

        // 処理開始ログを出力
        writeLog("新規登録処理を開始");

        // ユーザーが名前と誕生日を入力
        String name = IO.readln("名前を入力 > ");
        String inputBirthDate = IO.readln("誕生日を入力 (yyyy-MM-dd形式) > ");

        // 入力の検証
        if(name == null){
            name = "";
        }
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(inputBirthDate, DATE_FORMATTER);
        } catch (Exception e) {
            IO.println("誕生日の形式が正しくありません。yyyy-MM-dd の形式で入力してください。");
            writeLog("誕生日の形式が不正: " + inputBirthDate);
            return;
        }

        // データベースに情報を登録
        int updated = jdbcClient.sql("INSERT INTO birthdays (name, birth_date) VALUES (:name, :birthDate)")
                .param("name", name)
                .param("birthDate", birthDate)
                .update();

        // 登録結果の表示
        if (updated > 0) {
            IO.println("登録が完了しました。");
            writeLog(String.format("新規登録: 名前=%s, 誕生日=%s を登録", name, inputBirthDate));
        }else{
            IO.println("登録に失敗しました。");
            writeLog("新規登録: 登録に失敗");
        }
    }

    /**
     * 指定した ID の誕生日情報を削除する。
     */
    private static void deleteBirthday() {
        // 画面に見出しを表示
        IO.println("\n--- 登録削除 ---");

        // 処理開始ログを出力
        writeLog("削除処理を開始");

        // ユーザーに削除対象のIDを入力させる
        String inputId = IO.readln("削除する対象のIDを入力 > ");

        // ID入力の検証
        int id;
        try {
            id = Integer.parseInt(inputId);
        } catch (NumberFormatException e) {
            IO.println("有効な数値を入力してください。");
            writeLog("削除対象IDの形式が不正: " + inputId);
            return;
        }

        // データベースから情報を削除
        int rowsAffected = jdbcClient.sql("DELETE FROM birthdays WHERE id = :id")
                .param("id", id)
                .update();

        // 削除結果の表示
        if (rowsAffected > 0) {
            IO.println("削除が完了しました。");
            writeLog(String.format("登録削除: ID=%d を削除", id));
        } else {
            IO.println("指定されたIDは見つかりませんでした。");
            writeLog(String.format("登録削除: ID=%d は存在せず削除できず", id));
        }
    }

    /**
     * アプリケーションの処理内容をログファイルへ記録する。
     *
     * @param logMessage ログに記録するメッセージ
     */
    private static void writeLog(String logMessage) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String logEntry = String.format("[%s] %s%n", timestamp, logMessage);

        try {
            Files.writeString(
                    LOG_FILE,
                    logEntry,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            IO.println("ログ書き込みに失敗しました: " + e.getMessage());
        }
    }
}
