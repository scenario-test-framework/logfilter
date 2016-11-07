package me.suwash.tools.logfilter.ap;

import me.suwash.ddd.policy.Input;
import me.suwash.ddd.policy.Output;
import me.suwash.ddd.policy.layer.ap.Facade;
import me.suwash.tools.logfilter.infra.policy.BaseLayerSuperType;

/**
 * アプリケーション層のLayerSuperType基底クラス。
 *
 * @param <I> 入力データモデル
 * @param <O> 出力データモデル
 */
public abstract class BaseFacade<I extends Input, O extends Output<I>> extends BaseLayerSuperType<I, O> implements Facade<I, O> {

}
