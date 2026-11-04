# 【ハンズオン資料】Java Tipsシート

本ハンズオンの目的は「クラス設計とDI(依存性の注入)」です。本筋の理解に直接関係しないJava特有の記述やライブラリ仕様で疑問が生じた場合は、本シートを参照してください。

---

## 1. データベース接続 (`DataSource` / `HikariCP`)

```java
HikariConfig config = new HikariConfig(); // HikariCPの設定オブジェクトを作成
config.setJdbcUrl(dbUrl); // DB接続URLを設定
config.setUsername(dbUsername); // DB接続ユーザー名を設定
config.setPassword(dbPassword); // DB接続パスワードを設定
dataSource = new HikariDataSource(config); // DataSource型の変数にHikariDataSourceを代入
```

- **`DataSource`**: データベースへの接続情報（URL、ユーザー名、パスワード等）を管理し、DB接続を提供するJavaの標準インターフェース（窓口）です。
- **`HikariDataSource` (HikariCP)**: `DataSource`を実装しているコネクションプール（接続の使い回し）を管理するライブラリです。アプリ起動時にDB接続を複数確立してプールしておくことで、都度接続するオーバーヘッドを削減します。

---

## 2. SQL実行クライアント (`JdbcClient`)

SQLをシンプルかつ安全に実行するための軽量クライアントです。`.sql()`でSQL文を指定し、`.param()`でSQL文のパラメータに値をセットします。なお、パラメータのセットでは、`:id` や `:space_number` のような名前付きパラメータを使用できます。データ取得時には、`.query()`で取得するオブジェクトの型を指定でき、データ更新時には`.update()`を使用します。SQLの実行でエラーが発生した場合は、`DataAccessException`などの例外がスローされます。

### ① SELECT系（データ検索・取得）

SQLの実行結果をオブジェクトやリストとして受け取ります。

- **複数件取得（Listとして取得）**

  ```java
  List<ParkingSessionRecord> inProgressSession = jdbcClient.sql("""
          SELECT id, space_number, entry_time, exit_time, fee, status
          FROM parking_sessions
          WHERE status = 'IN_PROGRESS'
          """)
          .query(ParkingSessionRecord.class) // 指定したクラスに自動マッピング(DB上のカラム名とクラスのフィールド名でマッピング)
          .list();                           // 結果をListで取得
  ```

- **1件取得(0件の場合は `null` を返す)**

  ```java
  ParkingSessionRecord session = jdbcClient.sql("""
          SELECT ... WHERE space_number = :space_number AND status = 'IN_PROGRESS'
          """)
          .param("space_number", spaceNumber)
          .query(ParkingSessionRecord.class)
          .optional()        // 0件または1件の結果を取得（Optional型）
          .orElse(null);     // 該当データがない場合は null を返す
  ```

- **1件取得(0件の場合は例外を返す)**
  ```java
  ParkingSessionRecord session = jdbcClient.sql("""
          SELECT ... WHERE space_number = :space_number AND status = 'IN_PROGRESS'
          """)
          .param("space_number", spaceNumber)
          .query(ParkingSessionRecord.class)
          .single();       // 0件の場合は例外をスロー
  ```

### ② INSERT / UPDATE系（データ登録・更新）

データの登録や更新を行います。

- **データ登録（INSERT） / データ更新（UPDATE）**
  ```java
  jdbcClient.sql("""
          INSERT INTO parking_sessions (id, space_number, entry_time, status)
          VALUES (:id, :space_number, :entry_time, 'IN_PROGRESS')
          """)
          .param("id", transactionId)
          .param("space_number", spaceNumber)
          .param("entry_time", Timestamp.valueOf(entryTime))
          .update(); // SQL（INSERT/UPDATE/DELETE）を実行する
  ```

---

## 3. データ保持用クラス (`record`)

```java
record ParkingSessionRecord(
        UUID id,
        String spaceNumber,
        LocalDateTime entryTime,
        LocalDateTime exitTime,
        Integer fee,
        String status) {
}

// 自動で作成されるコンストラクタでインスタンスを生成
ParkingSessionRecord session = new ParkingSessionRecord(id, spaceNumber, entryTime, exitTime, fee, status); 

// レコードからプロパティを取得
UUID id = session.id(); 
```

- **`record`（レコード）**: データを保持することに特化した**不変（Immutable）クラス**を作る構文です。
- 定義するだけで、各フィールドの値を取得するメソッド（例: `session.spaceNumber()`）、コンストラクタ、オブジェクト比較用の処理（`equals()` / `hashCode()`）が自動生成されます。

---

## 4. コレクション・データ構造 (`Map` / `HashMap`)

```java
Map<String, String> env = new HashMap<>();
env.put("KEY", "VALUE"); // キーと値のペアを追加
String value = env.get("KEY"); // キーに対応する値を取得
```

- **`Map<K, V>`**: 「キー（Key）」と「値（Value）」のペアでデータを管理するインターフェースです。
- **`HashMap`**: `Map` インターフェースの代表的な実装クラスです。キーを指定して値を高速に取得・設定できます。

---

## 5. 日付や時間の扱い


```java
LocalDate today = LocalDate.now();
LocalDateTime now = LocalDateTime.now();
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

String todayText = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
String nowText = now.format(formatter);
```

- **`LocalDate`**: 日付だけを表す型です。例: `LocalDate.now()`
- **`LocalDateTime`**: 日付と時刻をまとめて表す型です。例: `LocalDateTime.now()`
- **`DateTimeFormatter`**: 文字列の表示形式を決めるためのフォーマッターです。
- 日付の表示形式は `ofPattern("yyyy-MM-dd HH:mm")` のように指定します。`yyyy` は年、`MM` は月、`dd` は日、`HH` は24時制の時間、`mm` は分です。
- `truncatedTo(ChronoUnit.MINUTES)` を使うと、秒以下を切り捨てて「分単位」に整えられます。
- 画面やログに表示する前に、必ず `format()` で文字列へ変換するのが基本です。

---

## 6. その他の便利な機能

### ① `IO.readln()`

- Java25以降で利用可能なコンソール（標準入力）からユーザーが入力した文字列を**1行取得する**入力メソッドです。

### ② `UUID` (`UUID.randomUUID()`)

- 重複しないユニークな識別子（例: `550e8400-e29b-41d4-a716-466554400000`）をランダムに自動発行する機能です。

### ③ `System.getenv()`

- **OSの環境変数**を取得するJava標準APIです。`System.getenv("ENV_VARIABLE_NAME")` のように呼び出すことで、実行環境に設定された接続情報を読み込みます。

### ④ `Files.writeString()`

- 指定したファイルパスに対して、文字列を一括で書き込む便利なファイル操作APIです。
- 第1引数に書き込むファイルパスを指定します。
- 第2引数に書き込む文字列を指定します。
- 第3引数には `StandardOpenOption.CREATE` を指定することで、ファイルが存在しない場合に新規作成されます。
- 第4引数に `StandardOpenOption.APPEND` を指定することで、既存のファイル末尾にログ文字列を追記（追記モード）しています。

### ⑤ `String.format()`

- 文字列を整形するためのメソッドです。
- 第1引数にフォーマット文字列を指定します。
- フォーマット文字列内のプレースホルダーは、`%d` や `%s` のように指定します。
- 第2引数以降に、フォーマット文字列内のプレースホルダーに対応する値を指定します。
- `%d` は整数、`%,d` は3桁区切り付きの整数表現、`%s` は文字列です。

---

## 7. その他の記述・文法

- **文字比較 (`equals()`)**: 文字列の内容を比較するためのメソッドです。`==` は参照の比較になるため、文字列の内容を比較する場合は `equals()` を使います。
- **テキストブロック (`""" ... """`)**: 複数行の文字列を改行も含めてそのまま記述できる構文です。SQL文などを可読性良く記述するために用いられます。
- **Switch式 (`case "1" -> handleEntryRegistration();`)**: 従来の `switch` 文と異なり、処理の書き忘れ（フォールスルー）が発生しない新しい構文です。
- **三項演算子 (`条件 ? A : B`)**:`if` の簡略版として使うことがある条件分岐の記法で、書き方は`条件 ? 条件が真のとき : 条件が偽のとき` です。
