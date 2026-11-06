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

Flyway がデータベーススキーマを自動的に管理します。マイグレーションファイルは `src/main/resources/db/migration/` に配置されています。アプリケーション起動時に、Flyway が未適用のマイグレーションをすべて適用してからリクエストの受け付けを開始します。

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