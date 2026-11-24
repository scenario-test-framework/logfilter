# AGENTS.md

## プロジェクト概要

- 大容量ログから期間/ログレベル/文字列/正規表現でログイベントを抽出する CLI。
- v2.0.0 で Java (Maven/JRE) から Go にリライト済み。旧 Java 実装は git 履歴 (`39ca2e6` の親) にのみ存在する。

## 最重要制約: 旧 Java 版との CLI 後方互換

以下は利用者のスクリプト・alias が依存しているため、変更しないこと。

- オプション体系: `-cf/-f/-ic/-oc/-tf/-tt/-l/-s/-r` + 位置引数 `input [output]`
- 設定ファイル (JSON) のキー名
- exit code: 0=成功 / 3=警告 (出力0行) / 6=エラー
- フィルタ仕様: TimeFilter は `From <= ts < To` (From==To は完全一致)、テキスト形式の LogLevelFilter は `" LEVEL "` or `"[LEVEL"` の contains 判定

## 構造化ログ

- `logFormat` (`-lf`) の既定は `text`。空文字も text として従来の行判定・フィルタ・出力を維持する。`json` (JSON Lines) と `logfmt` は明示指定で有効になる。
- `timestampKey` (`-tk`) 未指定時は `time`、`timestamp`、`ts`、`@timestamp` の順に最初に存在するキーを使う。JSON のドット区切りパスは各階層でリテラルキーを優先し、次に短い接頭辞から探索する。logfmt のドットはキー名の一部。
- `timestampFormat` (`-tfmt`) の既定は `auto`。数字だけの文字列は Unix 時刻 (8桁の整数は `timeparse.Parse` で yyyyMMdd を先に試し、不正な暦日なら Unix 秒)、それ以外の文字列は `timeparse.Parse`。timeparse は17桁以上の数字列を固定位置で日時と読むため、数字列を先に timeparse へ渡さないこと。Unix 時刻が0001〜9999年の範囲外なら変換エラー。logfmt の値なしキー (`=` の無い単語) は記録しない。BOM は構造化形式の1行目の解析時だけ除き、出力は元の行のまま。数値は Unix 時刻に変換し、`unix` / `unixms` / `unixus` / `unixns` で単位を固定できる。ナノ秒精度の保持には浮動小数点を経由しないこと。
- タイムスタンプを取得した行からイベントが始まる。解析不能・キーなしの行は直前のイベントの継続行。キーの値が変換不能なら、行番号と値を含むエラーで終了する。文字列・正規表現フィルタと出力には元の行を使う。
- 構造化形式の LogLevelFilter は先頭行の `logLevelKey` (`-lk`、既定候補は `level`、`severity`、`lvl`) を優先する。文字列・数値は大文字小文字を無視した完全一致、それ以外の型やキーなしの場合はイベント全文に対する従来の contains 判定。

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

- `testdata/` には旧 Java テストの資材と、構造化ログ用の `structured.jsonl` / `structured.logfmt` がある。旧テストから移植した期待値 (apache_access.log 5行→3行、sample.log の ERROR 2イベント12行 等) を変更する場合は互換性への影響を確認する。

## リリース / インフラ

- `docs/` は **GitHub Pages の配信元** (HTML5UP テンプレートの静的 HTML)。削除・リネーム禁止。実装を変えたら Getting Started の記載も更新する。
- リリースは `git tag vX.Y.Z && git push origin vX.Y.Z` で全自動 (Releases への tar.gz 添付 + GHCR への amd64/arm64 イメージ公開)。
- GHCR は org 設定によりデフォルト public (初回リリース時に公開設定は不要だった)。
