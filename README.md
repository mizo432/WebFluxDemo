# WebFluxDemo

Spring WebFlux、PostgreSQL R2DBC、Gradle を使用したリアクティブ Web アプリケーションのデモプロジェクトです。

## 技術スタック

- **Spring Boot 4.1.2**
- **Spring WebFlux** - リアクティブ Web フレームワーク
- **Thymeleaf** - サーバーサイドテンプレートエンジン
- **R2DBC** - リアクティブなリレーショナルデータベース接続
- **Flyway** - データベースマイグレーション管理
- **PostgreSQL** - データベース（`r2dbc-postgresql` ドライバー使用）
- **Gradle 9.8.0** - ビルドツール（Kotlin DSL）
- **Spring Boot DevTools** - 開発時のホットリロード
- **BootUI** - 開発者用コンソール（`/bootui/`）
- **デジタル庁デザインシステム** - WebページのUIデザイン
- **Java 25**

## 前提条件

- Java 25 以降
- Docker および Docker Compose（ローカルデータベースの設定用）

## Docker Compose を使ったクイックスタート

最も簡単な方法は Docker Compose を使用することです。PostgreSQL が自動的にセットアップされます。

```bash
docker compose up -d
```

これにより、`localhost:5432` で PostgreSQL 16 コンテナが起動し、データベース `webfluxdb` が作成されます。アプリケーション起動時に Flyway が自動的にスキーママイグレーションを適用します。

## アプリケーションの実行

```bash
./gradlew bootRun
```

アプリケーションは `http://localhost:8080` で起動します。

### DevTools（ホットリロード）

Spring Boot DevTools が開発用依存関係として設定されています。開発中に Java コードや Thymeleaf テンプレートを修正すると、アプリケーションが自動的に再起動し、ブラウザのリフレッシュで変更が反映されます。

DevTools は `developmentOnly` スコープで追加されているため、本番環境では自動的に無効化されます。

### BootUI（開発者用コンソール）

BootUI が開発用依存関係として設定されています。アプリケーション起動中に `http://localhost:8080/bootui/` にアクセスすると、ランタイムの可観測性パネル（ヘルスチェック、メトリクス、メモリ、スレッド、ヒープダンプ、起動時間、JVM サイズなど）を確認できます。

BootUI は `developmentOnly` スコープで追加されているため、本番環境では自動的に無効化されます。

### Java 25 の Netty 警告について

Java 25 でアプリケーションを起動すると、以下のような WARNING が出力されることがあります。

```
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::loadLibrary has been called by io.netty.util.internal.NativeLibraryUtil
```

これは Java 25 で導入されたネイティブアクセス制限による警告です。`build.gradle.kts` の `bootRun` タスクに `--enable-native-access=ALL-UNNAMED` JVM オプションが設定されており、この警告は抑制されています。

JAR ファイルとして実行する場合は、以下のようにオプションを指定してください。

```bash
java --enable-native-access=ALL-UNNAMED -jar build/libs/WebFluxDemo-0.0.1-SNAPSHOT.jar
```

### macOS DNS 解決について

macOS で Netty を使用すると、以下のような ERROR ログが出力されることがあります。

```
Unable to load io.netty.resolver.dns.macos.MacOSDnsServerAddressStreamProvider, fallback to system defaults.
```

これは Netty の macOS 用 DNS ネイティブライブラリが不足しているためです。`build.gradle.kts` に `netty-resolver-dns-native-macos` 依存関係が追加されており、Apple Silicon（ARM64）環境で正しい DNS 解決が行われるようにしています。

## データベースの停止

```bash
docker compose down
```

データベースボリュームを削除して最初から始めたい場合は、以下を実行します。

```bash
docker compose down -v
```

## データベースの設定

`src/main/resources/application.yml` を編集してデータベースの認証情報を更新してください。

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/webfluxdb
    username: postgres
    password: postgres
  flyway:
    url: jdbc:postgresql://localhost:5432/webfluxdb
    user: postgres
    password: postgres
```

## データベースマイグレーション

Flyway がデータベーススキーマを自動的に管理します。マイグレーションファイルは `src/main/resources/db/migration/` に配置されています。

Spring WebFlux（R2DBC）環境では Flyway の自動設定が有効にならないため、`FlywayConfiguration.java` で Flyway Bean を手動で定義しています。アプリケーション起動時に、Flyway が未適用のマイグレーションをすべて適用してからリクエストの受け付けを開始します。

新しいマイグレーションを追加する場合は、`src/main/resources/db/migration/` に `V{version}__description.sql` 形式でファイルを作成してください。

## UI デザイン

Webページは[デジタル庁デザインシステム](https://design.digital.go.jp/)に基づいてデザインされています。デジタル庁が公開する日本の行政サービス向けのデザインシステムで、アクセシビリティ、統一感、再現性を重視した設計が特徴です。

- **dads-global.css** - デジタル庁デザインシステムのデザイントークン（カラーパレット、タイポグラフィ、レイアウトなど）
- **dads-components.css** - プロジェクト固有のコンポーネントスタイル（ボタン、テーブル、フォーム、カードなど）

すべてのThymeleafテンプレートでこれらのCSSを使用しており、デジタル庁デザインシステムのユーティリティクラス（`.dads-u-std-*`、`.dads-u-dsp-*` など）を適用しています。

## エラーページ

Spring BootのWhitelabel Error Page（白画面）の代わりに、カスタムエラーページ（`error.html`）が設定されています。エラーが発生した際（404 Not Found、500 Internal Server Errorなど）、デジタル庁デザインシステムのスタイルを適用したエラーページが表示されます。

エラーページには以下の情報が表示されます：

- エラーメッセージ
- ステータスコード
- 発生時刻
- リクエストパス
- トップページに戻る・前のページに戻るリンク

## Web ページ（@Controller）

| メソッド | エンドポイント      | 説明             |
|--------|-------------------|------------------|
| GET    | /users            | ユーザー一覧ページ    |
| GET    | /users/new        | ユーザー作成フォーム  |
| GET    | /users/edit/{id}  | ユーザー編集フォーム  |
| POST   | /users/submit     | フォーム送信         |

## RouterFunction Web ページ（Functional Endpoints）

Spring WebFlux の Functional Endpoints 方式（RouterFunction + Handler）で実装した CRUD サンプルです。
既存の `@Controller` 方式とは別に、`/router/` プレフィックスでアクセスできます。

| メソッド | エンドポイント            | 説明             |
|--------|-------------------------|------------------|
| GET    | /router/users           | ユーザー一覧ページ    |
| GET    | /router/users/new       | ユーザー作成フォーム  |
| GET    | /router/users/edit/{id} | ユーザー編集フォーム  |
| POST   | /router/users/submit    | フォーム送信         |
| POST   | /router/users/delete    | ユーザー削除（フォーム）|

## RouterFunction API エンドポイント（Functional Endpoints）

| メソッド | エンドポイント           | 説明             |
|--------|-----------------------|------------------|
| GET    | /router/api/users     | ユーザー一覧取得      |
| GET    | /router/api/users/{id}| ユーザー詳細取得      |
| POST   | /router/api/users     | ユーザー作成         |
| PUT    | /router/api/users/{id}| ユーザー更新         |
| DELETE | /router/api/users/{id}| ユーザー削除         |

## API エンドポイント

| メソッド | エンドポイント      | 説明             |
|--------|------------------|------------------|
| GET    | /api/users       | ユーザー一覧取得      |
| GET    | /api/users/{id}  | ユーザー詳細取得      |
| POST   | /api/users       | ユーザー作成         |
| PUT    | /api/users/{id}  | ユーザー更新         |
| DELETE | /api/users/{id}  | ユーザー削除         |

## リクエストの例

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```