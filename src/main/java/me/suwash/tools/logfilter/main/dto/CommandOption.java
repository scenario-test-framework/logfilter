package me.suwash.tools.logfilter.main.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import com.beust.jcommander.Parameter;

/**
 * コマンドラインオプション。
 */
@Data
public class CommandOption {

    /** 設定ファイルパス。 */
    @Parameter(names = { "-cf", "--configFile" })
    private String configFilePath;

    /** 強制上書きフラグ。 */
    @Parameter(names = { "-f", "--force" })
    private boolean forceOverWrite = false;

    /** 入力文字コード。 */
    @Parameter(names = { "-ic", "--inputCharset" })
    private String inputCharset;

    /** 出力文字コード。 */
    @Parameter(names = { "-oc", "--outputCharset" })
    private String outputCharset;

    /** TimeFilter設定値: From。 */
    @Parameter(names = { "-tf", "--timeFilterFrom" })
    private String timeFilterFrom;

    /** TimeFilter設定値: To。 */
    @Parameter(names = { "-tt", "--timeFilterTo" })
    private String timeFilterTo;

    /** LogLevelFilter設定値。 */
    @Parameter(names = { "-l", "--logLevelFilter" })
    private String logLevelFilter;

    /** StringContentFilter設定値。 */
    @Parameter(names = { "-s", "--stringContentFilter" })
    private String stringContentFilter;

    /** regexFilter設定値。 */
    @Parameter(names = { "-r", "--regexFilter" })
    private String regexFilter;

    /** その他のパラメータ。 */
    @Parameter
    private List<String> parameters = new ArrayList<String>();

}
