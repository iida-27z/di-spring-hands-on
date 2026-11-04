package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 標準テキスト形式のログ書き込みを担当するクラス。
 */
public class AppLogger implements Logger {

    private static final Path LOG_FILE = Paths.get("birthday_app.log");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 指定したメッセージをタイムスタンプ付きでログファイルに追記する。
     *
     * @param logMessage 追記するログメッセージ
     */
    @Override
    public void log(String logMessage) {
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
