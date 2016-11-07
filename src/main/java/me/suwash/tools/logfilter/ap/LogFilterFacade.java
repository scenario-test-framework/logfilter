package me.suwash.tools.logfilter.ap;

import java.io.File;

import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeInput;
import me.suwash.tools.logfilter.ap.dto.LogFilterFacadeOutput;
import me.suwash.tools.logfilter.infra.Const;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;
import me.suwash.tools.logfilter.sv.LogFilterService;
import me.suwash.tools.logfilter.sv.dto.LogFilterServiceInput;
import me.suwash.tools.logfilter.sv.dto.LogFilterServiceOutput;

import org.apache.commons.lang3.StringUtils;

/**
 * ファイルの実行制御。
 */
public class LogFilterFacade extends BaseFacade<LogFilterFacadeInput, LogFilterFacadeOutput> {

    /** フィルタサービス。 */
    private final transient LogFilterService service;

    /**
     * コンストラクタ。
     */
    public LogFilterFacade() {
        super();
        service = new LogFilterService();
    }

    /* (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#preExecute(me.suwash.ddd.policy.Input)
     */
    @Override
    protected LogFilterFacadeOutput preExecute(final LogFilterFacadeInput input) {
        // ----------------------------------------
        // 関連チェック
        // ----------------------------------------
        // 出力ファイルパス
        final String outputFilePath = input.getOutputFilePath();
        final File outputFile = new File(outputFilePath);
        // すでに存在する場合、削除
        if (outputFile.exists() && !outputFile.delete()) {
            throw new LogFilterException(Const.FILE_CANTDELETE, new Object[] { outputFile });
        }
        // 親ディレクトリが存在しない場合、作成
        final File outputDirFile = outputFile.getParentFile();
        if (!outputDirFile.isDirectory() && !outputDirFile.mkdirs()) {
            throw new LogFilterException(Const.DIR_CANTCREATE, new Object[] { outputDirFile });
        }

        // 出力文字コード
        final String outputCharset = input.getOutputCharset();
        if (StringUtils.isEmpty(outputCharset)) {
            // 未設定の場合、入力文字コードと同一値を設定
            input.setOutputCharset(input.getInputCharset());
        }

        // ----------------------------------------
        // 出力データモデルの初期化
        // ----------------------------------------
        final LogFilterFacadeOutput output = new LogFilterFacadeOutput();
        output.setInput(input);
        return output;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#mainExecute(me.suwash.ddd.policy.Input, me.suwash.ddd.policy.Output)
     */
    @Override
    protected LogFilterFacadeOutput mainExecute(final LogFilterFacadeInput input, final LogFilterFacadeOutput preExecuteOutput) {
        // ------------------------------
        // 設定
        // ------------------------------
        final LogFilterServiceInput inBean = new LogFilterServiceInput();
        inBean.setInputFilePath(input.getInputFilePath());
        inBean.setInputCharset(input.getInputCharset());
        inBean.setOutputFilePath(input.getOutputFilePath());
        inBean.setOutputCharset(input.getOutputCharset());
        inBean.setTimeFilterValueFrom(input.getTimeFilterValueFrom());
        inBean.setTimeFilterValueTo(input.getTimeFilterValueTo());
        inBean.setLogLevelFilterValueList(input.getLogLevelFilterValueList());
        inBean.setStringContentFilterValue(input.getStringContentFilterValue());
        inBean.setRegexFilterValue(input.getRegexFilterValue());

        // ------------------------------
        // サービス呼出し
        // ------------------------------
        final LogFilterServiceOutput outBean = service.execute(inBean);

        // ------------------------------
        // 出力データ作成
        // ------------------------------
        final LogFilterFacadeOutput outDto = new LogFilterFacadeOutput();
        // 入力データ
        outDto.setInput(input);
        // ステータス
        outDto.setProcessStatus(outBean.getProcessStatus());
        // 入力行数
        outDto.setInputRowCount(outBean.getInputRowCount());
        // 出力行数
        outDto.setOutputRowCount(outBean.getOutputRowCount());

        return outDto;
    }

}
