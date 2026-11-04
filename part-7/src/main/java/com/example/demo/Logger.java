package com.example.demo;

/**
 * アプリケーションのログ出力を抽象化するインターフェース。
 */
public interface Logger {

    /**
     * ログメッセージを出力する。
     *
     * @param logMessage 出力するメッセージ
     */
    void log(String logMessage);
}
