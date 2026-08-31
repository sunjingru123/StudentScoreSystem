package com.student.studentscoresystem.mapper;

import com.student.studentscoresystem.dto.ScoreExportRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScoreExportMapper {

    /**
     * 查询指定学期、指定班级的有效加减分记录
     *
     * 数据来源：
     *
     * score_record
     *      ↓ student_id
     * sys_user
     *
     * score_record
     *      ↓ rule_id
     * score_rule
     *
     * score_record
     *      ↓ source_id
     * department_score_apply
     *
     * 导出条件：
     *
     * 1. 指定学期
     * 2. 指定班级
     * 3. score_record.status = 1
     * 4. score_record.admin_hidden = 0
     * 5. sys_user.status = 1
     */
    @Select("""
            SELECT
                u.id AS student_id,
                u.student_no AS student_no,
                u.real_name AS real_name,
                u.class_name AS class_name,

                r.semester_id AS semester_id,

                s.name AS semester_name,

                sr.name AS rule_name,

                r.source_type AS source_type,
                r.source_id AS source_id,
                r.score AS score,

                dsa.title AS department_apply_title,
                dsa.description AS department_apply_description

            FROM score_record r

            INNER JOIN sys_user u
                ON u.id = r.student_id

            LEFT JOIN sys_semester s
                ON s.id = r.semester_id

            LEFT JOIN score_rule sr
                ON sr.id = r.rule_id

            LEFT JOIN department_score_apply dsa
                ON dsa.id = r.source_id
                AND r.source_type = 'DEPARTMENT'

            WHERE r.semester_id = #{semesterId}

              AND u.class_name = #{className}

              AND r.status = 1

              AND r.admin_hidden = 0

              AND u.status = 1

            ORDER BY
                u.student_no ASC,
                r.create_time ASC,
                r.id ASC
            """)
    List<ScoreExportRow> selectExportRows(
            @Param("semesterId") Long semesterId,
            @Param("className") String className
    );
}