package me.suwash.tools.logfilter.sv.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;

import lombok.Getter;
import lombok.Setter;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.ddd.policy.Output;

/**
 * フォーマット変換サービス出力データ。
 */
public class LogFilterServiceOutput implements Output<LogFilterServiceInput> {

    /** チェック結果。 */
    private Set<ConstraintViolation<LogFilterServiceInput>> violationSet;

    /** 処理ステータス。 */
    private ProcessStatus processStatus = ProcessStatus.Processing;

    /** 入力データ。 */
    @Setter
    private LogFilterServiceInput input;

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
    public LogFilterServiceInput getInput() {
        return input;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#getViolationSet()
     */
    @Override
    public Set<ConstraintViolation<LogFilterServiceInput>> getViolationSet() {
        return violationSet;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.Output#setViolationSet(java.util.Set)
     */
    @Override
    public void setViolationSet(Set<ConstraintViolation<LogFilterServiceInput>> violationSet) {
        this.violationSet = violationSet;
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
     * @see me.suwash.ddd.policy.Output#setProcessStatus(me.suwash.ddd.classification.ProcessStatus)
     */
    @Override
    public void setProcessStatus(ProcessStatus processStatus) {
        this.processStatus = processStatus;
    }

}
