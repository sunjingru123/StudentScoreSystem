package com.student.studentscoresystem.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 班级名称规范化。
 *
 * 历史数据里同一个班级可能存在两种写法：
 *
 *   25级1班   ←→   2025级1班
 *
 * 为了界面、筛选、导出保持一致，系统统一使用四位年份：
 *
 *   展示 / 保存：2025级1班
 *   兼容查询   ：仍可命中历史短写法（25级1班）
 *
 * 只处理「两位年级」开头的班级名称，其它格式原样返回，
 * 不会误改「2025级1班」「研究生1班」这类已经规范的名称。
 */
public final class ClassNameUtils {

    /**
     * 短写法：25级1班
     */
    private static final Pattern SHORT_FORM =
            Pattern.compile("^(\\d{2})级(.*)$");

    /**
     * 规范写法：2025级1班
     */
    private static final Pattern LONG_FORM =
            Pattern.compile("^(20\\d{2})级(.*)$");

    /**
     * 短写法可接受的年份范围（10 ~ 49 对应 2010 ~ 2049），
     * 避免把「99级」之类的历史写法误改成 2099。
     */
    private static final int MIN_SHORT_YEAR = 10;

    private static final int MAX_SHORT_YEAR = 49;

    private ClassNameUtils() {
    }

    /**
     * 规范成四位年份写法。
     *
     * 25级1班 → 2025级1班
     * 2025级1班 → 2025级1班
     */
    public static String normalize(String className) {

        if (className == null) {

            return null;
        }

        String value = className.trim();

        if (value.isEmpty()) {

            return value;
        }

        /* 已经是四位年份，保持不变 */
        if (LONG_FORM.matcher(value).matches()) {

            return value;
        }

        Matcher matcher = SHORT_FORM.matcher(value);

        if (!matcher.matches()) {

            return value;
        }

        int shortYear = Integer.parseInt(matcher.group(1));

        if (shortYear < MIN_SHORT_YEAR
                || shortYear > MAX_SHORT_YEAR) {

            return value;
        }

        return "20" + matcher.group(1) + "级" + matcher.group(2);
    }

    /**
     * 由规范写法推导历史短写法，用于兼容旧数据查询。
     *
     * 2025级1班 → 25级1班
     *
     * @return 无法推导时返回 null
     */
    public static String legacyShort(String className) {

        String normalized = normalize(className);

        if (normalized == null || normalized.isEmpty()) {

            return null;
        }

        Matcher matcher = LONG_FORM.matcher(normalized);

        if (!matcher.matches()) {

            return null;
        }

        return normalized.substring(2);
    }

    /**
     * 生成用于 LIKE 查询的关键字。
     *
     * LIKE '%25级1班%' 同时能命中
     * 「25级1班」与「2025级1班」两种历史写法，
     * 因此规范班级名统一转成短写法再查询。
     *
     * 其它输入（例如只输入「1班」）原样返回。
     */
    public static String searchKeyword(String input) {

        if (input == null) {

            return null;
        }

        String normalized = normalize(input);

        String shortForm = legacyShort(normalized);

        return shortForm != null
                ? shortForm
                : normalized;
    }
}
