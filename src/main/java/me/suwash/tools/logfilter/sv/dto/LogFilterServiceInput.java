package me.suwash.tools.logfilter.sv.dto;

import java.util.List;

import lombok.Data;
import me.suwash.ddd.policy.Input;
import me.suwash.util.validation.constraints.Charset;
import me.suwash.util.validation.constraints.Date;
import me.suwash.util.validation.constraints.ExistPath;
import me.suwash.util.validation.constraints.File;

import org.hibernate.validator.constraints.NotEmpty;


/**
 * ログフィルタサービス入力データ。
 */
@Data
public class LogFilterServiceInput implements Input {

    /** 入力ファイルパス。 */
    @NotEmpty
    @ExistPath
    @File
    private String inputFilePath;

    /** 入力ファイル文字コード。 */
    @NotEmpty
    @Charset
    private String inputCharset;

    /** 出力ファイルパス。 */
    @NotEmpty
    private String outputFilePath;

    /** 出力ファイル文字コード。 */
    @NotEmpty
    @Charset
    private String outputCharset;

    /** TimeFilter設定値：抽出開始時刻。 */
    @Date
    private String timeFilterValueFrom;

    /** TimeFilter設定値：抽出終了時刻。 */
    @Date
    private String timeFilterValueTo;

    /** LogLevelFilter設定値。 */
    private List<String> logLevelFilterValueList;

    /** StringContentFilter設定値。 */
    private String stringContentFilterValue;

    /** RegexFilter設定値。 */
    private String regexFilterValue;

}
