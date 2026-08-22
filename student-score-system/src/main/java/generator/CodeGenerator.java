package com.student.studentscoresystem.generator;


import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;

import java.util.Collections;


public class CodeGenerator {


    public static void main(String[] args) {


        String url =
                "jdbc:postgresql://localhost:5432/student_score_system";

        String username = "postgres";

        String password = "jingru20070507";


        FastAutoGenerator.create(
                        url,
                        username,
                        password
                )

                // 全局配置
                .globalConfig(builder -> {
                    builder
                            .author("茹茹宝贝")
                            .enableSwagger()
                            .disableOpenDir()
                            .outputDir(
                                    System.getProperty("user.dir")
                                            + "/src/main/java"
                            );
                })


                // 包配置
                .packageConfig(builder -> {

                    builder
                            .parent(
                                    "com.student.studentscoresystem"
                            )

                            .entity("entity")

                            .mapper("mapper")

                            .service("service")

                            .serviceImpl("service.impl")

                            .controller("controller")

                            .pathInfo(
                                    Collections.singletonMap(
                                            OutputFile.xml,
                                            System.getProperty("user.dir")
                                                    + "/src/main/resources/mapper"
                                    ));

                })


                // 数据库配置
                .strategyConfig(builder -> {

                    builder
                            .addInclude(
                                    "activity_student",
                                    "activity_template",
                                    "sys_semester",
                                    "sys_user",
                                    "sys_user_position",
                                    "sys_department",
                                    "sys_position",
                                    "activity",
                                    "activity_archive",
                                    "score_apply",
                                    "score_rule",
                                    "score_audit",
                                    "score_record",
                                    "score_modify_log",
                                    "system_notice",
                                    "score_flow",
                                    "file_info",
                                    "notice_message",
                                    "operation_log"
                            )
                            .entityBuilder()
                            .enableLombok()

                            .controllerBuilder()
                            .enableRestStyle();

                })


                .execute();

    }
}