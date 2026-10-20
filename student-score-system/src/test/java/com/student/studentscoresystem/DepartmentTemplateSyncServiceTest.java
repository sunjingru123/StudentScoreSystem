package com.student.studentscoresystem;

import com.student.studentscoresystem.entity.DepartmentScoreTemplate;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.mapper.DepartmentScoreTemplateMapper;
import com.student.studentscoresystem.service.DepartmentTemplateSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * 评分规则 -> 部门加减分模板同步测试。
 *
 * 重点验证：同一部门下同名但分值 / 类型不同的规则
 * 必须各自拥有独立模板，不能互相覆盖。
 */
@ExtendWith(MockitoExtension.class)
class DepartmentTemplateSyncServiceTest {

    private static final Long DEPARTMENT_ID = 5L;

    private static final String RULE_NAME =
            "教室卫生检查：卫生优良";

    @Mock
    private DepartmentScoreTemplateMapper templateMapper;

    private DepartmentTemplateSyncService service;

    private List<DepartmentScoreTemplate> store;

    private long nextId;

    @BeforeEach
    void setUp() {

        store = new ArrayList<>();
        nextId = 1L;

        service = new DepartmentTemplateSyncService(templateMapper);

        lenient()
                .when(templateMapper.selectList(any()))
                .thenAnswer(invocation -> new ArrayList<>(store));

        lenient()
                .when(templateMapper.insert(any(DepartmentScoreTemplate.class)))
                .thenAnswer(invocation -> {

                    DepartmentScoreTemplate template =
                            invocation.getArgument(0);

                    template.setId(nextId++);

                    store.add(template);

                    return 1;
                });

        lenient()
                .when(templateMapper.updateById(any(DepartmentScoreTemplate.class)))
                .thenAnswer(invocation -> {

                    DepartmentScoreTemplate template =
                            invocation.getArgument(0);

                    for (int i = 0; i < store.size(); i++) {

                        if (Objects.equals(
                                store.get(i).getId(),
                                template.getId()
                        )) {

                            store.set(i, template);

                            return 1;
                        }
                    }

                    return 0;
                });

        lenient()
                .when(templateMapper.deleteById(any(Serializable.class)))
                .thenAnswer(invocation -> {

                    Serializable id =
                            invocation.getArgument(0);

                    store.removeIf(
                            template -> Objects.equals(
                                    template.getId(),
                                    id
                            )
                    );

                    return 1;
                });
    }

    @Test
    void keepsSameNameRulesWithDifferentScores() {

        service.sync(rule(RULE_NAME, (short) 1, "1.00"));

        assertEquals(1, store.size());

        service.sync(rule(RULE_NAME, (short) 1, "0.50"));

        assertEquals(
                2,
                store.size(),
                "同部门同名不同分值应各自同步为独立模板"
        );

        assertScore("1.00", store.get(0));
        assertScore("0.50", store.get(1));

        assertEquals(RULE_NAME, store.get(1).getName());
        assertEquals(DEPARTMENT_ID, store.get(1).getDepartmentId());
    }

    @Test
    void keepsSameNameAndScoreWithDifferentTypes() {

        service.sync(rule(RULE_NAME, (short) 1, "1.00"));

        service.sync(rule(RULE_NAME, (short) -1, "1.00"));

        assertEquals(
                2,
                store.size(),
                "同名同分值但加减分类型不同应视为不同规则"
        );

        assertEquals(
                Short.valueOf((short) 1),
                store.get(0).getScoreType()
        );

        assertEquals(
                Short.valueOf((short) -1),
                store.get(1).getScoreType()
        );
    }

    @Test
    void updatesTemplateWhenIdentityIsIdentical() {

        service.sync(rule(RULE_NAME, (short) 1, "1.00"));

        Long templateId = store.get(0).getId();

        ScoreRule updated =
                rule(RULE_NAME, (short) 1, "1.00");

        updated.setDescription("新的说明");
        updated.setStatus((short) 0);

        service.sync(updated);

        assertEquals(
                1,
                store.size(),
                "身份完全一致的规则不应新增模板"
        );

        assertEquals(templateId, store.get(0).getId());
        assertEquals("新的说明", store.get(0).getDescription());
        assertEquals(
                Short.valueOf((short) 0),
                store.get(0).getStatus()
        );
    }

    @Test
    void movesTemplateInPlaceWhenScoreChanges() {

        service.sync(rule(RULE_NAME, (short) 1, "1.00"));

        Long templateId = store.get(0).getId();

        service.syncRename(
                DEPARTMENT_ID,
                RULE_NAME,
                (short) 1,
                new BigDecimal("1.00"),
                rule(RULE_NAME, (short) 1, "1.50")
        );

        assertEquals(
                1,
                store.size(),
                "修改分值不应产生重复模板"
        );

        assertEquals(
                templateId,
                store.get(0).getId(),
                "应保留模板主键，历史申报通过 template_id 引用它"
        );

        assertScore("1.50", store.get(0));
    }

    @Test
    void removesOnlyMatchingIdentity() {

        service.sync(rule(RULE_NAME, (short) 1, "1.00"));
        service.sync(rule(RULE_NAME, (short) 1, "0.50"));

        assertEquals(2, store.size());

        service.remove(rule(RULE_NAME, (short) 1, "1.00"));

        assertEquals(
                1,
                store.size(),
                "只应删除身份一致的那条模板"
        );

        assertScore("0.50", store.get(0));
    }

    private ScoreRule rule(
            String name,
            Short scoreType,
            String score
    ) {

        ScoreRule rule = new ScoreRule();

        rule.setDepartmentId(DEPARTMENT_ID);
        rule.setName(name);
        rule.setScoreType(scoreType);
        rule.setScore(new BigDecimal(score));
        rule.setStatus((short) 1);

        return rule;
    }

    private void assertScore(
            String expected,
            DepartmentScoreTemplate template
    ) {

        assertNotNull(template.getScore());

        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(template.getScore()),
                "模板分值应为 " + expected
        );
    }
}
