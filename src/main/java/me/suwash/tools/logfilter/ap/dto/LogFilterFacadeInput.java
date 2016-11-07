package me.suwash.tools.logfilter.ap.dto;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import lombok.Data;
import me.suwash.ddd.policy.Input;
import me.suwash.tools.logfilter.infra.Config;

/**
 * Facadeの入力データモデル。
 */
@Data
public class LogFilterFacadeInput implements Input {

    /** 設定。 */
    @NotNull
    @Valid
    private Config config;

    /**
     * 入力ファイルパスを返します。
     *
     * @return inputFilePath
     */
    public String getInputFilePath() {
        return config.getInputFilePath();
    }

    /**
     * 入力ファイルパスを設定します。
     *
     * @param inputFilePath セットする inputFilePath
     */
    public void setInputFilePath(final String inputFilePath) {
        config.setInputFilePath(inputFilePath);
    }

    /**
     * 入力ファイル文字コードを返します。
     *
     * @return inputCharset
     */
    public String getInputCharset() {
        return config.getInputCharset();
    }

    /**
     * 入力ファイル文字コードを設定します。
     *
     * @param inputCharset セットする inputCharset
     */
    public void setInputCharset(final String inputCharset) {
        config.setInputCharset(inputCharset);
    }

    /**
     * 出力ファイルパスを返します。
     *
     * @return outputFilePath
     */
    public String getOutputFilePath() {
        return config.getOutputFilePath();
    }

    /**
     * 出力ファイルパスを設定します。
     *
     * @param outputFilePath セットする outputFilePath
     */
    public void setOutputFilePath(final String outputFilePath) {
        config.setOutputFilePath(outputFilePath);
    }

    /**
     * 出力ファイル文字コードを返します。
     *
     * @return outputCharset
     */
    public String getOutputCharset() {
        return config.getOutputCharset();
    }

    /**
     * 出力ファイル文字コードを設定します。
     *
     * @param outputCharset セットする outputCharset
     */
    public void setOutputCharset(final String outputCharset) {
        config.setOutputCharset(outputCharset);
    }

    /**
     * TimeFilter設定値：抽出開始時刻を返します。
     *
     * @return timeFilterValueFrom
     */
    public String getTimeFilterValueFrom() {
        return config.getTimeFilterValueFrom();
    }

    /**
     * TimeFilter設定値：抽出開始時刻を設定します。
     *
     * @param timeFilterValueFrom セットする timeFilterValueFrom
     */
    public void setTimeFilterValueFrom(final String timeFilterValueFrom) {
        config.setTimeFilterValueFrom(timeFilterValueFrom);
    }

    /**
     * TimeFilter設定値：抽出終了時刻を返します。
     *
     * @return timeFilterValueTo
     */
    public String getTimeFilterValueTo() {
        return config.getTimeFilterValueTo();
    }

    /**
     * TimeFilter設定値：抽出終了時刻を設定します。
     *
     * @param timeFilterValueTo セットする timeFilterValueTo
     */
    public void setTimeFilterValueTo(final String timeFilterValueTo) {
        config.setTimeFilterValueTo(timeFilterValueTo);
    }

    /**
     * LogLevelFilter設定値リストを返します。
     *
     * @return logLevelFilterValueList
     */
    public List<String> getLogLevelFilterValueList() {
        return config.getLogLevelFilterValueList();
    }

    /**
     * LogLevelFilter設定値リストを設定します。
     *
     * @param logLevelFilterValueList セットする logLevelFilterValueList
     */
    public void setLogLevelFilterValueList(final List<String> logLevelFilterValueList) {
        config.setLogLevelFilterValueList(logLevelFilterValueList);
    }

    /**
     * StringContentFilter設定値を返します。
     *
     * @return stringContentFilterValue
     */
    public String getStringContentFilterValue() {
        return config.getStringContentFilterValue();
    }

    /**
     * StringContentFilter設定値を設定します。
     *
     * @param stringContentFilterValue セットする stringContentFilterValue
     */
    public void setStringContentFilterValue(final String stringContentFilterValue) {
        config.setStringContentFilterValue(stringContentFilterValue);
    }

    /**
     * RegexFilter設定値を返します。
     *
     * @return regexFilterValue
     */
    public String getRegexFilterValue() {
        return config.getRegexFilterValue();
    }

    /**
     * RegexFilter設定値を設定します。
     *
     * @param regexFilterValue セットする regexFilterValue
     */
    public void setRegexFilterValue(final String regexFilterValue) {
        config.setRegexFilterValue(regexFilterValue);
    }

}
