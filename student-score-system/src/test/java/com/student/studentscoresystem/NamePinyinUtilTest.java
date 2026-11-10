package com.student.studentscoresystem;

import com.student.studentscoresystem.utils.NamePinyinUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * =========================================================
 * 姓名转拼音（教师账号自动生成）测试
 * =========================================================
 *
 * 管理员新增教师时不需要填写账号，
 * 账号 = 教师姓名全拼。
 * =========================================================
 */
class NamePinyinUtilTest {

    @Test
    void shouldConvertChineseNameToFullPinyin() {

        assertEquals("sunjingru", NamePinyinUtil.toAccount("孙靖茹"));
        assertEquals("zhangsan", NamePinyinUtil.toAccount("张三"));
        assertEquals("lisi", NamePinyinUtil.toAccount("李四"));
    }

    @Test
    void shouldTrimSpacesAndIgnorePunctuation() {

        assertEquals("sunjingru", NamePinyinUtil.toAccount("  孙靖茹  "));
        assertEquals("zhangsan", NamePinyinUtil.toAccount("张 三"));
        assertEquals("lisi", NamePinyinUtil.toAccount("李·四"));
    }

    @Test
    void shouldKeepLatinLettersAndDigitsInLowerCase() {

        assertEquals("tomli", NamePinyinUtil.toAccount("Tom Li"));
        assertEquals("zhangsan2", NamePinyinUtil.toAccount("张三2"));
    }

    @Test
    void shouldReturnEmptyWhenNothingConvertible() {

        assertEquals("", NamePinyinUtil.toAccount(null));
        assertEquals("", NamePinyinUtil.toAccount(""));
        assertEquals("", NamePinyinUtil.toAccount("   "));
        assertEquals("", NamePinyinUtil.toAccount("!!!"));
    }
}
