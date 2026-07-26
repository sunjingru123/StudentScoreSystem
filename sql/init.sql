-- ==========================================
-- 综合测评管理系统数据库初始化脚本
-- PostgreSQL
-- Author: 茹茹宝贝
-- ==========================================

-- ==========================================
-- 用户表
-- 存储学生、老师、管理员基础信息
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
-- 部门表
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
    FOREIGN KEY(user_id)
    REFERENCES sys_user(id),
    
    CONSTRAINT fk_department
    FOREIGN KEY(department_id)
    REFERENCES sys_department(id),
    
    CONSTRAINT fk_position
    FOREIGN KEY(position_id)
    REFERENCES sys_position(id)
);

-- ==========================================
-- 学期表
-- ==========================================

CREATE TABLE IF NOT EXISTS sys_semester
(
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(50) NOT NULL,
    start_date      DATE,
    end_date        DATE,
    status          SMALLINT DEFAULT 1,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    FOREIGN KEY(template_id)
    REFERENCES activity_template(id),
    
    CONSTRAINT fk_activity_organizer
    FOREIGN KEY(organizer_id)
    REFERENCES sys_user(id)
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
    FOREIGN KEY(activity_id)
    REFERENCES activity(id),
    
    CONSTRAINT fk_activity_student_user
    FOREIGN KEY(student_id)
    REFERENCES sys_user(id)
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
    FOREIGN KEY(activity_id)
    REFERENCES activity(id),
    
    CONSTRAINT fk_archive_user
    FOREIGN KEY(uploader_id)
    REFERENCES sys_user(id)
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
    description TEXT,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
    FOREIGN KEY(student_id)
    REFERENCES sys_user(id),

    CONSTRAINT fk_apply_activity
    FOREIGN KEY(activity_id)
    REFERENCES activity(id),

    CONSTRAINT fk_apply_rule
    FOREIGN KEY(rule_id)
    REFERENCES score_rule(id)
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
    FOREIGN KEY(apply_id)
    REFERENCES score_apply(id),

    CONSTRAINT fk_audit_user
    FOREIGN KEY(auditor_id)
    REFERENCES sys_user(id)
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
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_record_student
    FOREIGN KEY(student_id)
    REFERENCES sys_user(id),

    CONSTRAINT fk_record_rule
    FOREIGN KEY(rule_id)
    REFERENCES score_rule(id),

    CONSTRAINT fk_record_semester
    FOREIGN KEY(semester_id)
    REFERENCES sys_semester(id)
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
    FOREIGN KEY(record_id)
    REFERENCES score_record(id),

    CONSTRAINT fk_modify_user
    FOREIGN KEY(modifier_id)
    REFERENCES sys_user(id)
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
    FOREIGN KEY(student_id)
    REFERENCES sys_user(id)
);

-- ==========================================
-- 学期表（合并重复定义）
-- ==========================================

CREATE TABLE IF NOT EXISTS sys_semester
(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    start_date DATE,
    end_date DATE,
    status INTEGER DEFAULT 1,
    description TEXT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
    FOREIGN KEY(uploader_id)
    REFERENCES sys_user(id)
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
    FOREIGN KEY(sender_id)
    REFERENCES sys_user(id),


    CONSTRAINT fk_notice_receiver
    FOREIGN KEY(receiver_id)
    REFERENCES sys_user(id)

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
    FOREIGN KEY(publisher_id)
    REFERENCES sys_user(id)

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
    FOREIGN KEY(user_id)
    REFERENCES sys_user(id)

);