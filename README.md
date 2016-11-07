# logfilter

## 概要

大容量のログファイルから、期間やログレベル、文言を指定して、必要なログイベントを抽出するCUIツールです。


## 機能

### ログファイルのフィルタリング

指定のログフォーマットで出力されているログファイルから
期間、ログレベル、文字列、正規表現 を指定して、マッチするログイベントだけのファイルを作成します。

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


## 環境構築手順

* 配布アーカイブ（tar.gz）を、ログファイルが配置されているサーバの任意のディレクトリに展開

``` sh
cd ${LOGFILTER_PARENT_DIR}
tar xvfz ./logfilter*.tar.gz
```

* Linux系OSの場合、下記のコマンドでshellに実行権限を付与

``` sh
chmod 755 ${LOGFILTER_ROOT}/bin/*.sh
```


## ヘルプ

``` sh
${LOGFILTER_ROOT}/bin/logfilter.sh -h
```


## Tips

1. 設定をまとめたファイルを指定＋コマンドで上書きして実行することができます。
下記のように、ルーティン作業を簡素化することも可能です。
    * 設定ファイルで ログレベル：WARN以上 に絞る設定をしておき、コマンドで対象ログファイルを指定する
    * 設定ファイルで 対象ログファイル を指定しておき、コマンドで期間を今日に絞る

1. 特定の絞り込み条件を指定したコマンドをalias登録しておき、パスだけ指定して動かすこともできます。


## 前提

* JRE 1.6 ~
