package com.student.studentscoresystem;

import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.controller.SysUserController;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.StudentAccountManageService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 学生班级列表接口（GET /user/student/classes）单元测试。
 *
 * 校验点：
 * 1. 空白、纯空格、null 班级会被过滤；
 * 2. 重复班级只保留一份；
 * 3. 结果按升序排列。
 */
class SysUserControllerStudentClassesTest {

    private SysUserController newController(SysUserMapper sysUserMapper) {
        return new SysUserController(
                sysUserMapper,
                mock(SysUserPositionMapper.class),
                mock(SysPositionMapper.class),
                mock(StudentAccountManageService.class),
                mock(com.student.studentscoresystem.service.UserSessionService.class)
        );
    }

    private SysUser userWithClassName(String className) {
        SysUser user = new SysUser();
        user.setClassName(className);
        return user;
    }

    @Test
    void studentClassesShouldTrimDeduplicateAndSort() {
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);

        List<SysUser> rows = new ArrayList<>(Arrays.asList(
                userWithClassName(" 25级7班 "),
                userWithClassName("25级7班"),
                userWithClassName("2025级7班"),
                userWithClassName("24级1班"),
                userWithClassName("2024级1班"),
                userWithClassName(""),
                userWithClassName("   "),
                userWithClassName(null),
                null
        ));

        when(sysUserMapper.selectList(any())).thenReturn(rows);

        Result<List<String>> result = newController(sysUserMapper).studentClasses();

        assertNotNull(result);
        List<String> data = result.getData();
        assertNotNull(data);

        /*
         * 历史短写法（25级7班）与规范写法（2025级7班）
         * 合并为同一个班级选项。
         */
        assertEquals(Arrays.asList("2024级1班", "2025级7班"), data);
    }

    @Test
    void studentClassesShouldReturnEmptyListWhenNoClassName() {
        SysUserMapper sysUserMapper = mock(SysUserMapper.class);

        when(sysUserMapper.selectList(any())).thenReturn(Collections.emptyList());

        Result<List<String>> result = newController(sysUserMapper).studentClasses();

        assertNotNull(result);
        List<String> data = result.getData();
        assertNotNull(data);
        assertTrue(data.isEmpty());
    }
}
