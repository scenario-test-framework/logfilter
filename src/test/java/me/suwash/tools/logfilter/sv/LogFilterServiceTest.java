package me.suwash.tools.logfilter.sv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.test.DefaultTestWatcher;
import me.suwash.tools.logfilter.infra.Const;
import me.suwash.tools.logfilter.infra.Context;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;
import me.suwash.tools.logfilter.sv.dto.LogFilterServiceInput;
import me.suwash.tools.logfilter.sv.dto.LogFilterServiceOutput;
import me.suwash.util.FileUtils;
import me.suwash.util.constant.UtilMessageConst;

import org.junit.Rule;
import org.junit.Test;

@lombok.extern.slf4j.Slf4j
public class LogFilterServiceTest {

    // テストディレクトリ
    private static final String testDirPath = "src/test/scripts/logfilter";
    // 入力ディレクトリ
    private static final String inputDirPath = testDirPath + "/input";
    // 出力ディレクトリ
    private static final String outputDirPath = testDirPath + "/actual/service";

    /** テスト対象。 */
    private LogFilterService service = new LogFilterService();

    @Rule
    public DefaultTestWatcher watcher = new DefaultTestWatcher();

    @Test
    public void test_validate() {
        LogFilterServiceInput input = new LogFilterServiceInput();

        // null
        try {
            service.execute(input);
            fail("validationエラーが発生すること");

        } catch (LogFilterException e) {
            assertEquals("validationエラーが発生すること", "error.validate", e.getMessageId());
            printViolations(e);
        }

        // 存在しないパス
        input.setInputFilePath("/path/to/input");
        // 存在しない文字コード
        input.setInputCharset("NotExist");
        // 存在しない文字コード
        input.setOutputCharset("NotExist2");
        // 変換できない日時
        input.setTimeFilterValueFrom("unmatched date format");
        // 変換できない日時
        input.setTimeFilterValueTo("9999-13-01 00:00:00.000");
        try {
            service.execute(input);
            fail();
        } catch (LogFilterException e) {
            printViolations(e);
        }
    }

    @Test
    public void test_execute() {
        //------------------------------
        // 準備
        //------------------------------
        String targetFilename = "apache_access.log";
        String inputFilePath = inputDirPath + "/" + targetFilename;
        String outputFilePath = outputDirPath + "/" + targetFilename;

        long inputRowCount = 5;
        long outputRowCount = 3;

        LogFilterServiceInput input = new LogFilterServiceInput();
        input.setInputFilePath(inputFilePath);
        input.setInputCharset(Const.CHARSET_DEFAULT_CONFIG);
        input.setOutputFilePath(outputFilePath);
        input.setOutputCharset(Const.CHARSET_DEFAULT_CONFIG);
        input.setTimeFilterValueFrom("2000-10-10 13:57:00.000");

        //------------------------------
        // 期待値
        //------------------------------
        // 処理ステータス
        ProcessStatus expectedStatus = ProcessStatus.Success;
        // 入力ファイル件数＝対象ファイル総数
        long expectedInputCount = inputRowCount;
        // 出力ファイル件数＝対象ファイル総数
        long expectedOutputCount = outputRowCount;

        //------------------------------
        // 実行 - ディレクトリなし
        //------------------------------
        FileUtils.rmdirs(outputDirPath);
        try {
            service.execute(input);
            fail("ファイル書き出しエラーが発生すること");
        } catch (LogFilterException e) {
            assertEquals("ファイル書き出しエラーが発生すること", UtilMessageConst.FILE_CANTWRITE, e.getMessageId());
            log.debug(e.getMessage());
        }

        //------------------------------
        // 実行 - ディレクトリあり
        //------------------------------
        FileUtils.mkdirs(outputDirPath);
        LogFilterServiceOutput output = service.execute(input);
        assertEquals("実行結果を返すこと。", expectedStatus, output.getProcessStatus());
        assertEquals("指定した設定が取り出せること。", input, output.getInput());
        assertEquals("入力ファイルの行数を返すこと。", expectedInputCount, output.getInputRowCount());
        assertEquals("出力ファイルの行数を返すこと。", expectedOutputCount, output.getOutputRowCount());
    }

    @Test
    public void test_execute_unsupported() {
        //------------------------------
        // 準備
        //------------------------------
        String targetFilename = "unsupported.log";
        String inputFilePath = inputDirPath + "/" + targetFilename;
        String outputFilePath = outputDirPath + "/" + targetFilename;

        LogFilterServiceInput input = new LogFilterServiceInput();
        input.setInputFilePath(inputFilePath);
        input.setInputCharset(Const.CHARSET_DEFAULT_CONFIG);
        input.setOutputFilePath(outputFilePath);
        input.setOutputCharset(Const.CHARSET_DEFAULT_CONFIG);
        input.setTimeFilterValueFrom("2000-10-10 13:57:00.000");

        //------------------------------
        // 実行
        //------------------------------
        FileUtils.mkdirs(outputDirPath);
        try {
            service.execute(input);
            fail("サポート外ログフォーマットのエラーが発生すること");
        } catch (LogFilterException e) {
            assertEquals("サポート外ログフォーマットのエラーが発生すること", "LogFilterService.unsupportedRecord", e.getMessageId());
            log.debug(e.getMessage());
        }

    }

    private void printViolations(LogFilterException e) {
        log.debug(e.getMessage() + "\n" + Context.getInstance().getErrors());
    }
}
