package me.suwash.tools.logfilter.infra;

import lombok.Getter;
import me.suwash.ddd.classification.ProcessStatus;
import me.suwash.tools.logfilter.infra.exception.Errors;

/**
 * システムコンテキスト。
 */
public final class Context {

    /** Singletonパターン。 */
    private static final Context instance = new Context();

    /** システムの処理ステータス。 */
    @Getter
    private ProcessStatus processStatus = ProcessStatus.Processing;

    /** チェックエラー。 */
    @Getter
    private Errors errors;

    /**
     * Singletonパターンでインスタンスを返します。
     *
     * @return インスタンス
     */
    public static Context getInstance() {
        return instance;
    }

    /**
     * コンストラクタ。
     */
    private Context() {}

    /**
     * チェックエラーを設定します。
     *
     * @param errors チェックエラー
     */
    public void setErrors(final Errors errors) {
        // チェックエラーを含む場合
        if (errors != null && errors.size() != 0) {
            // エラー内容を保持
            this.errors = errors;
            // 処理ステータスを失敗に設定
            this.processStatus = ProcessStatus.Failure;
        }
    }
}
