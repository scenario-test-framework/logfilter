package me.suwash.tools.logfilter.ap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.test.DefaultTestWatcher;
import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeInput;
import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeOutput;
import me.suwash.tools.logfilter.infra.Config;
import me.suwash.tools.logfilter.infra.Const;
import me.suwash.tools.logfilter.infra.Context;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;

import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class LogFilterFacadeTest {

    // テストディレクトリ
    private static final String testDirPath = "src/test/scripts/logfilter";
    // 入力ディレクトリ
    private static final String inputDirPath = testDirPath + "/input";
    // 出力ディレクトリ
    private static final String outputDirPath = testDirPath + "/actual/facade";

    /** テスト対象。 */
    private LogFilterFacade facade = new LogFilterFacade();

    @Rule
    public DefaultTestWatcher watcher = new DefaultTestWatcher();

    @Test
    public void test_validate() {
        // config == null
        LogFilterFacadeInput input = new LogFilterFacadeInput();
        try {
            facade.execute(input);
            fail("validationエラーが発生すること");

        } catch (LogFilterException e) {
            assertEquals("validationエラーが発生すること", "error.validate", e.getMessageId());
            printViolations(e);
        }

        // config.* == null
        Config config = new Config();
        input.setConfig(config);
        try {
            facade.execute(input);
            fail("validationエラーが発生すること");

        } catch (LogFilterException e) {
            assertEquals("validationエラーが発生すること", "error.validate", e.getMessageId());
            printViolations(e);
        }

        config.setInputFilePath("/not/exist/input");
        config.setInputCharset("NotExist");
        config.setOutputCharset("UnSupportedCharset");
        try {
            facade.execute(input);
            fail("validationエラーが発生すること");

        } catch (LogFilterException e) {
            assertEquals("validationエラーが発生すること", "error.validate", e.getMessageId());
            printViolations(e);
        }
    }

    private void printViolations(LogFilterException e) {
        log.debug(e.getMessage() + "\n" + Context.getInstance().getErrors());
    }

    @Test
    public void test_execute() {
        String targetFilename = "apache_access.log";
        String inputFilePath = inputDirPath + "/" + targetFilename;
        String outputFilePath = outputDirPath + "/" + targetFilename;

        final long inputRowCount = 5;
        final long outputRowCount = 1;

        // from=to の場合、ミリ秒が完全一致するログイベントのみを出力すること
        final String timeFilterValueFrom = "2000-10-10 13:57:00.000";
        final String timeFilterValueTo = "2000-10-10 13:58:00.000";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        final String stringContentFilterValue = "";
        final String regexFilterValue = "";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        LogFilterFacadeInput input = new LogFilterFacadeInput();
        Config config = new Config();

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePath);
        config.setInputCharset(Const.CHARSET_DEFAULT_CONFIG);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePath);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

        input.setConfig(config);

        // --------------------------------------------------------------------------------
        //  Facade実行結果の期待値
        // --------------------------------------------------------------------------------
        // 処理ステータス
        ProcessStatus expectedStatus = ProcessStatus.Success;
        // 入力ファイル件数＝対象ファイル総数
        long expectedInputCount = inputRowCount;
        // 出力ファイル件数＝対象ファイル総数
        long expectedOutputCount = outputRowCount;

        // ==================================================
        //  実行
        // ==================================================
        // Facade呼出し
        LogFilterFacadeOutput outDto = facade.execute(input);

        // ==================================================
        //  確認
        // ==================================================
        // --------------------------------------------------------------------------------
        // Facade実行結果
        // --------------------------------------------------------------------------------
        assertEquals("実行結果を返すこと。", expectedStatus, outDto.getProcessStatus());
        assertEquals("指定した設定が取り出せること。", config, outDto.getInput().getConfig());
        assertEquals("入力ファイルの行数を返すこと。", expectedInputCount, outDto.getInputRowCount());
        assertEquals("出力ファイルの行数を返すこと。", expectedOutputCount, outDto.getOutputRowCount());


        // 再実行で、同一パスを上書き
        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePath);
        config.setInputCharset(Const.CHARSET_DEFAULT_CONFIG);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePath);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

        input.setConfig(config);

        // ==================================================
        //  実行
        // ==================================================
        // Facade呼出し
        outDto = facade.execute(input);

        // ==================================================
        //  確認
        // ==================================================
        // --------------------------------------------------------------------------------
        // Facade実行結果
        // --------------------------------------------------------------------------------
        assertEquals("実行結果を返すこと。", expectedStatus, outDto.getProcessStatus());
        assertEquals("指定した設定が取り出せること。", config, outDto.getInput().getConfig());
        assertEquals("入力ファイルの行数を返すこと。", expectedInputCount, outDto.getInputRowCount());
        assertEquals("出力ファイルの行数を返すこと。", expectedOutputCount, outDto.getOutputRowCount());
    }

    @Test
    public void test_execute_error() {
    }

}
