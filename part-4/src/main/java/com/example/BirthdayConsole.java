package com.example;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * コンソール画面と処理の流れの制御を担当するクラス。
 */
public class BirthdayConsole {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final Logger logger;
    private final BirthdayRepository birthdayRepository;

    /**
     * コンストラクタ。
     * 
     * @param logger ログ出力用のインスタンス
     * @param birthdayRepository 誕生日情報の永続化を担当するリポジトリ
     */
    public BirthdayConsole(Logger logger, BirthdayRepository birthdayRepository) {
        this.logger = logger;
        this.birthdayRepository = birthdayRepository;
    }

    /**
     * アプリケーションのメインループを開始し、ユーザー入力に応じて処理を分岐する。
     */
    public void run() {
        try {
            logger.log("システムが起動しました");

            boolean running = true;
            while (running) {
                IO.println("\n=== お誕生日登録メモ ===");
                IO.println("1: 一覧表示");
                IO.println("2: 情報登録");
                IO.println("3: 情報削除");
                IO.println("0: 終了");

                String choice = IO.readln("操作番号を選択してください > ");
                if (choice == null) {
                    choice = "";
                }

                switch (choice) {
                    case "1" -> listBirthdays();
                    case "2" -> addBirthday();
                    case "3" -> deleteBirthday();
                    case "0" -> {
                        IO.println("アプリケーションを終了します。");
                        logger.log("システムを終了しました");
                        running = false;
                    }
                    default -> {
                        IO.println("無効な選択です。0〜3を入力してください。");
                        logger.log("無効な選択: " + choice);
                    }
                }
            }
        } catch (Exception e) {
            IO.println("予期せぬエラーが発生しました: " + e.getMessage());
            logger.log("エラー: " + e.getMessage());
        }
    }

    /**
     * 登録されている誕生日情報を一覧表示する。
     */
    private void listBirthdays() {
        IO.println("\n--- 登録一覧 ---");
        logger.log("一覧表示処理を開始");

        List<Person> rows = birthdayRepository.findAll();

        if (rows.isEmpty()) {
            IO.println("データが登録されていません。");
            logger.log("一覧表示件数: 0件");
            return;
        }

        for (Person row : rows) {
            IO.println(String.format("ID: %-3s | 名前: %-10s | 誕生日: %-10s",
                    row.id(), row.name(), row.birthDate().format(DATE_FORMATTER)));
        }

        logger.log("一覧表示件数: " + rows.size() + "件");
    }

    /**
     * ユーザー入力から名前と誕生日を受け取り、新規データを保存する。
     */
    private void addBirthday() {
        IO.println("\n--- 新規登録 ---");
        logger.log("新規登録処理を開始");

        String name = IO.readln("名前を入力 > ");
        String inputBirthDate = IO.readln("誕生日を入力 (yyyy-MM-dd形式) > ");

        if (name == null) {
            name = "";
        }
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(inputBirthDate, DATE_FORMATTER);
        } catch (Exception e) {
            IO.println("誕生日の形式が正しくありません。yyyy-MM-dd の形式で入力してください。");
            logger.log("誕生日の形式が不正: " + inputBirthDate);
            return;
        }

        int insertedCount = birthdayRepository.insert(name, birthDate);
        if (insertedCount > 0) {
            IO.println("登録が完了しました。");
            logger.log(String.format("新規登録: 名前=%s, 誕生日=%s を登録", name, inputBirthDate));
            return;
        }

        IO.println("登録に失敗しました。");
        logger.log("新規登録: 登録に失敗");
    }

    /**
     * 指定した ID の誕生日情報を削除する。
     */
    private void deleteBirthday() {
        IO.println("\n--- 登録削除 ---");
        logger.log("削除処理を開始");

        String inputId = IO.readln("削除する対象のIDを入力 > ");

        int id;
        try {
            id = Integer.parseInt(inputId);
        } catch (NumberFormatException e) {
            IO.println("有効な数値を入力してください。");
            logger.log("削除対象IDの形式が不正: " + inputId);
            return;
        }

        int deletedCount = birthdayRepository.deleteById(id);

        if (deletedCount > 0) {
            IO.println("削除が完了しました。");
            logger.log(String.format("登録削除: ID=%d を削除", id));
        } else {
            IO.println("指定されたIDは見つかりませんでした。");
            logger.log(String.format("登録削除: ID=%d は存在せず削除できず", id));
        }
    }
}
