package me.suwash.tools.logfilter.sv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.logfilter.infra.Const;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;
import me.suwash.tools.logfilter.infra.i18n.LogFilterMessageSource;
import me.suwash.tools.logfilter.sv.dto.LogFilterServiceInput;
import me.suwash.tools.logfilter.sv.dto.LogFilterServiceOutput;
import me.suwash.util.DateUtils;
import me.suwash.util.RuntimeUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

/**
 * フィルタリング処理。
 */
@lombok.extern.slf4j.Slf4j
public class LogFilterService extends BaseService<LogFilterServiceInput, LogFilterServiceOutput> {

    /** INFOログ出力する行数間隔。 */
    private static final int REPORT_LINE_COUNT = 100000;

    /* (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#preExecute(me.suwash.ddd.policy.Input)
     */
    @Override
    protected LogFilterServiceOutput preExecute(LogFilterServiceInput input) {
        // --------------------------------------------------
        // 設定の表示
        // --------------------------------------------------
        log.info("・入力ファイル");
        log.info("    ・パス      : " + input.getInputFilePath());
        log.info("    ・文字コード: " + input.getInputCharset());
        log.info("・出力ファイル");
        log.info("    ・パス      : " + input.getOutputFilePath());
        log.info("    ・文字コード: " + input.getOutputCharset());
        log.info("・フィルタ設定");
        log.info("    ・TimeFilter");
        log.info("        ・From : " + input.getTimeFilterValueFrom());
        log.info("        ・To   : " + input.getTimeFilterValueTo());
        log.info("    ・LogLevelFilter");
        log.info("        ・Value: " + input.getLogLevelFilterValueList());
        log.info("    ・StringContentFilter");
        log.info("        ・Value: " + input.getStringContentFilterValue());
        log.info("    ・RegexFilter");
        log.info("        ・Value: " + input.getRegexFilterValue());

        // ----------------------------------------
        // 関連チェック
        // ----------------------------------------
        // TimeFilterの前後確認
        final String timeFileterValueFrom = input.getTimeFilterValueFrom();
        final String timeFilterValueTo = input.getTimeFilterValueTo();
        if (!StringUtils.isEmpty(timeFileterValueFrom) && !StringUtils.isEmpty(timeFilterValueTo)) {
            // From & To指定
            final Date fromTimestamp = DateUtils.toDate(timeFileterValueFrom);
            final Date toTimestamp = DateUtils.toDate(timeFilterValueTo);
            if (fromTimestamp.compareTo(toTimestamp) > 0) {
                // To < From の場合、エラー
                throw new LogFilterException(Const.MSGCD_LOGFILTER_SERVICE_TIMEFILTER_INVALIDRANGE);
            }
        }

        // ----------------------------------------
        // 出力データモデルの初期化
        // ----------------------------------------
        LogFilterServiceOutput output = new LogFilterServiceOutput();
        output.setInput(input);
        return output;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#mainExecute(me.suwash.ddd.policy.Input, me.suwash.ddd.policy.Output)
     */
    @Override
    protected LogFilterServiceOutput mainExecute(final LogFilterServiceInput input, final LogFilterServiceOutput preExecuteOutput) {
        // --------------------------------------------------
        // readerの作成
        // --------------------------------------------------
        final File inputFile = new File(input.getInputFilePath());
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), input.getInputCharset()));
        } catch (Exception e) {
            throw new LogFilterException(Const.FILE_CANTREAD, new Object[] { inputFile }, e);
        }

        // --------------------------------------------------
        // writerの作成
        // --------------------------------------------------
        final File outputFile = new File(input.getOutputFilePath());
        final String outputCharset = input.getOutputCharset();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), outputCharset));
        } catch (Exception e) {
            try {
                reader.close();
            } catch (IOException e1) {
                final String curErrorMessage = LogFilterMessageSource.getInstance().getMessage(Const.STREAM_CANTCLOSE_INPUT, new Object[] {
                    inputFile
                });
                final String message = LogFilterMessageSource.getInstance().getMessage(Const.ERROR_ON_ERRORHANDLE, new Object[] {
                    "createWriter", curErrorMessage
                });
                log.error(message, e1);

            }
            throw new LogFilterException(Const.FILE_CANTWRITE, new Object[] { outputFile }, e);
        }

        // --------------------------------------------------
        // フィルタリング
        // --------------------------------------------------
        // inputをループ
        long inputRowCount = 0;
        long outputRowCount = 0;
        String curRow = StringUtils.EMPTY;
        StringBuilder curLogEvent = new StringBuilder();
        final String outputLineSp = SystemUtils.LINE_SEPARATOR;
        try {
            while ((curRow = reader.readLine()) != null) {
                inputRowCount++;

                if (log.isTraceEnabled() && inputRowCount % 10000 == 0) {
                    log.trace(inputRowCount + " 行目：" + RuntimeUtils.getMemoryInfo());
                }
                if (inputRowCount % REPORT_LINE_COUNT == 0) {
                    log.info(inputRowCount + " rows finished.");
                }

                // 1行目でログフォーマットを確認
                if (inputRowCount == 1) {
                    getLogTimestamp(curRow);
                }

                // 当該行の文字列を確認
                if (isLogEventChanged(curRow) && inputRowCount != 1) {
                    // ログイベントが切り替わる行の場合
                    // 蓄積したログイベントの文言がフィルタにマッチするか確認
                    if (isWriteLogEvent(curLogEvent, input)) {
                        // マッチする場合、出力
                        writer.write(curLogEvent.toString());
                        final int writeLineSpCount = StringUtils.countMatches(curLogEvent.toString(), outputLineSp);
                        outputRowCount = outputRowCount + writeLineSpCount;
                    }

                    // ログイベントの蓄積をリフレッシュ
                    curLogEvent = new StringBuilder();
                    curLogEvent.append(curRow).append(outputLineSp);

                } else {
                    // ログイベントが継続中の場合
                    curLogEvent.append(curRow).append(outputLineSp);
                }
            }

        } catch (IOException e) {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    final String curErrorMessage = LogFilterMessageSource.getInstance().getMessage(Const.STREAM_CANTCLOSE_INPUT, new Object[] {
                        inputFile
                    });
                    final String message = LogFilterMessageSource.getInstance().getMessage(Const.ERROR_ON_ERRORHANDLE, new Object[] {
                        "LogFilterService.filtering", curErrorMessage
                    });
                    log.error(message, e1);
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e1) {
                    final String curErrorMessage = LogFilterMessageSource.getInstance().getMessage(Const.STREAM_CANTCLOSE_INPUT, new Object[] {
                        inputFile
                    });
                    final String message = LogFilterMessageSource.getInstance().getMessage(Const.ERROR_ON_ERRORHANDLE, new Object[] {
                        "LogFilterService.filtering", curErrorMessage
                    });
                    log.error(message, e1);
                }
            }
            throw new LogFilterException(Const.MSGCD_LOGFILTER_SERVICE_ERRORHANDLE, new Object[] {inputFile, inputRowCount}, e);
        }

        // 最後のログイベントがフィルタにマッチするか確認
        if (isWriteLogEvent(curLogEvent, input)) {
            // マッチする場合、出力
            try {
                writer.write(curLogEvent.toString());
            } catch (IOException e) {
                try {
                    reader.close();
                    writer.close();
                } catch (IOException e1) {
                    final String curErrorMessage = LogFilterMessageSource.getInstance().getMessage("error.streamClose");
                    final String message = LogFilterMessageSource.getInstance().getMessage(Const.ERROR_ON_ERRORHANDLE, new Object[] {
                        "LogFilterService.filtering", curErrorMessage
                    });
                    log.error(message, e1);
                }
                throw new LogFilterException(Const.MSGCD_LOGFILTER_SERVICE_ERRORHANDLE, new Object[] {inputFile, inputRowCount}, e);
            }
            outputRowCount++;
        }

        // --------------------------------------------------
        // streamをclose
        // --------------------------------------------------
        try {
            reader.close();
            writer.close();
        } catch (IOException e) {
            throw new LogFilterException(Const.MSGCD_ERROR_STREAM_CLOSE, e);
        }

        // --------------------------------------------------
        // 結果反映
        // --------------------------------------------------
        preExecuteOutput.setInputRowCount(inputRowCount);
        preExecuteOutput.setOutputRowCount(outputRowCount);

        if (outputRowCount == 0) {
            preExecuteOutput.setProcessStatus(ProcessStatus.Warning);
            log.warn(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_LOGFILTER_SERVICE_UNMATCHED));
        }

        return preExecuteOutput;
    }

    /* (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#postExecute(me.suwash.ddd.policy.Input, me.suwash.ddd.policy.Output)
     */
    @Override
    protected LogFilterServiceOutput postExecute(final LogFilterServiceInput input, final LogFilterServiceOutput mainExecuteOutput) {
        // 処理ステータスが未設定の場合、正常終了を設定
        if (ProcessStatus.Processing.equals(mainExecuteOutput.getProcessStatus())) {
            mainExecuteOutput.setProcessStatus(ProcessStatus.Success);
        }
        return mainExecuteOutput;
    }

    /**
     * ログイベントが切り替わった行か否かを返します。
     *
     * @param curRow 現在行文字列
     * @return yyyy-MM-dd hh:mm:ss.SSS など 日時を含む場合、true
     */
    private boolean isLogEventChanged(final String curRow) {
        try {
            // 変換できた場合、true
            getLogTimestamp(curRow);
            return true;

        } catch (Exception e) {
            // 変換できない場合、false
            return false;
        }
    }

    /**
     * 指定されたフィルタを適用して、出力するべき行か否かを返します。
     *
     * @param curLogEvent 判定対象のログイベント
     * @param dto 入力データモデル
     * @return フィルタの適用結果
     */
    private boolean isWriteLogEvent(final StringBuilder curLogEvent, final LogFilterServiceInput dto) {
        // --------------------------------------------------
        // TimeFilter
        // --------------------------------------------------
        final String timeFileterValueFrom = dto.getTimeFilterValueFrom();
        final String timeFilterValueTo = dto.getTimeFilterValueTo();

        if (!StringUtils.isEmpty(timeFileterValueFrom) || !StringUtils.isEmpty(timeFilterValueTo)) {
            // フィルタあり
            boolean isWrite = false;

            // 取得できたタイムスタンプを採用
            final Date logEventTimestamp = getLogTimestamp(curLogEvent.toString());
            if (logEventTimestamp != null) {
                // フィルタ設定と比較
                if (!StringUtils.isEmpty(timeFileterValueFrom) && StringUtils.isEmpty(timeFilterValueTo)) {
                    // Fromのみ指定
                    final Date fromTimestamp = DateUtils.toDate(timeFileterValueFrom);
                    if (logEventTimestamp.compareTo(fromTimestamp) >= 0) {
                        isWrite = true;
                    }

                } else if (StringUtils.isEmpty(timeFileterValueFrom) && !StringUtils.isEmpty(timeFilterValueTo)) {
                    // Toのみ指定
                    final Date toTimestamp = DateUtils.toDate(timeFilterValueTo);
                    if (logEventTimestamp.compareTo(toTimestamp) < 0) {
                        isWrite = true;
                    }

                } else if (!StringUtils.isEmpty(timeFileterValueFrom) && !StringUtils.isEmpty(timeFilterValueTo)) {
                    // From & To指定
                    final Date fromTimestamp = DateUtils.toDate(timeFileterValueFrom);
                    final Date toTimestamp = DateUtils.toDate(timeFilterValueTo);
                    if (fromTimestamp.compareTo(toTimestamp) == 0) {
                        // From = To の場合、完全一致を確認
                        if (logEventTimestamp.compareTo(fromTimestamp) == 0) {
                            isWrite = true;
                        }

                    } else {
                        // From < To の場合、From <= timestamp < To を確認
                        if ( logEventTimestamp.compareTo(fromTimestamp) >= 0 &&
                            logEventTimestamp.compareTo(toTimestamp) < 0 ) {
                            isWrite = true;
                        }
                    }
                }
            }

            if (!isWrite) {
                // TimeFilterの結果が出力対象でない場合、false
                return false;
            }
        }

        // --------------------------------------------------
        // LogLevelFilter
        // --------------------------------------------------
        final List<String> logLevelFilterValueList = dto.getLogLevelFilterValueList();
        if (logLevelFilterValueList != null && !logLevelFilterValueList.isEmpty()) {
            // フィルタあり
            boolean isWrite = false;

            final String curLogEventValue = curLogEvent.toString();
            for (final String curLogLevel : logLevelFilterValueList) {

                // Pattern pattern = Pattern.compile(".*\\[" + curLogLevel + " *\\].*");
                // Matcher matcher = pattern.matcher(curLogEventValue);
                // if (curLogEventValue.contains(" " + curLogLevel + " ") || matcher.find()) {
                // []括りを正規表現で確認すると処理が重いので、先頭に[が付いていることで代替しています。
                if (curLogEventValue.contains(" " + curLogLevel + " ") || curLogEventValue.contains("[" + curLogLevel)) {
                    isWrite = true;
                }
            }

            if (!isWrite) {
                // LogLevelFilterの結果が出力対象でない場合、false
                return false;
            }
        }

        // --------------------------------------------------
        // StringContentFilter
        // --------------------------------------------------
        final String stringContentFilterValue = dto.getStringContentFilterValue();
        if (!StringUtils.isEmpty(stringContentFilterValue)) {
            // フィルタあり
            boolean isWrite = false;

            final String curLogEventValue = curLogEvent.toString();
            if (curLogEventValue.contains(stringContentFilterValue)) {
                isWrite = true;
            }

            if (!isWrite) {
                // StringContentFilterの結果が出力対象でない場合、false
                return false;
            }
        }

        // --------------------------------------------------
        // RegexFilter
        // --------------------------------------------------
        final String regexFilterValue = dto.getRegexFilterValue();
        if (!StringUtils.isEmpty(regexFilterValue)) {
            // フィルタあり
            boolean isWrite = false;

            final String curLogEventValue = curLogEvent.toString();
            final Pattern pattern = Pattern.compile(regexFilterValue);
            final Matcher matcher = pattern.matcher(curLogEventValue);

            if (matcher.find()) {
                isWrite = true;
            }

            if (!isWrite) {
                // RegexFilterの結果が出力対象でない場合、false
                return false;
            }
        }

        // 全てのフィルタを通過した場合、true
        return true;
    }

    /**
     * 1〜5フィールド目を利用して日時変換を実行します。
     *
     * @param target 対象文字列
     * @return 変換した日時
     */
    private Date getLogTimestamp(final String target) {
        return getLogTimestamp(target, 1, 5, ' ');
    }

    /**
     * 指定フィールド間の1 or 2フィールドを利用して、日時変換を実行します。
     *
     * @param target 対象文字列
     * @param startFieldNum 開始フィールド番号
     * @param endFieldNum 終了フィールド番号
     * @param separator 区切り文字
     * @return 変換した日時
     */
    private Date getLogTimestamp(final String target, final int startFieldNum, final int endFieldNum, final char separator) {
        Date logEventTimestamp = null;
        for (int curFieldNum = startFieldNum; curFieldNum <= endFieldNum; curFieldNum++) {
            // 現在フィールドを取得
            final String curFieldValue = getField(target, separator, curFieldNum);
            if (StringUtils.isEmpty(curFieldValue)) {
                // 取得できない場合、エラー
                throw new LogFilterException(Const.MSGCD_LOGFILTER_SERVICE_UNSUPPORTED_RECORD, new Object[] {target});
            }

            String nextFieldValue = StringUtils.EMPTY;
            String targetValue = StringUtils.EMPTY;

            // 現在フィールド + 次フィールド で変換
            if (curFieldNum < endFieldNum) {
                nextFieldValue = getField(target, separator, curFieldNum + 1);
                targetValue = curFieldValue + ' ' + nextFieldValue;
                try {
                    logEventTimestamp = DateUtils.toDate(targetValue);
                } catch (Exception e) {
                    // 破棄
                }
                if (logEventTimestamp != null) {
                    return logEventTimestamp;
                }
            }

            // 現在フィールドのみで変換
            targetValue = curFieldValue;
            try {
                logEventTimestamp = DateUtils.toDate(targetValue);
            } catch (Exception e) {
                // 破棄
            }
            if (logEventTimestamp != null) {
                return logEventTimestamp;
            }
        }

        // 取得できない場合、エラー
        throw new LogFilterException(Const.MSGCD_LOGFILTER_SERVICE_UNSUPPORTED_RECORD, new Object[] {target});
    }

    /**
     * フィールド値を返します。
     *
     * @param line 行データ
     * @param separator 区切り文字
     * @param fieldNum フィールド番号
     * @return フィールド番号
     */
    private String getField(final String line, final char separator, final int fieldNum) {
        int curFieldNum = 0;
        int beforePos = 0;
        int curPos = line.indexOf(separator);

        while (curPos > 0) {
            // 現在フィールド番号を更新
            curFieldNum++;

            if (curFieldNum == fieldNum) {
                // 指定フィールドと一致する場合、部分文字列を返却
                return line.substring(beforePos + 1, curPos);
            }
            // インデックスを更新
            beforePos = curPos;
            curPos = line.indexOf(separator, beforePos + 1);
        }

        // 取得できない場合、空文字を返却
        return StringUtils.EMPTY;
    }

}
