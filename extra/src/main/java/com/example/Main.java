package com.example;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 駐車場の入庫・出庫・利用状況を管理するコンソールアプリケーション。
 *
 * <p>環境変数からデータベース接続情報を読み込み、ユーザーからのメニュー入力に応じて
 * 入庫登録、出庫精算、利用状況照会を実行する。</p>
 */
public class Main {

        /**
         * 駐車場の区画数と料金計算で使用する定数群。
         */
        private static final int TOTAL_SPACES = 20;
        private static final int FREE_MINUTES = 30;
        private static final int FEE_BLOCK_MINUTES = 60;
        private static final int FEE_PER_BLOCK = 100;
        private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        private static final DateTimeFormatter LOG_FILE_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        private static final String DB_HOST_ENV_KEY = "POSTGRES_HOST";
        private static final String DB_PORT_ENV_KEY = "POSTGRES_PORT";
        private static final String DB_NAME_ENV_KEY = "POSTGRES_DB";
        private static final String DB_USER_ENV_KEY = "APP_USER";
        private static final String DB_PASSWORD_ENV_KEY = "APP_PASSWORD";

        /** 環境変数から読み込んだDB接続設定。 */
        private static Map<String, String> appConfig;
        /** PostgreSQLへの接続プール。 */
        private static HikariDataSource dataSource;
        /** SQL実行用クライアント。 */
        private static JdbcClient jdbcClient;

        /**
         * アプリケーションの起動処理を行う。
         *
         * <p>DB接続の初期化、メインメニューの表示、例外発生時のエラー出力を行う。</p>
         *
         * @param args 起動引数。現行実装では未使用。
         */
        public static void main(String[] args) {
                try {
                        // 起動ログ
                        logInfo("アプリケーションを起動しました");
                        logInfo("アプリケーションの初期化処理を開始します");

                        // 環境変数からDB接続情報を取得
                        appConfig = loadEnvironment();

                        // 接続情報(DataSource)の設定
                        String dbUrl = "jdbc:postgresql://"
                                        + appConfig.get(DB_HOST_ENV_KEY)
                                        + ":"
                                        + appConfig.get(DB_PORT_ENV_KEY)
                                        + "/"
                                        + appConfig.get(DB_NAME_ENV_KEY);
                        HikariConfig config = new HikariConfig();
                        config.setJdbcUrl(dbUrl);
                        config.setUsername(appConfig.get(DB_USER_ENV_KEY));
                        config.setPassword(appConfig.get(DB_PASSWORD_ENV_KEY));
                        dataSource = new HikariDataSource(config);

                        // SQLクライアント(JdbcClient)の初期化
                        jdbcClient = JdbcClient.create(dataSource);

                        // 初期化完了ログ
                        logInfo("DB接続を確立しました (url=" + dbUrl + ")");
                        logInfo("アプリケーションの初期化処理が完了しました");

                        // コンソールアプリケーションの実行
                        runConsole();

                        // 終了ログ
                        logInfo("アプリケーションを終了します");
                } catch (ConfigurationException e) {
                        logError("アプリケーションの起動に必要な設定が不足しています: " + e.logMessage, e);
                        System.out.println();
                        System.out.println("[エラー] " + e.userMessage);
                        System.out.println("==========================================");
                } catch (Exception e) {
                        logError("アプリケーションの起動または実行に失敗しました", e);
                        System.out.println();
                        System.out.println("[エラー] 予期せぬエラーが発生しました");
                        System.out.println("==========================================");
                } finally {
                        if (dataSource != null) {
                                dataSource.close();
                        }
                }
        }

        /**
         * メニュー画面を表示し、ユーザーの選択に応じて処理を振り分ける。
         */
        private static void runConsole() {
                boolean running = true;
                while (running) {
                        // メニューを表示
                        printMenu();

                        // ユーザーからのメニュー入力を取得
                        String menu = IO.readln();
                        if (menu == null) {
                                return;
                        }

                        // メニュー選択に応じた処理を実行
                        switch (menu.trim()) {
                                case "1" -> handleEntryRegistration();
                                case "2" -> handleExitSettlement();
                                case "3" -> handleOccupancyInquiry();
                                case "0" -> running = false;
                                default -> {
                                        logWarn("メニュー入力が不正です input=" + menu);
                                        System.out.println();
                                        System.out.println("[エラー] メニュー番号は0〜3で指定してください");
                                        System.out.println("==========================================");
                                }
                        }
                }
        }

        /**
         * 入庫登録処理を実行する。
         *
         * <p>駐車位置の入力を受け取り、妥当性検証と重複チェックを行ったうえで
         * 新しい入庫セッションをデータベースへ登録する。</p>
         */
        private static void handleEntryRegistration() {
                // 駐車位置の入力を促し、入力された値を検証して不正な場合はエラーを表示する
                System.out.println();
                System.out.println("--- 入庫登録 ---");
                System.out.print("駐車位置を入力してください (01-20) > ");
                String input = IO.readln();
                String spaceNumber;
                try {
                        spaceNumber = normalizeSpaceNumber(input);
                } catch (InvalidInputException e) {
                        logWarn(e.logMessage);
                        printBusinessError(e.userMessage);
                        return;
                }

                
                // 入庫登録処理
                logInfo("入庫登録を開始します space=" + spaceNumber);
                try {
                        // 駐車位置に既に入庫中のセッションがあるか確認
                        ParkingSessionRecord existing = findInProgressBySpaceNumber(spaceNumber);
                        if (existing != null) {
                                logWarn("入庫登録に失敗しました（駐車位置入庫中） space=" + spaceNumber + " existingTxId=" + existing.id);
                                printBusinessError("この駐車位置は既に入庫中です（駐車位置: " + spaceNumber + "）");
                                return;
                        }

                        // 取引IDを発行し、入庫時間を取得
                        UUID transactionId = UUID.randomUUID();
                        LocalDateTime entryTime = LocalDateTime.now();

                        // 入庫情報をDBに登録
                        jdbcClient.sql("""
                                        INSERT INTO parking_sessions (id, space_number, entry_time, exit_time, fee, status)
                                        VALUES (:id, :space_number, :entry_time, NULL, NULL, 'IN_PROGRESS')
                                        """)
                                        .param("id", transactionId)
                                        .param("space_number", spaceNumber)
                                        .param("entry_time", entryTime)
                                        .update();

                        // 入庫登録完了ログ
                        logInfo("入庫登録が完了しました txId="
                                        + transactionId
                                        + " space="
                                        + spaceNumber
                                        + " entryTime="
                                        + entryTime.truncatedTo(ChronoUnit.MINUTES).format(LOG_TIMESTAMP));

                        // 入庫完了メッセージを表示
                        System.out.println();
                        System.out.println("[登録完了]");
                        System.out.println("取引ID   : " + transactionId);
                        System.out.println("駐車位置 : " + spaceNumber);
                        System.out.println("入庫時刻 : " + formatDateTime(entryTime));
                        System.out.println("==========================================");
                } catch (DataAccessException e) {
                        logError("DBアクセスに失敗しました operation=entry space=" + spaceNumber, e);
                        printUnexpectedError();
                } catch (Exception e) {
                        logError("入庫登録で予期せぬエラーが発生しました space=" + spaceNumber, e);
                        printUnexpectedError();
                }
        }

        /**
         * 出庫精算処理を実行する。
         *
         * <p>指定駐車位置の進行中セッションを取得し、利用時間から料金を計算して
         * 出庫時刻と料金を登録する。</p>
         */
        private static void handleExitSettlement() {
                // 駐車位置の入力を促し、入力された値を検証して不正な場合はエラーを表示する
                System.out.println();
                System.out.println("--- 出庫精算 ---");
                System.out.print("駐車位置を入力してください (01-20) > ");
                String input = IO.readln();
                String spaceNumber;
                try {
                        spaceNumber = normalizeSpaceNumber(input);
                } catch (InvalidInputException e) {
                        logWarn(e.logMessage);
                        printBusinessError(e.userMessage);
                        return;
                }

                // 出庫精算処理
                logInfo("出庫精算を開始します space=" + spaceNumber);
                try {
                        // 駐車位置に入庫中のセッションがあるか確認
                        ParkingSessionRecord session = findInProgressBySpaceNumber(spaceNumber);
                        if (session == null) {
                                logWarn("出庫精算に失敗しました（入庫記録なし） space=" + spaceNumber);
                                printBusinessError("入庫記録が見つかりません（駐車位置: " + spaceNumber + "）");
                                return;
                        }

                        // 出庫時刻を取得し、料金を計算
                        LocalDateTime exitTime = LocalDateTime.now();
                        FeeBreakdown fee = calculateFee(session.entryTime, exitTime);

                        // 出庫情報をDBに更新
                        jdbcClient.sql("""
                                        UPDATE parking_sessions
                                           SET exit_time = :exit_time,
                                               fee = :fee,
                                               status = 'SETTLED'
                                        WHERE id = :id
                                        """)
                                        .param("exit_time", exitTime)
                                        .param("fee", fee.totalFee)
                                        .param("id", session.id)
                                        .update();

                        // 出庫精算完了ログ
                        logInfo("出庫精算が完了しました txId="
                                        + session.id
                                        + " space="
                                        + spaceNumber
                                        + " entry="
                                        + session.entryTime.truncatedTo(ChronoUnit.MINUTES).format(LOG_TIMESTAMP)
                                        + " exit="
                                        + exitTime.truncatedTo(ChronoUnit.MINUTES).format(LOG_TIMESTAMP)
                                        + " durationMinutes="
                                        + fee.totalMinutes
                                        + " fee="
                                        + fee.totalFee);

                        // 出庫完了メッセージを表示
                        System.out.println();
                        System.out.println("[精算完了]");
                        System.out.println("取引ID   : " + session.id);
                        System.out.println("駐車位置 : " + spaceNumber);
                        System.out.println("入庫時刻 : " + formatDateTime(session.entryTime));
                        System.out.println("出庫時刻 : " + formatDateTime(exitTime));
                        System.out.println("駐車時間 : " + formatDuration(fee.totalMinutes));
                        System.out.println("------------------------------------------");
                        System.out.println("[料金内訳]");
                        System.out.println("無料時間 30分 / 課金対象時間 " + formatBillableDuration(fee.billableMinutes)
                                        + "(" + fee.billableBlocks + "コマ)");
                        System.out.println("100円 × " + fee.billableBlocks + "コマ");
                        System.out.println("------------------------------------------");
                        System.out.println("合計金額 : " + formatMoney(fee.totalFee) + "円");
                        System.out.println("==========================================");
                } catch (DataAccessException e) {
                        logError("DBアクセスに失敗しました operation=settle space=" + spaceNumber, e);
                        printUnexpectedError();
                } catch (Exception e) {
                        logError("出庫精算で予期せぬエラーが発生しました space=" + spaceNumber, e);
                        printUnexpectedError();
                }
        }

        /**
         * 現在の駐車場利用状況を一覧表示する。
         *
         * <p>入庫中のセッションを取得し、各区画の空き状況と経過時間、現時点料金を
         * コンソールに表示する。</p>
         */
        private static void handleOccupancyInquiry() {
                // 画面表示用の処理
                System.out.println();
                System.out.println("--- 駐車場利用状況 ---");

                // 現在時刻を取得
                LocalDateTime now = LocalDateTime.now();

                // 利用状況照会処理
                try {
                        // 入庫中のセッションを取得
                        List<ParkingSessionRecord> inProgressSession = jdbcClient.sql("""
                                        SELECT id, space_number, entry_time, exit_time, fee, status
                                        FROM parking_sessions
                                        WHERE status = 'IN_PROGRESS'
                                        ORDER BY space_number
                                        """)
                                        .query(ParkingSessionRecord.class)
                                        .list();

                        // セッションを駐車位置ごとにマッピング
                        Map<String, ParkingSessionRecord> sessionMap = new HashMap<>();
                        for (ParkingSessionRecord session : inProgressSession) {
                                sessionMap.put(session.spaceNumber, session);
                        }

                        // 満空状況を計算
                        int occupiedCount = inProgressSession.size();
                        int vacantCount = TOTAL_SPACES - occupiedCount;

                        // 利用状況照会ログ
                        logInfo("利用状況を照会しました occupied="
                                        + occupiedCount
                                        + " vacant="
                                        + vacantCount
                                        + " total="
                                        + TOTAL_SPACES);

                        // 画面に現在の利用状況を表示
                        System.out.println("現在時刻 : " + formatDateTime(now));
                        System.out.println("満空状況 : 入庫中 " + occupiedCount + " / 全 " + TOTAL_SPACES + " 区画（空き "
                                        + vacantCount + "）");
                        System.out.println("------------------------------------------------------------");
                        System.out.println("位置  状態    入庫時刻            経過時間    現時点料金");

                        for (int i = 1; i <= TOTAL_SPACES; i++) {
                                String spaceNumber = String.format("%02d", i);
                                ParkingSessionRecord session = sessionMap.get(spaceNumber);
                                if (session == null) {
                                        System.out.printf("%-4s  %-4s  %-19s  %-10s  %s%n",
                                                        spaceNumber,
                                                        "空き",
                                                        "-",
                                                        "-",
                                                        "-");
                                } else {
                                        FeeBreakdown fee = calculateFee(session.entryTime, now);
                                        System.out.printf("%-4s  %-4s  %-19s  %-10s  %s円%n",
                                                        spaceNumber,
                                                        "入庫中",
                                                        formatDateTime(session.entryTime),
                                                        formatDuration(fee.totalMinutes),
                                                        formatMoney(fee.totalFee));
                                }
                        }

                        System.out.println("------------------------------------------------------------");
                        System.out.println("==========================================");
                } catch (DataAccessException e) {
                        logError("DBアクセスに失敗しました operation=inquiry", e);
                        printUnexpectedError();
                } catch (Exception e) {
                        logError("利用状況照会で予期せぬエラーが発生しました", e);
                        printUnexpectedError();
                }
        }

        /**
         * 入力値を駐車位置として正規化する。
         *
         * <p>1桁の入力は2桁に変換し、01〜20の範囲に収まらない場合は業務例外を送出する。</p>
         *
         * @param rawInput ユーザーから入力された駐車位置文字列
         * @return 2桁固定長の駐車位置文字列
         * @throws InvalidInputException 形式不正または範囲外の場合
         */
        static String normalizeSpaceNumber(String rawInput) throws InvalidInputException {
                String value = rawInput == null ? "" : rawInput;
                if (!value.matches("\\d{1,2}")) {
                        throw new InvalidInputException(
                                        "駐車位置は01〜20で指定してください（駐車位置: " + value + "）",
                                        "入力値が不正です（駐車位置の形式不正） input=" + value);
                }

                int numericValue = Integer.parseInt(value);
                if (numericValue < 1 || numericValue > TOTAL_SPACES) {
                        throw new InvalidInputException(
                                        "駐車位置は01〜20で指定してください（駐車位置: " + value + "）",
                                        "入力値が不正です（駐車位置の範囲外） input=" + value);
                }

                return String.format("%02d", numericValue);
        }

        /**
         * 入庫時刻から出庫時刻までの料金を計算する。
         *
         * <p>秒は切り捨て、30分までは無料、超過分は60分単位で切り上げて料金化する。</p>
         *
         * @param entryTime 入庫時刻
         * @param exitTime 出庫時刻
         * @return 利用時間と費用の内訳を保持する計算結果
         */
        static FeeBreakdown calculateFee(LocalDateTime entryTime, LocalDateTime exitTime) {
                // 入庫・出庫時刻を分単位に切り捨て
                LocalDateTime truncatedEntry = entryTime.truncatedTo(ChronoUnit.MINUTES);
                LocalDateTime truncatedExit = exitTime.truncatedTo(ChronoUnit.MINUTES);

                // 利用時間の計算（分単位）
                long totalMinutes = ChronoUnit.MINUTES.between(truncatedEntry, truncatedExit);
                if (totalMinutes < 0) {
                        totalMinutes = 0;
                }

                // 料金の計算
                long billableMinutes = Math.max(0, totalMinutes - FREE_MINUTES);
                int billableBlocks = billableMinutes == 0
                                ? 0
                                : (int) Math.ceil((double) billableMinutes / FEE_BLOCK_MINUTES);
                int totalFee = billableBlocks * FEE_PER_BLOCK;

                // 料金の内訳を返す
                return new FeeBreakdown(totalMinutes, billableMinutes, billableBlocks, totalFee);
        }

        /**
         * 分単位の利用時間を「X時間Y分」の形式に変換する。
         *
         * @param totalMinutes 総利用時間（分）
         * @return 画面表示用の時間文字列
         */
        static String formatDuration(long totalMinutes) {
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;
                return hours + "時間" + minutes + "分";
        }

        /**
         * 課金対象時間を表示形式で返す。
         *
         * @param totalMinutes 課金対象時間（分）
         * @return 例: 27時間5分
         */
        static String formatBillableDuration(long totalMinutes) {
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;
                String minuteText = minutes == 0 ? "00" : String.valueOf(minutes);
                return hours + "時間" + minuteText + "分";
        }

        /**
         * LocalDateTime を表示用の形式に整形する。
         *
         * @param dateTime 整形対象の日時
         * @return 画面表示用の日時文字列
         */
        private static String formatDateTime(LocalDateTime dateTime) {
                return dateTime.truncatedTo(ChronoUnit.MINUTES).format(DISPLAY_DATE_TIME);
        }

        /**
         * 金額を三桁区切り付きの文字列へ変換する。
         *
         * @param amount 金額
         * @return 例: 2,800
         */
        private static String formatMoney(int amount) {
                return String.format("%,d", amount);
        }

        /**
         * 指定駐車位置の進行中セッションを取得する。
         *
         * @param spaceNumber 2桁固定長の駐車位置
         * @return 該当セッション。存在しない場合は null
         */
        private static ParkingSessionRecord findInProgressBySpaceNumber(String spaceNumber) {
                // 指定された駐車スペースで進行中のセッションを検索
                return jdbcClient.sql("""
                                SELECT id, space_number, entry_time, exit_time, fee, status
                                FROM parking_sessions
                                WHERE 1=1
                                  AND space_number = :space_number
                                  AND status = 'IN_PROGRESS'
                                """)
                                .param("space_number", spaceNumber)
                                .query(ParkingSessionRecord.class)
                                .optional()
                                .orElse(null);
        }

        /**
         * 環境変数からデータベース接続に必要な設定を読み込む。
         *
         * @return DB接続情報のマップ
         * @throws ConfigurationException 必須環境変数が未設定の場合
         */
        private static Map<String, String> loadEnvironment() {
                // 環境変数からDB接続情報を取得
                Map<String, String> env = new HashMap<>();
                env.put(DB_HOST_ENV_KEY, getRequiredEnv(DB_HOST_ENV_KEY));
                env.put(DB_PORT_ENV_KEY, getRequiredEnv(DB_PORT_ENV_KEY));
                env.put(DB_NAME_ENV_KEY, getRequiredEnv(DB_NAME_ENV_KEY));
                env.put(DB_USER_ENV_KEY, getRequiredEnv(DB_USER_ENV_KEY));
                env.put(DB_PASSWORD_ENV_KEY, getRequiredEnv(DB_PASSWORD_ENV_KEY));
                return env;
        }

        /**
         * 必須環境変数の値を取得する。
         *
         * @param name 環境変数名
         * @return 環境変数の値
         * @throws ConfigurationException 設定が欠落している場合
         */
        private static String getRequiredEnv(String name) {
                String value = System.getenv(name);
                if (value == null || value.isBlank()) {
                        throw new ConfigurationException(
                                        "DB接続に必要な情報が取得できませんでした",
                                        name + " environment variable is not set");
                }
                return value;
        }

        /**
         * メインメニューをコンソールに表示する。
         */
        private static void printMenu() {
                System.out.println("==========================================");
                System.out.println("  駐車場管理システム v1.0");
                System.out.println("==========================================");
                System.out.println("1. 入庫登録");
                System.out.println("2. 出庫精算");
                System.out.println("3. 利用状況照会");
                System.out.println("0. 終了");
                System.out.println("------------------------------------------");
                System.out.print("番号を選択してください > ");
        }

        /**
         * 業務エラーを画面に表示する。
         *
         * @param message 表示するメッセージ
         */
        private static void printBusinessError(String message) {
                System.out.println();
                System.out.println("[エラー] " + message);
                System.out.println("==========================================");
        }

        /**
         * 予期せぬシステム障害時の汎用エラーを表示する。
         */
        private static void printUnexpectedError() {
                System.out.println();
                System.out.println("[エラー] 予期せぬエラーが発生しました");
                System.out.println("==========================================");
        }

        /**
         * INFO ログを出力する。
         *
         * @param message ログ本文
         */
        private static void logInfo(String message) {
                logging("INFO", message);
        }

        /**
         * WARN ログを出力する。
         *
         * @param message ログ本文
         */
        private static void logWarn(String message) {
                logging("WARN", message);
        }

        /**
         * ERROR ログを出力する。
         *
         * @param message ログ本文
         * @param e 例外情報
         */
        private static void logError(String message, Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                logging("ERROR", message + "\n" + sw);
        }

        /**
         * 指定したログレベルでアプリケーションログを出力する。
         *
         * @param level INFO / WARN / ERROR
         * @param message ログ本文
         */
        private static void logging(String level, String message) {
                Path path = Path.of("app-" + LocalDate.now().format(LOG_FILE_DATE) + ".log");
                String timestamp = LocalDateTime.now().format(LOG_TIMESTAMP);
                String logMessage = timestamp + " [" + String.format("%-5s", level) + "]  " + message + "\n";

                try {
                        Files.writeString(
                                        path,
                                        logMessage,
                                        StandardOpenOption.CREATE,
                                        StandardOpenOption.APPEND);
                } catch (IOException e) {
                        System.err.println("【警告】ログの書き込みに失敗しています。");
                }
        }

        /**
         * アプリケーション起動時に必要な設定が不足した場合に送出される例外。
         */
        static final class ConfigurationException extends RuntimeException {
                /** ユーザー向けに表示するメッセージ。 */
                private final String userMessage;
                /** ログ記録用に利用するメッセージ。 */
                private final String logMessage;

                /**
                 * 例外を生成する。
                 *
                 * @param userMessage ユーザー向けメッセージ
                 * @param logMessage ログ出力用メッセージ
                 */
                private ConfigurationException(String userMessage, String logMessage) {
                        super(userMessage);
                        this.userMessage = userMessage;
                        this.logMessage = logMessage;
                }
        }

        /**
         * 入力値が駐車位置として不正である場合に送出される業務例外。
         */
        static final class InvalidInputException extends Exception {
                /** ユーザー向けメッセージ。 */
                private final String userMessage;
                /** ログ出力用メッセージ。 */
                private final String logMessage;

                /**
                 * 例外を生成する。
                 *
                 * @param userMessage ユーザー向けメッセージ
                 * @param logMessage ログ出力用メッセージ
                 */
                private InvalidInputException(String userMessage, String logMessage) {
                        super(userMessage);
                        this.userMessage = userMessage;
                        this.logMessage = logMessage;
                }
        }

        /**
         * 料金計算の結果を保持する値オブジェクト。
         *
         * @param totalMinutes 総利用時間（分）
         * @param billableMinutes 課金対象時間（分）
         * @param billableBlocks 60分単位で切り上げた課金ブロック数
         * @param totalFee 合計料金
         */
        record FeeBreakdown(long totalMinutes, long billableMinutes, int billableBlocks, int totalFee) {
        }

        /**
         * 駐車セッションのデータを保持するレコード。
         *
         * @param id 取引ID
         * @param spaceNumber 駐車位置（01〜20）
         * @param entryTime 入庫時刻
         * @param exitTime 出庫時刻。未出庫時は null
         * @param fee 精算済み料金。未精算時は null
         * @param status セッション状態
         */
        record ParkingSessionRecord(
                        UUID id,
                        String spaceNumber,
                        LocalDateTime entryTime,
                        LocalDateTime exitTime,
                        Integer fee,
                        String status) {
        }
}
