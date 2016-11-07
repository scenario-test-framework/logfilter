package me.suwash.tools.logfilter.infra.policy;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.ddd.policy.GenericLayerSuperType;
import me.suwash.ddd.policy.Input;
import me.suwash.ddd.policy.LayerSuperType;
import me.suwash.ddd.policy.Output;
import me.suwash.tools.logfilter.infra.Const;
import me.suwash.tools.logfilter.infra.Context;
import me.suwash.tools.logfilter.infra.exception.Errors;
import me.suwash.tools.logfilter.infra.exception.LogFilterException;

/**
 * CUIツール向けLayerSuperTypeパターン基底クラス。
 * validationエラー発生時は、Contextに保存して、例外をthrowします。
 *
 * @param <I> Input
 * @param <O> Output
 */
@lombok.extern.slf4j.Slf4j
public abstract class BaseLayerSuperType<I extends Input, O extends Output<I>> extends GenericLayerSuperType<I, O> implements LayerSuperType<I, O> {

    /* (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#execute(me.suwash.ddd.policy.Input)
     */
    @Override
    public O execute(final I input) {
        // validate
        log.debug("START validate");
        final Set<ConstraintViolation<I>> violations = validate(input);
        log.debug("END   validate");
        // エラー時は、Contextに保存して、例外をthrow
        if (! violations.isEmpty()) {
            final Errors errors = new Errors();
            errors.addViolations(violations);
            final Context context = Context.getInstance();
            context.setErrors(errors);
            throw new LogFilterException("error.validate");
        }

        // preExecute
        log.debug("START preExecute");
        final O preExecuteOutput = preExecute(input);
        log.debug("END   preExecute");
        if (ProcessStatus.Failure.equals(preExecuteOutput.getProcessStatus())) {
            return preExecuteOutput;
        }

        // main
        log.debug("START mainExecute");
        final O mainExecuteOutput = mainExecute(input, preExecuteOutput);
        log.debug("END   mainExecute");
        if (ProcessStatus.Failure.equals(mainExecuteOutput.getProcessStatus())) {
            return mainExecuteOutput;
        }

        // postExecute
        log.debug("START postExecute");
        final O postExecuteOutput = postExecute(input, mainExecuteOutput);
        log.debug("END   postExecute");
        return postExecuteOutput;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.ddd.policy.LayerSuperType#validate(me.suwash.ddd.policy.Input)
     */
    @Override
    public Set<ConstraintViolation<I>> validate(final I input) {
        final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        return validator.validate(input);
    }

    /* (非 Javadoc)
     * @see me.suwash.ddd.policy.GenericLayerSuperType#getOutput(me.suwash.ddd.policy.Input)
     */
    @Override
    protected O getOutput(final I input) {
        // validationエラー発生時は、Context保存するので、不使用。
        throw new LogFilterException(Const.UNSUPPORTED);
    }

}