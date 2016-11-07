package me.suwash.tools.logfilter.sv;

import me.suwash.ddd.policy.Input;
import me.suwash.ddd.policy.Output;
import me.suwash.ddd.policy.layer.sv.Service;
import me.suwash.tools.logfilter.infra.policy.BaseLayerSuperType;

/**
 * サービス層のLayerSuperType基底クラス。
 *
 * @param <I> 入力データモデル
 * @param <O> 出力データモデル
 */
public abstract class BaseService<I extends Input, O extends Output<I>> extends BaseLayerSuperType<I, O> implements Service<I, O> {

}
