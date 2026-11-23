package com.student.studentscoresystem;

import com.student.studentscoresystem.utils.ClassNameUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 班级名称规范化测试。
 *
 * 历史数据同时存在「25级1班」与「2025级1班」，
 * 系统统一展示四位年份写法，同时兼容旧写法查询。
 */
class ClassNameUtilsTest {

    @Test
    void shortFormIsNormalizedToFullYear() {

        assertEquals(
                "2025级1班",
                ClassNameUtils.normalize("25级1班")
        );

        assertEquals(
                "2024级7班",
                ClassNameUtils.normalize("  24级7班  ")
        );
    }

    @Test
    void normalizedFormIsKeptUnchanged() {

        assertEquals(
                "2025级1班",
                ClassNameUtils.normalize("2025级1班")
        );
    }

    @Test
    void otherNamesAreNotTouched() {

        assertEquals(
                "研究生1班",
                ClassNameUtils.normalize("研究生1班")
        );

        /* 超出可识别年份范围，保持原样 */
        assertEquals(
                "99级1班",
                ClassNameUtils.normalize("99级1班")
        );

        assertNull(
                ClassNameUtils.normalize(null)
        );
    }

    @Test
    void legacyShortFormIsDerived() {

        assertEquals(
                "25级1班",
                ClassNameUtils.legacyShort("2025级1班")
        );

        assertEquals(
                "25级1班",
                ClassNameUtils.legacyShort("25级1班")
        );

        assertNull(
                ClassNameUtils.legacyShort("研究生1班")
        );
    }

    @Test
    void searchKeywordMatchesBothHistoricalForms() {

        /*
         * LIKE '%25级1班%' 能同时命中
         * 「25级1班」与「2025级1班」
         */
        assertEquals(
                "25级1班",
                ClassNameUtils.searchKeyword("2025级1班")
        );

        assertEquals(
                "25级1班",
                ClassNameUtils.searchKeyword("25级1班")
        );

        /*
         * 非班级格式的输入保持原样
         */
        assertEquals(
                "1班",
                ClassNameUtils.searchKeyword("1班")
        );
    }
}
