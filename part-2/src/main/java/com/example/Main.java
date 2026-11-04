package com.example;

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
        BirthdayConsole birthdayConsole = new BirthdayConsole();
        birthdayConsole.run();
    }
}
