# WebFluxDemo

Spring WebFlux、PostgreSQL R2DBC、Gradle を使用したリアクティブ Web アプリケーションのデモプロジェクトです。

## 技術スタック

- **Spring Boot 4.1.2**
- **Spring WebFlux** - リアクティブ Web フレームワーク
- **Spring Security** - 認証・認可（API エンドポイント保護）
- **Thymeleaf Security Dialect** - テンプレートでのセキュリティ機能
- **Bean Validation** - 入力検証（`@NotBlank`, `@Email` など）
- **Thymeleaf** - サーバーサイドテンプレートエンジン
- **R2DBC** - リアクティブなリレーショナルデータベース接続
- **Flyway** - データベースマイグレーション管理
- **PostgreSQL** - データベース（`r2dbc-postgresql` ドライバー使用）
- **Gradle 9.8.0** - ビルドツール（Kotlin DSL）
- **Spring Boot DevTools** - 開発時のホットリロード
- **BootUI** - 開発者用コンソール（`/bootui/`）
- **htmx** - HTMLオーバーHTTP（自動リフレッシュ機能）
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

## 環境変数の設定

データベースの認証情報は環境変数で管理できます。`.env.sample` を `.env` にコピーして使用してください。

```bash
cp .env.sample .env
```

`.env` ファイルには以下の環境変数を設定できます（`.gitignore` で除外されています）。

```
DB_NAME=webfluxdb
DB_USERNAME=postgres
DB_PASSWORD=postgres

# セキュリティ設定（デフォルト: user/user）
SECURITY_USERNAME=user
SECURITY_PASSWORD=user

# Webページ認証有効化（デフォルト: false = 公開）
SECURITY_WEB_AUTH=false
```

環境変数を設定しない場合は、`application.yml` に定義されたデフォルト値が使用されます。

## セキュリティ

Spring Security が導入されており、API エンドポイント（`/api/**`、`/router/api/**`）は HTTP Basic 認証で保護されています。

- ユーザー名: `user`（環境変数 `SECURITY_USERNAME` で変更可能）
- パスワード: `user`（環境変数 `SECURITY_PASSWORD` で変更可能）

Web ページ（`/users/**`、`/router/users/**`）はデモ用に公開されています。`SECURITY_WEB_AUTH=true` を設定すると、Web ページも認証が必要になります。

### CSRF 保護

CSRF（クロスサイトリクエストフォージェリ）保護について：

- API エンドポイントはステートレスな HTTP Basic 認証を使用するため、CSRF 保護は不要で無効化されています
- Web フォームには Thymeleaf Security Dialect を介して CSRF トークンが埋め込まれており、防御的な対策として機能します
- `CookieServerCsrfTokenRepository` が設定されており、トークンは Cookie に格納されます

### 入力検証

すべてのエンドポイントで入力検証が行われています。

- **REST API**: Bean Validation（`@Valid`）と `@RestControllerAdvice`（`GlobalExceptionHandler`）で検証エラーを JSON で返します
- **RouterFunction API**: `UserHandler` で手動検証を行い、検証エラーを JSON で返します
- **Web フォーム**: `BindingResult` で検証エラーをキャッチし、エラーメッセージをフォームに表示します

### ページパラメータのバリデーション

ページングパラメータ（`page`、`size`）には以下の制限があります：

- `page`: 0 以上（負の値は 0 に補正）
- `size`: 1〜100（デフォルト 10、100 超は 100 に補正）

### API 検証エラーハンドリング

`@RestControllerAdvice` を使用して、API エンドポイントでの入力検証エラーを一元管理しています。無効なリクエストボディが送信されると、400 Bad Request とともに検証エラーの詳細が JSON で返されます。

API リクエストの例（認証付き）:

```bash
curl -u user:user http://localhost:8080/api/users
```

## 環境プロファイル

アプリケーションは環境ごとに設定を切り替えられます。

- **dev**（デフォルト）: DEBUG ログ有効、DevTools 有効
- **prod**: WARN ログのみ、DevTools 無効

プロファイルの切り替え:

```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
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

`CustomErrorController` が `spring.profiles.active` を基に `env` 変数を設定し、開発環境でのみ詳細情報を表示します。

- 開発環境（devプロファイル）では、ステータスコード、時刻、パス、メッセージなどの詳細情報が表示されます
- 本番環境（prodプロファイル）では、一般的なエラーメッセージのみが表示され、機密情報が漏洩しません

## 入力検証

User エンティティに Bean Validation アノテーション（`@NotBlank`、`@Email`、`@Size`）が設定されており、サーバーサイドで入力データの検証が行われます。無効なデータ（空の名前、不正なメールアドレスなど）は拒否されます。

- **REST API**: `@Valid` アノテーションと `@RestControllerAdvice`（`GlobalExceptionHandler`）で検証エラーを JSON で返します
- **Web フォーム**: `BindingResult` で検証エラーをキャッチし、エラーメッセージをフォームに表示します
- **RouterFunction**: `UserHandler` で手動検証を行い、エラー時にフォームページに戻ります

## Web ページ（@Controller）

| メソッド | エンドポイント      | 説明             |
|--------|-------------------|------------------|
| GET    | /users            | ユーザー一覧ページ    |
| GET    | /users/new        | ユーザー作成フォーム  |
| GET    | /users/edit/{id}  | ユーザー編集フォーム  |
| POST   | /users/submit     | フォーム送信         |

### ページング

ユーザー一覧ページにはページング機能が実装されています。デフォルトで1ページあたり10件表示し、ページ番号とサイズをクエリパラメータで指定できます。

```
/users?page=1&size=5
```

ページネーションUIには以下の情報が表示されます：

- 総件数と現在の表示範囲（例：「25件中、11〜20件表示」）
- 前へ・次へボタン
- ページ番号リンク（現在ページはハイライト表示）
- ページ数が5ページを超える場合は「...」で省略表示

RouterFunction の一覧ページ（`/router/users`）でも同様のページング機能が利用できます。

### htmx 自動リフレッシュ

ユーザー一覧ページには htmx を使用した自動リフレッシュ機能が実装されています。10秒ごとにテーブルとページネーションをサーバーから最新のデータで更新します。

- **htmx** は JavaScript の最小限の使用で、HTML を HTTP 経由で動的に読み込むライブラリです
- CDN（`https://unpkg.com/htmx.org@2.0.4`）から読み込んでおり、SRI（Subresource Integrity）で改ざん防止しています
- テーブル上部に「⟳ 自動更新中」インジケーターが表示され、緑色の点滅アニメーションで更新間隔を視覚的に示します
- リクエスト中はインジケーターが回転アニメーションに切り替わります
- ページネーションでページを切り替えても、htmx は新しいページに対応した URL を自動的に追跡して更新し続けます

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

API エンドポイントは HTTP Basic 認証が必要です。

```bash
# ユーザー一覧取得
curl -u user:user http://localhost:8080/api/users

# ユーザー作成
curl -u user:user -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com"}'
```