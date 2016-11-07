package me.suwash.tools.logfilter.ap.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;

import lombok.Getter;
import lombok.Setter;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.ddd.policy.Output;

/**
 * Facadeの出力データモデル。
 */
public class LogFilterFacadeOutput implements Output<LogFilterFacadeInput> {

    /** 入力データ。 */
    @Setter
    private LogFilterFacadeInput input;

    /** 処理ステータス。 */
    @Setter
    private ProcessStatus processStatus = ProcessStatus.Processing;

    /** チェック結果。 */
    private Set<ConstraintViolation<LogFilterFacadeInput>> violationSet;

    /** 入力行数。 */
    @Getter
    @Setter
    private long inputRowCount;

    /** 出力行数。 */
    @Getter
    @Setter
    private long outputRowCount;

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getInput()
     */
    @Override
    public LogFilterFacadeInput getInput() {
        return input;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getProcessStatus()
     */
    @Override
    public ProcessStatus getProcessStatus() {
        return processStatus;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getViolationSet()
     */
    @Override
    public Set<ConstraintViolation<LogFilterFacadeInput>> getViolationSet() {
        return violationSet;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#setViolationSet(java.util.Set)
     */
    @Override
    public void setViolationSet(final Set<ConstraintViolation<LogFilterFacadeInput>> violationSet) {
        this.violationSet = violationSet;
    }

}
