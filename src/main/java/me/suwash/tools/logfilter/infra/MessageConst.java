package me.suwash.tools.logfilter.infra;

import me.suwash.util.constant.UtilMessageConst;

/**
 * メッセージコード。
 */
public interface MessageConst extends UtilMessageConst {

    /** 処理が終了しました。 */
    String MSGCD_EXIT_SUCCESS = "exit.success";
    /** 処理が警告終了しました。 */
    String MSGCD_EXIT_WARN = "exit.warn";
    /** 処理が異常終了しました。 */
    String MSGCD_EXIT_FAIL = "exit.fail";
    /** 想定外のエラーが発生しました。 */
    String MSGCD_EXIT_ERROR = "exit.error";

    /** 引数の指定内容に誤りがあります。 */
    String MSGCD_ERROR_ARG = "error.arg";
    /** {0} のパースに失敗しました。 */
    String MSGCD_ERROR_PARSE = "error.parse";
    /** 妥当性チェックエラーが発生しました。 */
    String MSGCD_ERROR_VALIDATE = "error.validate";
    /** ストリームを閉じられません。 */
    String MSGCD_ERROR_STREAM_CLOSE = "error.streamClose";

    /** {0} 設定値={1}。 */
    String MSGCD_WRAP_VALIDATE_ERROR = "wrap.validateError";

    /** 入力ファイル：{0}.{1} 行目の入出力でエラーが発生しました。 */
    String MSGCD_LOGFILTER_SERVICE_ERRORHANDLE = "LogFilterService.errorHandle";
    /** フィルタにマッチするログイベントは存在しませんでした。 */
    String MSGCD_LOGFILTER_SERVICE_UNMATCHED = "LogFilterService.unmatched";
    /** ログイベントからタイムスタンプを検出できませんでした。対象文字列：{0}。 */
    String MSGCD_LOGFILTER_SERVICE_UNSUPPORTED_RECORD = "LogFilterService.unsupportedRecord";
    /** TimeFilterのFromは、Toより前の時刻を設定して下さい。 */
    String MSGCD_LOGFILTER_SERVICE_TIMEFILTER_INVALIDRANGE = "LogFilterService.timeFilter.invalidRange";

}
