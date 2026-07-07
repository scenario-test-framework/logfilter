# CLAUDE.md

## プロジェクト概要

- 大容量ログから期間/ログレベル/文字列/正規表現でログイベントを抽出する CLI。
- v2.0.0 で Java (Maven/JRE) から Go にリライト済み。旧 Java 実装は git 履歴 (`39ca2e6` の親) にのみ存在する。

## 最重要制約: 旧 Java 版との CLI 後方互換

以下は利用者のスクリプト・alias が依存しているため、変更しないこと。

- オプション体系: `-cf/-f/-ic/-oc/-tf/-tt/-l/-s/-r` + 位置引数 `input [output]`
- 設定ファイル (JSON) のキー名
- exit code: 0=成功 / 3=警告 (出力0行) / 6=エラー
- フィルタ仕様: TimeFilter は `From <= ts < To` (From==To は完全一致)、LogLevelFilter は `" LEVEL "` or `"[LEVEL"` の contains 判定

## 互換性の照合先 (コードから導出できない出典)

- 日時パース (`internal/timeparse`) の原典: **suwa-sh/java リポジトリの `util/src/main/java/me/suwash/util/DateUtils.java`**。挙動に疑問が出たらここと照合する (`gh api repos/suwa-sh/java/contents/...` で取得可能)。
- エラーメッセージ文言の原典: 旧 `src/main/resources/log_filter_message_source_ja.properties` (git 履歴参照)。

## 意図的な非互換 (バグではないので「修正」しないこと)

- 正規表現: Java regex → Go RE2 (lookaround/後方参照は非対応)
- 出力改行: OS 依存 → `\n` 固定
- 最終イベントの出力行数カウント: 旧版のバグ (常に+1) を修正済み
- 旧 `LogFilter` クラス (設定ファイル専用エントリポイント) と `logfilter.sh` は廃止。入力ファイルパスは常に位置引数で、設定ファイルの `inputFilePath` は使用されない
- 空入力ファイル: 警告終了 (旧版はフィルタなし時に出力1行と数える等の不定動作)

## タイムゾーンの扱い

- タイムゾーンなしのタイムスタンプ (`yyyy-MM-dd HH:mm:ss` 等) は **ローカルタイム** で解釈する (旧 Java Calendar と同じ)。
- このため apache ログ (`+0900` 付き) とローカルタイムのフィルタ値の比較は TZ 依存 → engine テストは `TestMain` で `Asia/Tokyo` に固定している。
- Docker イメージは `-tags timetzdata` で TZ データベースを埋め込み済み (scratch でも `TZ=Asia/Tokyo` が効く)。

## テストデータ

- `testdata/` は旧 Java テストの資材を移設したもの。期待値 (apache_access.log 5行→3行、sample.log の ERROR 2イベント12行 等) は旧テストの移植なので、変更する場合は互換性への影響を確認する。

## リリース / インフラ

- `docs/` は **GitHub Pages の配信元** (HTML5UP テンプレートの静的 HTML)。削除・リネーム禁止。実装を変えたら Getting Started の記載も更新する。
- リリースは `git tag vX.Y.Z && git push origin vX.Y.Z` で全自動 (Releases への tar.gz 添付 + GHCR への amd64/arm64 イメージ公開)。
- GHCR は org 設定によりデフォルト public (初回リリース時に公開設定は不要だった)。
