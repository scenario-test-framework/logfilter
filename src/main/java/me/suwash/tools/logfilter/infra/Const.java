package me.suwash.tools.logfilter.infra;

/**
 * 定数定義。
 */
public interface Const extends MessageConst {

    /** デフォルト設定ファイルの読み込み文字コード。 */
    String CHARSET_DEFAULT_CONFIG = "UTF-8";

    /** CUIのリターンコード：成功。 */
    int EXITCODE_SUCCESS = 0;
    /** CUIのリターンコード：警告終了。 */
    int EXITCODE_WARN = 3;
    /** CUIのリターンコード：エラー終了。 */
    int EXITCODE_ERROR = 6;

    /** ユーザ入力：YES。 */
    String USERINPUT_YES = "y";
}
