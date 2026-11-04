# 【ハンズオン資料】Maven Tipsシート

本ハンズオンの目的は「クラス設計とDI(依存性の注入)」です。本筋の理解に直接関係しないMavenの基本や設定で疑問が生じた場合は、本シートを参照してください。

---

## 1. Mavenとは

Mavenは、Javaプロジェクトの「ビルド」「依存ライブラリ管理」「実行環境の整理」を自動でおこなうツールです。

- プロジェクトの設定を `pom.xml` に記述できる
- 外部ライブラリを `pom.xml` に追加できる
- `compile` / `test` / `package` などの作業を自動化できる

Mavenを使えば、必要なライブラリの自動ダウンロードから実行時の読み込み設定まで一括で行われます。そのため、外部ファイルの手動配置や複雑なビルド設定の手間を省けます。

---

## 2. `pom.xml` とは

Mavenの中心になる設定ファイルが `pom.xml` です。これは「Project Object Model」の略で、プロジェクト名・依存ライブラリ・Javaバージョン・ビルド方法などを定義します。

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>demo</artifactId>
    <version>1.0-SNAPSHOT</version>

    <properties>
        <maven.compiler.release>25</maven.compiler.release>
    </properties>
</project>
```

- **`groupId`**: 会社や団体のような管理名
- **`artifactId`**: プロジェクト名（アプリ名）
- **`version`**: バージョン
- **`properties`**: 共通設定の定義（Javaのバージョンなど）

pom.xmlは、XML形式のテキストファイルであるため、Gitなどのバージョン管理システムで差分を追いやすく、チームでの共同開発にも適しています。

---

## 3. 依存関係の追加

Mavenでは、外部ライブラリを `dependency` として`pom.xml` に追加します。

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-jdbc</artifactId>
        <version>7.0.9</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>6.1.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

- `groupId`: どの組織が作ったライブラリか
- `artifactId`: ライブラリ名
- `version`: ライブラリのバージョン

このように書いておくと、Mavenが必要なJARファイルを自動的にダウンロードしてくれます。
また、依存関係には「どの範囲で利用するか」を表す `scope` という概念があります。

- **`compile`**: 本番コードでも使える(特に指定がない場合はデフォルトで `compile`)
- **`test`**: テスト時のみ使える
- **`runtime`**: 実行時に必要だが、コンパイル時には不要

依存関係の範囲を `scope` で明示することで、コンパイルや実行時に必要なライブラリを適切に管理できます。

---

## 4. Mavenのライフサイクル

Mavenには、開発やビルドの中でよく使う処理が決まった流れで用意されています。これを「ライフサイクル」と呼びます。

### よく使うコマンド
- 過去のビルドで生成された`target/`を削除します。
    ```
    mvn clean
    ```
- Javaソースをコンパイルして、`target/classes` に出力します。
    ```bash
    mvn compile
    ```

- テストを実行します。
    ```bash
    mvn test
    ```

- JARファイルなどのアプリの成果物をまとめて作成します。
    ```bash
    mvn package
    ```

この流れで、開発中の確認から成果物の作成までを統一的に扱えます。

---

## 5. プロジェクト構成のイメージ

典型的なJavaプロジェクトでは、次のような構成になります。

```text
project/
├── pom.xml
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/example/Main.java
│   └── test/
│       └── java/
│           └── com/example/MainTest.java
└── target/
```

- **`src/main/java`**: 本番コード
- **`src/test/java`**: テストコード
- **`target`**: ビルド結果が入る出力先

この構成に揃っていると、Mavenがどこをコンパイル・テストするかを一貫して理解できます。

---

## 6. プラグインと設定

Mavenのライフサイクルは、プラグインを使って詳細な設定や拡張を行うことができます。

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.16.0</version>
        </plugin>
    </plugins>
</build>
```

- **プラグイン**: コンパイル、テスト、パッケージ化などの処理を実行する部品
- 例: `maven-compiler-plugin` は Javaのコンパイルを担当
- 例: `maven-surefire-plugin` はテスト実行を担当

Mavenのあらゆる作業は、実際にはこのプラグインの組み合わせで実現されています。

---
