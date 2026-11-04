package com.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSONL 形式でログを出力する構造化ログ実装。
 */
public class JsonLogger implements Logger {

    private static final Path LOG_FILE = Paths.get("birthday_app.jsonl");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void log(String logMessage) {
        String timestamp = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        String jsonLine = String.format("{\"timestamp\":\"%s\",\"message\":\"%s\"}%n",
                timestamp,
                escape(logMessage));

        try {
            Files.writeString(
                    LOG_FILE,
                    jsonLine,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            IO.println("ログ書き込みに失敗しました: " + e.getMessage());
        }
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
