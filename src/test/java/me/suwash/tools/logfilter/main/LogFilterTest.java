package me.suwash.tools.logfilter.main;

import static me.suwash.tools.logfilter.infra.Const.CHARSET_DEFAULT_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeOutput;
import me.suwash.tools.logfilter.infra.Config;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

public class LogFilterTest {

    // テストディレクトリ
    private static final String testDir = "src/test/scripts/logfilter";
    // 入力ディレクトリ
    private static final String inputDirPathStr = testDir + "/input";
    // 出力ディレクトリ
    private static final String outputDirPathStr = testDir + "/actual";

    @Test
    public void testExecuteFacade_TimeFilter_FromTo() {

        final String inputFilePathStr = inputDirPathStr + "/sample.log";
        final String outputFilePathStr = outputDirPathStr + "/sample.log_time_from-to";

        final long inputRowCount = 59;
        final long outputRowCount = 1;

        // from=to の場合、ミリ秒が完全一致するログイベントのみを出力すること
        final String timeFilterValueFrom = "2016-11-06 12:17:53.999";
        final String timeFilterValueTo = "2016-11-06 12:17:53.999";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        final String stringContentFilterValue = "";
        final String regexFilterValue = "";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        Config config = null;
        ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            config = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePathStr);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePathStr);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

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
        LogFilterFacadeOutput outDto = LogFilter.executeFacade(config);

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
    public void testExecuteFacade_TimeFilter_From() {

        final String inputFilePathStr = inputDirPathStr + "/sample.log";
        final String outputFilePathStr = outputDirPathStr + "/sample.log_time_from";

        final long inputRowCount = 59;
        final long outputRowCount = 8;

        // fromとミリ秒が一致するログイベントを出力すること
        final String timeFilterValueFrom = "2016-11-06 12:17:54.022";
        final String timeFilterValueTo = "";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        final String stringContentFilterValue = "";
        final String regexFilterValue = "";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        Config config = null;
        ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            config = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePathStr);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePathStr);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

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
        LogFilterFacadeOutput outDto = LogFilter.executeFacade(config);

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
    public void testExecuteFacade_TimeFilter_To() {

        final String inputFilePathStr = inputDirPathStr + "/sample.log";
        final String outputFilePathStr = outputDirPathStr + "/sample.log_time_to";

        final long inputRowCount = 59;
        final long outputRowCount = 7;

        final String timeFilterValueFrom = "";
        // toとミリ秒が一致するログイベントを出力しないこと
        final String timeFilterValueTo = "2016-11-06 12:17:52.914";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        final String stringContentFilterValue = "";
        final String regexFilterValue = "";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        Config config = null;
        ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            config = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePathStr);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePathStr);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

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
        LogFilterFacadeOutput outDto = LogFilter.executeFacade(config);

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
    public void testExecuteFacade_TimeFilter_FromTo_Error() {

        final String inputFilePathStr = inputDirPathStr + "/sample.log";
        final String outputFilePathStr = outputDirPathStr + "/sample.log_time_error";

        final String timeFilterValueFrom = "2016-11-06 12:17:53.999";
        final String timeFilterValueTo = "2016-11-06 12:17:53.998";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        final String stringContentFilterValue = "";
        final String regexFilterValue = "";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        Config config = null;
        ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            config = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePathStr);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePathStr);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

        // --------------------------------------------------------------------------------
        //  Facade実行結果の期待値
        // --------------------------------------------------------------------------------
        // エラーメッセージ
        String expect = "LogFilterService.timeFilter.invalidRange";

        // ==================================================
        //  実行＆確認
        // ==================================================
        // Facade呼出し
        try {
            LogFilter.executeFacade(config);
            fail();

        } catch (LogFilterException e) {
            assertEquals("日付設定エラーを返すこと", expect, e.getMessageId());
        }
    }


    @Test
    public void testExecuteFacade_LogLevelFilter() {

        final String inputFilePathStr = inputDirPathStr + "/sample.log";
        final String outputFilePathStr = outputDirPathStr + "/sample.log_loglevel";

        final long inputRowCount = 59;
        final long outputRowCount = 13;

        final String timeFilterValueFrom = "";
        final String timeFilterValueTo = "";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        logLevelFilterValueList.add("WARN");
        logLevelFilterValueList.add("ERROR");
        final String stringContentFilterValue = "";
        final String regexFilterValue = "";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        Config config = null;
        ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            config = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePathStr);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePathStr);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

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
        LogFilterFacadeOutput outDto = LogFilter.executeFacade(config);

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
    public void testExecuteFacade_StringContentFilter() {

        final String inputFilePathStr = inputDirPathStr + "/sample.log";
        final String outputFilePathStr = outputDirPathStr + "/sample.log_stringContent";

        final long inputRowCount = 59;
        final long outputRowCount = 5;

        final String timeFilterValueFrom = "";
        final String timeFilterValueTo = "";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        final String stringContentFilterValue = "メモリ情報";
        final String regexFilterValue = "";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        Config config = null;
        ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            config = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePathStr);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePathStr);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

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
        LogFilterFacadeOutput outDto = LogFilter.executeFacade(config);

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
    public void testExecuteFacade_RegexFilter() {

        final String inputFilePathStr = inputDirPathStr + "/sample.log";
        final String outputFilePathStr = outputDirPathStr + "/sample.log_regex";

        final long inputRowCount = 59;
        final long outputRowCount = 2;

        final String timeFilterValueFrom = "";
        final String timeFilterValueTo = "";
        List<String> logLevelFilterValueList = new ArrayList<String>();
        final String stringContentFilterValue = "";
        final String regexFilterValue = ".*Scceeded\\.$";

        // --------------------------------------------------------------------------------
        //  設定オブジェクトの作成
        // --------------------------------------------------------------------------------
        Config config = null;
        ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            config = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 入力ディレクトリを上書き
        config.setInputFilePath(inputFilePathStr);
        // 出力ディレクトリを上書き
        config.setOutputFilePath(outputFilePathStr);
        // TimeFilter
        config.setTimeFilterValueFrom(timeFilterValueFrom);
        config.setTimeFilterValueTo(timeFilterValueTo);
        // LogLevelFilter
        config.setLogLevelFilterValueList(logLevelFilterValueList);
        // StringContentFilter
        config.setStringContentFilterValue(stringContentFilterValue);
        // RegexFilter
        config.setRegexFilterValue(regexFilterValue);

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
        LogFilterFacadeOutput outDto = LogFilter.executeFacade(config);

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
}
