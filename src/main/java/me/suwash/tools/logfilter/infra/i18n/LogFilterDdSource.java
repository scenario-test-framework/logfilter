package me.suwash.tools.logfilter.infra.i18n;

import me.suwash.ddd.i18n.DddDdSource;
import me.suwash.util.i18n.DdSource;

/**
 * データディクショナリ用プロパティファイルの定義保持クラス。
 */
public class LogFilterDdSource extends DdSource {
    private static LogFilterDdSource instance = new LogFilterDdSource();

    /**
     * Singletonパターンでインスタンスを返します。
     *
     * @return インスタンス
     */
    public static LogFilterDdSource getInstance() {
        return instance;
    }

    /*
     * (非 Javadoc)
     * @see me.suwash.util.i18n.DdSource#getParent()
     */
    @Override
    protected DdSource getParent() {
        return DddDdSource.getInstance();
    }
}
