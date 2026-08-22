-- ==========================================
-- 综合测评管理系统数据库初始化脚本【最终整合版】
-- PostgreSQL
-- Author: 茹茹宝贝
-- ==========================================

-- ==========================================
-- 用户表
-- ==========================================
CREATE TABLE IF NOT EXISTS sys_user
(
    id              BIGSERIAL PRIMARY KEY,
    student_no      VARCHAR(20) UNIQUE NOT NULL,
    username        VARCHAR(50) UNIQUE NOT NULL,
    password        VARCHAR(255) NOT NULL,
    real_name       VARCHAR(50) NOT NULL,
    gender          SMALLINT,
    phone           VARCHAR(20),
    email           VARCHAR(100),
    class_name      VARCHAR(100),
    status          SMALLINT DEFAULT 1,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- sys_department系统部门表
-- ==========================================
CREATE TABLE IF NOT EXISTS sys_department
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) UNIQUE,
    description     VARCHAR(255),
    status          SMALLINT DEFAULT 1,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 岗位表
-- ==========================================
CREATE TABLE IF NOT EXISTS sys_position
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    description     VARCHAR(255),
    status          SMALLINT DEFAULT 1,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 用户岗位关联表
-- ==========================================
CREATE TABLE IF NOT EXISTS sys_user_position
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    department_id   BIGINT NOT NULL,
    position_id     BIGINT NOT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user
        FOREIGN KEY(user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_department
        FOREIGN KEY(department_id) REFERENCES sys_department(id),
    CONSTRAINT fk_position
        FOREIGN KEY(position_id) REFERENCES sys_position(id)
);

-- ==========================================
-- 学期表
-- ==========================================
CREATE TABLE IF NOT EXISTS sys_semester
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    start_date      DATE,
    end_date        DATE,
    status          INTEGER DEFAULT 1,
    description     TEXT,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 活动模板表
-- ==========================================
CREATE TABLE IF NOT EXISTS activity_template
(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 活动表
-- ==========================================
CREATE TABLE IF NOT EXISTS activity
(
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    organizer_id BIGINT,
    status SMALLINT DEFAULT 1,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activity_template
        FOREIGN KEY(template_id) REFERENCES activity_template(id),
    CONSTRAINT fk_activity_organizer
        FOREIGN KEY(organizer_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 活动学生关联表
-- ==========================================
CREATE TABLE IF NOT EXISTS activity_student
(
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    join_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status SMALLINT DEFAULT 1,
    score NUMERIC(10,2),

    CONSTRAINT fk_activity_student_activity
        FOREIGN KEY(activity_id) REFERENCES activity(id),
    CONSTRAINT fk_activity_student_user
        FOREIGN KEY(student_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 活动档案表
-- ==========================================
CREATE TABLE IF NOT EXISTS activity_archive
(
    id BIGSERIAL PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    file_name VARCHAR(255),
    file_path VARCHAR(500),
    file_type VARCHAR(50),
    uploader_id BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_archive_activity
        FOREIGN KEY(activity_id) REFERENCES activity(id),
    CONSTRAINT fk_archive_user
        FOREIGN KEY(uploader_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 测评加分规则表
-- ==========================================
CREATE TABLE IF NOT EXISTS score_rule
(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    score NUMERIC(10,2) NOT NULL,
    department_id BIGINT, -- ←新增字段，允许null，null代表全校通用
    description TEXT,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_score_rule_dept FOREIGN KEY(department_id) REFERENCES department(id)
);
CREATE INDEX IF NOT EXISTS idx_score_rule_dept ON score_rule(department_id);

-- ==========================================
-- 加分申请单表
-- ==========================================
CREATE TABLE IF NOT EXISTS score_apply
(
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    activity_id BIGINT,
    rule_id BIGINT NOT NULL,
    apply_score NUMERIC(10,2),
    material_file VARCHAR(500),
    description TEXT,
    status SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_apply_student
        FOREIGN KEY(student_id) REFERENCES sys_user(id),
    CONSTRAINT fk_apply_activity
        FOREIGN KEY(activity_id) REFERENCES activity(id),
    CONSTRAINT fk_apply_rule
        FOREIGN KEY(rule_id) REFERENCES score_rule(id)
);

-- ==========================================
-- 加分审核表
-- ==========================================
CREATE TABLE IF NOT EXISTS score_audit
(
    id BIGSERIAL PRIMARY KEY,
    apply_id BIGINT NOT NULL,
    auditor_id BIGINT,
    audit_status SMALLINT DEFAULT 0,
    audit_comment TEXT,
    audit_time TIMESTAMP,

    CONSTRAINT fk_audit_apply
        FOREIGN KEY(apply_id) REFERENCES score_apply(id),
    CONSTRAINT fk_audit_user
        FOREIGN KEY(auditor_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 加分记录表
-- ==========================================
CREATE TABLE IF NOT EXISTS score_record
(
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    rule_id BIGINT NOT NULL,
    score NUMERIC(10,2) NOT NULL,
    semester_id BIGINT,
    source_type VARCHAR(50),
    source_id BIGINT,
    adminHidden SMALLINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_record_student
        FOREIGN KEY(student_id) REFERENCES sys_user(id),
    CONSTRAINT fk_record_rule
        FOREIGN KEY(rule_id) REFERENCES score_rule(id),
    CONSTRAINT fk_record_semester
        FOREIGN KEY(semester_id) REFERENCES sys_semester(id)
);

-- ==========================================
-- 加分修改日志表
-- ==========================================
CREATE TABLE IF NOT EXISTS score_modify_log
(
    id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL,
    old_score NUMERIC(10,2),
    new_score NUMERIC(10,2),
    modifier_id BIGINT,
    reason TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_modify_record
        FOREIGN KEY(record_id) REFERENCES score_record(id),
    CONSTRAINT fk_modify_user
        FOREIGN KEY(modifier_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 加分流水表
-- ==========================================
CREATE TABLE IF NOT EXISTS score_flow
(
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    change_score NUMERIC(10,2),
    before_score NUMERIC(10,2),
    after_score NUMERIC(10,2),
    change_type VARCHAR(50),
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_flow_student
        FOREIGN KEY(student_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 文件表
-- ==========================================
CREATE TABLE IF NOT EXISTS file_info
(
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    file_size BIGINT,
    file_path VARCHAR(500),
    uploader_id BIGINT,
    business_type VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_file_user
        FOREIGN KEY(uploader_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 通知消息
-- ==========================================
CREATE TABLE IF NOT EXISTS notice_message
(
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    sender_id BIGINT,
    receiver_id BIGINT,
    read_status INTEGER DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notice_sender
        FOREIGN KEY(sender_id) REFERENCES sys_user(id),
    CONSTRAINT fk_notice_receiver
        FOREIGN KEY(receiver_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 系统公告
-- ==========================================
CREATE TABLE IF NOT EXISTS system_notice
(
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT,
    publisher_id BIGINT,
    status INTEGER DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_system_notice_user
        FOREIGN KEY(publisher_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 操作日志
-- ==========================================
CREATE TABLE IF NOT EXISTS operation_log
(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    operation VARCHAR(200),
    method VARCHAR(200),
    request_url VARCHAR(500),
    ip VARCHAR(50),
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_operation_user
        FOREIGN KEY(user_id) REFERENCES sys_user(id)
);

-- ==========================================
-- 课程信息
-- ==========================================
CREATE TABLE IF NOT EXISTS course
(
    id BIGSERIAL PRIMARY KEY,
    course_name VARCHAR(100) NOT NULL,
    teacher_id BIGINT,
    credit INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 分数录入
-- ==========================================
CREATE TABLE IF NOT EXISTS score
(
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    score INTEGER,
    semester VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==========================================
-- 分数调整表 score_admin_adjustment
-- ==========================================
CREATE SEQUENCE IF NOT EXISTS "public"."score_admin_adjustment_id_seq";
CREATE TABLE IF NOT EXISTS "public"."score_admin_adjustment" (
    "id" bigint NOT NULL DEFAULT nextval('public.score_admin_adjustment_id_seq'::regclass),
    "student_id" bigint NOT NULL,
    "admin_id" bigint NOT NULL,
    "adjust_type" smallint NOT NULL,
    "score" numeric(10, 2) NOT NULL,
    "reason" varchar(500),
    "create_time" timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY ("id"),
    CONSTRAINT "fk_adjust_student" FOREIGN KEY ("student_id") REFERENCES "public"."sys_user" ("id"),
    CONSTRAINT "fk_adjust_admin" FOREIGN KEY ("admin_id") REFERENCES "public"."sys_user" ("id"),
    CONSTRAINT "ck_adjust_type" CHECK ("adjust_type" IN (-1, 1))
);
ALTER SEQUENCE "public"."score_admin_adjustment_id_seq" OWNED BY "public"."score_admin_adjustment"."id";

-- ==========================================
-- score_record_admin_log
-- ==========================================
CREATE TABLE IF NOT EXISTS score_record_admin_log (
    id BIGSERIAL PRIMARY KEY,
    score_record_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    operation VARCHAR(50) NOT NULL,
    reason VARCHAR(500),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_score_record_admin_log_record
        FOREIGN KEY (score_record_id) REFERENCES score_record(id),
    CONSTRAINT fk_score_record_admin_log_admin
        FOREIGN KEY (admin_id) REFERENCES sys_user(id)
);

-- ==========================================
-- score_record_operation_log 成绩隐藏恢复日志
-- ==========================================
CREATE SEQUENCE IF NOT EXISTS score_record_operation_log_id_seq;
CREATE TABLE IF NOT EXISTS score_record_operation_log (
    id bigint NOT NULL DEFAULT nextval('score_record_operation_log_id_seq'),
    score_record_id bigint NOT NULL,
    operator_id bigint NOT NULL,
    operation varchar(50) NOT NULL,
    reason varchar(500),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_score_record_operation_record
        FOREIGN KEY (score_record_id) REFERENCES score_record(id),
    CONSTRAINT fk_score_record_operation_user
        FOREIGN KEY (operator_id) REFERENCES sys_user(id)
);

BEGIN;
-- =========================================================
-- 1. 部门表 department
-- =========================================================
CREATE TABLE IF NOT EXISTS public.department (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    name VARCHAR(100) NOT NULL,
    teacher_id BIGINT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_department PRIMARY KEY (id),
    CONSTRAINT uk_department_name UNIQUE (name),
    CONSTRAINT ck_department_status CHECK (status IN (0, 1))
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_department_teacher') THEN
        ALTER TABLE public.department ADD CONSTRAINT fk_department_teacher FOREIGN KEY (teacher_id) REFERENCES public.sys_user(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_department_teacher_id ON public.department(teacher_id);
CREATE INDEX IF NOT EXISTS idx_department_status ON public.department(status);

-- =========================================================
-- 2. 学生部门关系表 sys_user_department
-- =========================================================
CREATE TABLE IF NOT EXISTS public.sys_user_department (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    user_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    position VARCHAR(30) NOT NULL DEFAULT '干事',
    status SMALLINT NOT NULL DEFAULT 1,
    join_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_sys_user_department PRIMARY KEY (id),
    CONSTRAINT uk_sys_user_department UNIQUE (user_id, department_id),
    CONSTRAINT ck_sys_user_department_status CHECK (status IN (0, 1)),
    CONSTRAINT ck_sys_user_department_position CHECK (position IN ('干事','副部长','部长'))
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_department_user') THEN
        ALTER TABLE public.sys_user_department ADD CONSTRAINT fk_user_department_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_user_department_department') THEN
        ALTER TABLE public.sys_user_department ADD CONSTRAINT fk_user_department_department FOREIGN KEY (department_id) REFERENCES public.department(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_department_user_id ON public.sys_user_department(user_id);
CREATE INDEX IF NOT EXISTS idx_user_department_department_id ON public.sys_user_department(department_id);
CREATE INDEX IF NOT EXISTS idx_user_department_position ON public.sys_user_department(position);
CREATE INDEX IF NOT EXISTS idx_user_department_status ON public.sys_user_department(status);

-- =========================================================
-- 3. 部门加减分申报表 department_score_apply
-- =========================================================
CREATE TABLE IF NOT EXISTS public.department_score_apply (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY,
    student_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    score_type SMALLINT NOT NULL,
    score NUMERIC(10, 2) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    evidence_url VARCHAR(1000),
    status SMALLINT NOT NULL DEFAULT 0,
    reviewer_id BIGINT,
    review_remark VARCHAR(1000),
    review_time TIMESTAMP WITHOUT TIME ZONE,
    review_target VARCHAR(30),
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_department_score_apply PRIMARY KEY (id),
    CONSTRAINT ck_department_score_apply_type CHECK (score_type IN (-1, 1)),
    CONSTRAINT ck_department_score_apply_score CHECK (score > 0),
    CONSTRAINT ck_department_score_apply_status CHECK (status IN (0, 1, 2))
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_department_score_apply_student') THEN
        ALTER TABLE public.department_score_apply ADD CONSTRAINT fk_department_score_apply_student FOREIGN KEY (student_id) REFERENCES public.sys_user(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_department_score_apply_department') THEN
        ALTER TABLE public.department_score_apply ADD CONSTRAINT fk_department_score_apply_department FOREIGN KEY (department_id) REFERENCES public.department(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_department_score_apply_reviewer') THEN
        ALTER TABLE public.department_score_apply ADD CONSTRAINT fk_department_score_apply_reviewer FOREIGN KEY (reviewer_id) REFERENCES public.sys_user(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_department_score_apply_student ON public.department_score_apply(student_id);
CREATE INDEX IF NOT EXISTS idx_department_score_apply_department ON public.department_score_apply(department_id);
CREATE INDEX IF NOT EXISTS idx_department_score_apply_status ON public.department_score_apply(status);
CREATE INDEX IF NOT EXISTS idx_department_score_apply_reviewer ON public.department_score_apply(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_department_score_apply_create_time ON public.department_score_apply(create_time);
CREATE INDEX IF NOT EXISTS idx_department_score_apply_duplicate ON public.department_score_apply(student_id, department_id, title, status);

COMMIT;
CREATE TABLE IF NOT EXISTS department_score_template (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    score_type SMALLINT NOT NULL,
    score NUMERIC(10,2) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 添加外键，判断避免重复创建约束
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_department_score_template_department') THEN
        ALTER TABLE department_score_template
        ADD CONSTRAINT fk_department_score_template_department
        FOREIGN KEY (department_id) REFERENCES department(id);
    END IF;
END $$;

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_department_score_template_department
ON department_score_template(department_id);