package com.student.studentscoresystem.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;

/**
 * 姓名转拼音工具
 *
 * 用于「新增教师」时自动生成登录账号：
 * 账号 = 教师姓名的完整拼音，例如：
 *
 * 孙靖茹 -> sunjingru
 * 张三   -> zhangsan
 *
 * 规则：
 * 1. 全部小写；
 * 2. 去除声调；
 * 3. 非中文的英文字母、数字原样保留（转小写）；
 * 4. 空格、标点等无法转换的字符直接丢弃。
 */
public final class NamePinyinUtil {

    /**
     * 账号最大长度，避免超长姓名生成超长账号
     */
    private static final int MAX_LENGTH = 32;

    /**
     * 拼音输出格式：小写、无声调、ü 记为 v
     */
    private static final HanyuPinyinOutputFormat FORMAT = createFormat();

    private NamePinyinUtil() {
    }

    /**
     * 姓名转全拼账号。
     *
     * 无法转换（例如全为符号）时返回空字符串，
     * 由调用方决定回退策略。
     */
    public static String toAccount(String name) {

        if (name == null) {
            return "";
        }

        String trimmed = name.trim();

        if (trimmed.isEmpty()) {
            return "";
        }

        StringBuilder account = new StringBuilder(trimmed.length() * 4);

        for (char ch : trimmed.toCharArray()) {

            if (ch < 128 && Character.isLetterOrDigit(ch)) {
                account.append(Character.toLowerCase(ch));
                continue;
            }

            String pinyin = firstPinyin(ch);

            if (pinyin != null) {
                account.append(pinyin);
            }
        }

        if (account.length() > MAX_LENGTH) {
            return account.substring(0, MAX_LENGTH);
        }

        return account.toString();
    }

    /**
     * 取单个汉字的第一个读音（多音字取默认读音）
     */
    private static String firstPinyin(char ch) {

        try {

            String[] pinyins =
                    PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);

            if (pinyins == null || pinyins.length == 0) {
                return null;
            }

            return pinyins[0];

        } catch (Exception e) {

            // 非中文字符无法转换，跳过
            return null;
        }
    }

    private static HanyuPinyinOutputFormat createFormat() {

        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();

        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);

        return format;
    }
}
