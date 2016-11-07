package me.suwash.tools.logfilter.main;

import static me.suwash.tools.logfilter.infra.Const.CHARSET_DEFAULT_CONFIG;
import static me.suwash.tools.logfilter.infra.Const.EXITCODE_ERROR;
import static me.suwash.tools.logfilter.infra.Const.EXITCODE_SUCCESS;
import static me.suwash.tools.logfilter.infra.Const.EXITCODE_WARN;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;

import me.suwash.tools.logfilter.ap.LogFilterFacade;
import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeInput;
import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeOutput;
import me.suwash.tools.logfilter.infra.Config;
import me.suwash.tools.logfilter.infra.Const;
import me.suwash.tools.logfilter.infra.Context;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;
import me.suwash.tools.logfilter.infra.i18n.LogFilterMessageSource;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * 設定ファイル指定のCUIバウンダリ。
 */
@lombok.extern.slf4j.Slf4j
public final class LogFilter {

    /** usageメッセージ。 */
    private static final String MSG_USAGE = "usage: " + LogFilter.class.getSimpleName() + " ${設定ファイルパス}";

    /** 期待する引数の数。 */
    private static final int ARG_LENGTH = 1;

    /** 単語区切り文字の開始可能なインデックス。 */
    private static final int DELIMITER_STARTABLE_POSITION = 1;

    /**
     * コンストラクタ。
     */
    private LogFilter() {}

    /**
     * 指定された設定で変換を実施します。
     *
     * @param args 0:設定ファイルパス
     */
    public static void main(final String... args) {
        // ==================================================
        // 事前処理
        // ==================================================
        // --------------------------------------------------
        // 入力チェック
        // --------------------------------------------------
        // 引数の数
        if (args.length != ARG_LENGTH) {
            log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_ERROR_ARG));
            log.error(MSG_USAGE);
            System.exit(EXITCODE_ERROR);
        }

        // 設定ファイルパス
        final String configFilePath = args[0];
        final File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            log.error(LogFilterMessageSource.getInstance().getMessage(Const.CHECK_NOTEXIST, new Object[] {configFile}));
            log.error(MSG_USAGE);
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
            final InputStream inStream = LogFilter.class.getResourceAsStream("/" + getConfigFileName());
            final Reader reader = new InputStreamReader(inStream, CHARSET_DEFAULT_CONFIG);
            defaultConfig = mapper.readValue(reader, Config.class);

        } catch (Exception e) {
            log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_ERROR_PARSE, new Object[] { "defaultConfig" }), e);
            System.exit(EXITCODE_ERROR);
        }

        // カスタム設定の読み込み
        try {
            config = mapper.readValue(new File(configFilePath), Config.class);

        } catch (Exception e) {
            log.error(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_ERROR_PARSE, new Object[] { configFilePath }), e);
            System.exit(EXITCODE_ERROR);
        }

        // デフォルト設定をマージ
        config.setDefault(defaultConfig);

        // --------------------------------------------------
        // Facade呼出し
        // --------------------------------------------------
        LogFilterFacadeOutput outDto = null;
        try {
            outDto = executeFacade(config);
        } catch (LogFilterException e) {
            loggingException(e);
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
                loggingFacadeOutput(outDto);
                log.info(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_SUCCESS));
                System.exit(EXITCODE_SUCCESS);
                break;

            case Warning:
                loggingFacadeOutput(outDto);
                log.warn(LogFilterMessageSource.getInstance().getMessage(Const.MSGCD_EXIT_WARN));
                System.exit(EXITCODE_WARN);
                break;

            case Failure:
                loggingFacadeOutput(outDto);
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
     * 指定された設定でFacadeを実行します。
     *
     * @param config 設定
     * @return Facade実行結果
     */
    static LogFilterFacadeOutput executeFacade(final Config config) {
        final LogFilterFacadeInput inDto = new LogFilterFacadeInput();
        inDto.setConfig(config);

        final LogFilterFacade facade = new LogFilterFacade();
        final LogFilterFacadeOutput outDto = facade.execute(inDto);;

        return outDto;
    }

    /**
     * 設定ファイル名を返します。
     *
     * @return 設定ファイル名
     */
    static String getConfigFileName() {
        final StringBuilder fileNameBuilder = new StringBuilder();

        // クラス名のUpperCamelCaseから、小文字の"_"区切りに置換
        final String className = LogFilter.class.getSimpleName();
        final String lowerCaseClassName = className.toLowerCase(Locale.getDefault());
        for (int pos = 0; pos < className.length(); pos++) {
            if (pos > DELIMITER_STARTABLE_POSITION && Character.isUpperCase(className.charAt(pos))) {
                fileNameBuilder.append('_');
            }
            fileNameBuilder.append(lowerCaseClassName.charAt(pos));
        }

        fileNameBuilder.append(".json");
        return fileNameBuilder.toString();
    }

    /**
     * Facadeの出力データモデルをログに出力します。
     *
     * @param dto Facadeの出力データモデル
     */
    static void loggingFacadeOutput(final LogFilterFacadeOutput dto) {
        log.info("--------------------------------------------------");
        log.info("・入出力行数");
        log.info("    ・入力：" + dto.getInputRowCount());
        log.info("    ・出力：" + dto.getOutputRowCount());
    }

    /**
     * Validateエラーとその他のエラーで、ログを出し分けます。
     *
     * @param exception LogFilterException
     */
    static void loggingException(LogFilterException exception) {
        if (Const.MSGCD_ERROR_VALIDATE.equals(exception.getMessageId())) {
            // Validateエラー用ログ出力
            log.error(exception.getMessage() + "\n" + Context.getInstance().getErrors().toString());
        } else {
            // その他エラー用ログ出力
            log.error(Const.ERRORHANDLE, new Object[] { "LogFilter", exception.getMessage() }, exception);
        }
    }

}
