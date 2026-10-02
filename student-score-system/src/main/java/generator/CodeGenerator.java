package com.student.studentscoresystem.generator;


import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;

import java.util.Collections;


public class CodeGenerator {


    public static void main(String[] args) {


        /*
         * 数据库连接信息从环境变量 / JVM 启动参数读取，
         * 禁止在源码中硬编码数据库密码。
         *
         * 可用变量：
         * DB_URL / DB_USERNAME / DB_PASSWORD
         */
        String url =
                config(
                        "DB_URL",
                        "jdbc:postgresql://localhost:5432/student_score_system"
                );

        String username =
                config(
                        "DB_USERNAME",
                        "postgres"
                );

        String password =
                config(
                        "DB_PASSWORD",
                        null
                );


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


    /**
     * 读取配置：优先环境变量，其次 JVM 系统属性。
     *
     * 若均未设置则返回默认值；默认值为 null 时抛出异常，
     * 避免使用空密码连接数据库。
     */
    private static String config(
            String key,
            String defaultValue
    ) {

        String value =
                System.getenv(key);

        if (value == null || value.isBlank()) {

            value =
                    System.getProperty(key);
        }

        if (value == null || value.isBlank()) {

            if (defaultValue == null) {

                throw new IllegalStateException(
                        "缺少必要配置：" + key
                                + "，请通过环境变量或 -D"
                                + key + " 设置"
                );
            }

            return defaultValue;
        }

        return value;
    }
}