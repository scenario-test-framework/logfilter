# logfilter

[![CI](https://github.com/scenario-test-framework/logfilter/actions/workflows/ci.yaml/badge.svg)](https://github.com/scenario-test-framework/logfilter/actions/workflows/ci.yaml)

## 概要

大容量のログファイルから、期間やログレベル、文言を指定して、必要なログイベントを抽出するCUIツールです。

Go 製の単一バイナリで動作します (旧 Java 実装から CLI 互換でリライト)。


## 機能

### ログファイルのフィルタリング

指定のログフォーマットで出力されているログファイルから
期間、ログレベル、文字列、正規表現 を指定して、マッチするログイベントだけのファイルを作成します。

タイムスタンプで始まらない行 (スタックトレースなど) は、直前のログイベントの継続行として扱います。

* 対応しているログフォーマット
    * apache access log形式
        * サンプル

            ```
            127.0.0.1 - name [10/Oct/2000:13:55:36 +0900] "GET /apache_pb.gif HTTP/1.0" 200 2326
            ```

    * 「タイムスタンプ ログレベル メッセージ」形式
        * タイムスタンプ
            * 日付
                * 「yyyy/MM/dd」, 「yy/MM/dd」
                * 「yyyy-MM-dd」, 「yy-MM-dd」
                * 「yyyyMMdd」
            * 日付時刻セパレータ
                * 「 （半角スペース）」
                * 「T」
            * 時刻
                * 「なし」
                * 「HH:mm」, 「HH:mm:ss」
                * 「HH:mm:ss.SSS」, 「HH:mm:ss,SSS」
        * タイムゾーン
            * 「なし」
            * 「+0900」
            * 「+0900:」
        * ログレベル
            * 前提
                * タイムスタンプとの区切りが「半角スペース」
                * ログレベルを「半角スペース」か「大カッコ」でくくっている
            * 形式
                * 任意（TRACE～ERROR、FINEST〜FATAL etc）
        * サンプル

            ```
            2016-11-06 12:17:53.985 DEBUG 15765 [      main] m.s.t.l.i.p.BaseLayerSuperType : START validate
            2016-11-06 12:17:54.022 DEBUG 15765 [      main] m.s.t.l.i.p.BaseLayerSuperType : END   validate
            2016-11-06 12:17:54.022 ERROR 15765 [      main] m.s.t.l.s.LogFilterServiceTest : [error.validate]妥当性チェックエラーが発生しました。
            LogFilterServiceInput.timeFilterValueFrom:日時に変換できません。 設定値=unmatched date format
            LogFilterServiceInput.outputCharset:サポートされていない文字コードです。 設定値=NotExist2
            LogFilterServiceInput.inputCharset:サポートされていない文字コードです。 設定値=NotExist
            LogFilterServiceInput.inputFilePath:存在しないパスです。 設定値=/path/to/input
            ```

* 対応している文字コード
    * UTF-8 / Shift_JIS (CP932) / EUC-JP / ISO-2022-JP / UTF-16 と、それぞれの別名 (`utf8`, `sjis`, `MS932` など)


## インストール

* GitHub Releases からお使いのプラットフォームのアーカイブをダウンロードして展開

    ``` sh
    tar xvfz ./logfilter_*.tar.gz
    mv ./logfilter_*/logfilter /usr/local/bin/
    ```

* または Go でビルド

    ``` sh
    go install github.com/scenario-test-framework/logfilter@latest
    ```

* または Docker で実行 (インストール不要)

    ``` sh
    docker pull ghcr.io/scenario-test-framework/logfilter:latest
    ```


## 使い方

``` sh
logfilter [OPTION] inputFilePath [outputFilePath]
```

``` sh
# ヘルプ
logfilter -h

# 期間で抽出
logfilter -tf '2016-11-06 12:17:53.000' -tt '2016-11-06 12:18:00.000' ./app.log ./filtered.log

# ログレベルで抽出
logfilter -l 'WARN,ERROR' ./app.log ./filtered.log

# 文字列・正規表現で抽出
logfilter -s 'Exception' ./app.log ./filtered.log
logfilter -r 'ERROR|Exception' ./app.log ./filtered.log

# 設定ファイル + コマンドオプションで上書き
logfilter -cf ./config.json -tf '2016-11-06' ./app.log ./filtered.log
```

### オプション

| オプション | 説明 |
|---|---|
| `-h`, `--help` | usage を表示します。 |
| `-f`, `--force` | 出力ファイルパスが存在する場合の確認を表示しません。 |
| `-cf`, `--configFile` | 設定ファイル (JSON) のパスを指定します。 |
| `-ic`, `--inputCharset` | 入力ファイルの文字コード。デフォルトは UTF-8。 |
| `-oc`, `--outputCharset` | 出力ファイルの文字コード。デフォルトは入力と同じ。 |
| `-tf`, `--timeFilterFrom` | 抽出を開始する時刻 (この時刻を含む)。 |
| `-tt`, `--timeFilterTo` | 抽出を終了する時刻 (この時刻を含まない)。 |
| `-l`, `--logLevelFilter` | 抽出するログレベル (カンマ区切り)。 |
| `-s`, `--stringContentFilter` | 抽出する文字列 (部分一致)。 |
| `-r`, `--regexFilter` | 抽出する正規表現 ([RE2構文](https://github.com/google/re2/wiki/Syntax))。 |

### 設定ファイル

``` json
{
    "inputCharset": "UTF-8",
    "outputFilePath": "",
    "outputCharset": "",
    "timeFilterValueFrom": "",
    "timeFilterValueTo": "",
    "logLevelFilterValueList": ["WARN", "ERROR"],
    "stringContentFilterValue": "",
    "regexFilterValue": ""
}
```

* 入力ファイルパスは常にコマンドの位置引数で指定します (設定ファイルでは指定できません)。
* 出力ファイルパスは、位置引数を省略した場合に設定ファイルの `outputFilePath` が使用されます。

### リターンコード

| コード | 意味 |
|---|---|
| 0 | 正常終了 |
| 3 | 警告終了 (フィルタにマッチするログイベントなし) |
| 6 | エラー終了 |

### Docker で実行

``` sh
# カレントディレクトリのログをフィルタリング
docker run --rm \
    -u "$(id -u):$(id -g)" \
    -e TZ=Asia/Tokyo \
    -v "$PWD:/work" \
    ghcr.io/scenario-test-framework/logfilter:latest \
    -l 'WARN,ERROR' /work/app.log /work/filtered.log
```

* `-e TZ=...` : タイムゾーンなしのタイムスタンプ (`yyyy-MM-dd HH:mm:ss` 等) の解釈に使用します。未指定の場合は UTC として解釈されます。
* `-u "$(id -u):$(id -g)"` : 出力ファイルの所有者をホストの実行ユーザーに合わせます。

docker compose の場合 ([compose.yaml](./compose.yaml)):

``` sh
docker compose run --rm logfilter -l 'WARN,ERROR' /work/logs/app.log /work/logs/filtered.log
```


## Tips

1. 設定をまとめたファイルを指定＋コマンドで上書きして実行することができます。
下記のように、ルーティン作業を簡素化することも可能です。
    * 設定ファイルで ログレベル：WARN以上 に絞る設定をしておき、コマンドで対象ログファイルを指定する
    * 設定ファイルで 出力先や文字コード を指定しておき、コマンドで期間を今日に絞る

1. 特定の絞り込み条件を指定したコマンドをalias登録しておき、パスだけ指定して動かすこともできます。


## 開発

``` sh
make        # vet + test + build
make cross  # 配布用クロスコンパイル (dist/)
```

* 前提: Go 1.25+

### CI/CD

| ワークフロー | トリガー | 内容 |
|---|---|---|
| [CI](./.github/workflows/ci.yaml) | push / pull_request | vet + test + build + docker build 検証 |
| [Release](./.github/workflows/release.yaml) | `v*` タグ push | 配布アーカイブを GitHub Releases に添付 + マルチアーチイメージ (amd64/arm64) を GHCR に公開 |

リリース手順:

``` sh
git tag v2.0.0
git push origin v2.0.0
```
