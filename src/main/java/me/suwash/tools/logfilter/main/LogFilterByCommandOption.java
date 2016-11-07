package me.suwash.tools.logfilter.main;

import static me.suwash.tools.logfilter.infra.Const.CHARSET_DEFAULT_CONFIG;
import static me.suwash.tools.logfilter.infra.Const.EXITCODE_ERROR;
import static me.suwash.tools.logfilter.infra.Const.EXITCODE_SUCCESS;
import static me.suwash.tools.logfilter.infra.Const.EXITCODE_WARN;
import static me.suwash.tools.logfilter.infra.Const.USERINPUT_YES;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeOutput;
import me.suwash.tools.logfilter.infra.Config;
import me.suwash.tools.logfilter.infra.Const;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;
import me.suwash.tools.logfilter.infra.i18n.LogFilterMessageSource;
import me.suwash.tools.logfilter.main.dto.CommandOption;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.beust.jcommander.JCommander;

/**
 * パラメータ指定のCUIバウンダリ。
 */
@lombok.extern.slf4j.Slf4j
public final class LogFilterByCommandOption {

    /** 期待する引数の数：最小値。 */
    private static final int ARG_LENGTH_MIN = 1;

    /** 期待する引数の数：最大値。 */
    private static final int ARG_LENGTH_MAX = 2;

    /**
     * コンストラクタ。
     */
    private LogFilterByCommandOption() {}

    /**
     * コマンドライン引数から設定を判断し、フィルタを実行します。
     *
     * @param args コマンドライン引数
     */
    public static void main(final String... args) {
        // ==================================================
        // 事前処理
        // ==================================================
        // --------------------------------------------------
        // 引数のパース
        // --------------------------------------------------
        final CommandOption option = new CommandOption();
        new JCommander(option, args);

        // 引数表示
        if (log.isTraceEnabled()) {
            // コマンドライン引数
            log.trace("・コマンドライン引数");
            for (int i = 0; i < args.length; i++) {
                log.trace("    ・" + i + ":" + args[i]);
            }
            // parse結果
            log.trace("・parse結果");
            log.trace("    " + option.toString());
            // 処理を一時停止
            System.out.println("Do you want to execute logfilter process ?  y|n");
            final Scanner scanner = new Scanner(System.in, System.getProperty("file.encoding"));
            final String input = scanner.next();
            // 入力値を確認
            if (!USERINPUT_YES.equals(input.toLowerCase(Locale.getDefault()))) {
                // Y or y 以外の場合、処理をキャンセル
                scanner.close();
                log.info("canceled.");
                System.exit(EXITCODE_SUCCESS);
            }
        }

        // オプション指定なしの引数リスト
        final List<String> paramList = option.getParameters();

        // --------------------------------------------------
        // 入力チェック
        // --------------------------------------------------
        // 引数の数
        if (paramList.size() < ARG_LENGTH_MIN || ARG_LENGTH_MAX < paramList.size()) {
            log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_ERROR_ARG));
            System.exit(EXITCODE_ERROR);
        }

        // ==================================================
        // 本処理
        // ==================================================
        // --------------------------------------------------
        // 設定ファイルの読み込み
        // --------------------------------------------------
        Config config = null;
        Config defaultConfig = null;
        final ObjectMapper mapper = new ObjectMapper();

        // デフォルト設定の読み込み
        try {
            final InputStream inStream = LogFilter.class.getResourceAsStream("/" + LogFilter.getConfigFileName());
            final Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            defaultConfig = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_ERROR_PARSE, new Object[] { "defaultConfig" }), e);
            System.exit(EXITCODE_ERROR);
        }

        // 設定ファイルパスの指定を確認
        if (StringUtils.isEmpty(option.getConfigFilePath())) {
            // 設定ファイルパスが指定されていない場合、デフォルト設定を利用
            config = defaultConfig;

        } else {
            // 設定ファイルパスが指定されている場合、カスタム設定の読み込み
            try {
                config = mapper.readValue(new File(escapeQuote(option.getConfigFilePath())), Config.class);
                config.setDefault(defaultConfig);

            } catch (Exception e) {
                log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_ERROR_PARSE, new Object[] { option.getConfigFilePath() }), e);
                System.exit(EXITCODE_ERROR);
            }
        }

        // --------------------------------------------------
        // 設定の上書き
        // --------------------------------------------------
        // 入力文字コード
        if (!StringUtils.isEmpty(option.getInputCharset())) {
            config.setInputCharset(escapeQuote(option.getInputCharset()));
        }
        // 出力文字コード
        if (!StringUtils.isEmpty(option.getOutputCharset())) {
            config.setOutputCharset(escapeQuote(option.getOutputCharset()));
        }
        // TimeFilter設定値: From
        if (!StringUtils.isEmpty(option.getTimeFilterFrom())) {
            config.setTimeFilterValueFrom(escapeQuote(option.getTimeFilterFrom()));
        }
        // TimeFilter設定値： To
        if (!StringUtils.isEmpty(option.getTimeFilterTo())) {
            config.setTimeFilterValueTo(escapeQuote(option.getTimeFilterTo()));
        }
        // LogLevelFilter設定値
        if (!StringUtils.isEmpty(option.getLogLevelFilter())) {
            final String[] logLevels = escapeQuote(option.getLogLevelFilter()).split(",");
            config.setLogLevelFilterValueList(Arrays.asList(logLevels));
        }
        // StringContentFilter設定値
        if (!StringUtils.isEmpty(option.getStringContentFilter())) {
            config.setStringContentFilterValue(escapeQuote(option.getStringContentFilter()));
        }
        // RegexFilter設定値
        if (!StringUtils.isEmpty(option.getRegexFilter())) {
            config.setRegexFilterValue(escapeQuote(option.getRegexFilter()));
        }

        // 入力ファイルパス
        config.setInputFilePath(escapeQuote(paramList.get(0)));

        // 出力ファイルパス
        if (paramList.size() == ARG_LENGTH_MAX) {
            config.setOutputFilePath(escapeQuote(paramList.get(1)));
        }

        // 強制上書きフラグの確認
        if (!option.isForceOverWrite()) {
            // 強制上書きしない場合、出力ファイルの存在確認
            final File outputFile = new File(config.getOutputFilePath());
            if (outputFile.exists()) {
                // ユーザ入力を待つ
                System.out.println("output file: " + outputFile + " is already exists. overwrite it ? y|n");
                final Scanner scanner = new Scanner(System.in, System.getProperty("file.encoding"));
                final String input = scanner.next();

                // 入力値を確認
                if (!USERINPUT_YES.equals(input.toLowerCase(Locale.getDefault()))) {
                    // Y or y 以外の場合、処理をキャンセル
                    scanner.close();
                    log.info("canceled.");
                    System.exit(EXITCODE_SUCCESS);
                }
            }
        }

        // --------------------------------------------------
        // Facade呼出し
        // --------------------------------------------------
        LogFilterFacadeOutput outDto = null;
        try {
            outDto = LogFilter.executeFacade(config);
        } catch (LogFilterException e) {
            LogFilter.loggingException(e);
            System.exit(EXITCODE_ERROR);

        } catch (Exception e) {
            log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_ERROR), e);
            System.exit(EXITCODE_ERROR);
        }

        // ==================================================
        // 事後処理
        // ==================================================
        // --------------------------------------------------
        // Facadeの結果判定
        // --------------------------------------------------
        switch (outDto.getProcessStatus()) {
            case Success:
                LogFilter.loggingFacadeOutput(outDto);
                log.info(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_SUCCESS));
                System.exit(EXITCODE_SUCCESS);
                break;

            case Warning:
                LogFilter.loggingFacadeOutput(outDto);
                log.warn(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_WARN));
                System.exit(EXITCODE_WARN);
                break;

            case Failure:
                LogFilter.loggingFacadeOutput(outDto);
                log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_FAIL));
                System.exit(EXITCODE_ERROR);
                break;

            default:
                log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_ERROR));
                System.exit(EXITCODE_ERROR);
                break;
        }
    }

    /**
     * クォート、ダブルクォートで括られた文字列から、括り文字を除去します。
     *
     * @param string クォート、ダブルクォートで括られた文字列
     * @return 括り文字を除去した文字列
     */
    private static String escapeQuote(final String string) {
        if (string.charAt(0) == '\'' && string.charAt(string.length() - 1) == '\'' ||
            string.charAt(0) == '\"' && string.charAt(string.length() - 1) == '\"') {
            return string.substring(1, string.length() - 1);

        } else {
            return string;
        }
    }
}
