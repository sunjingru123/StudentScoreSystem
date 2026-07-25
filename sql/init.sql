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

CREATE TABLE activity_template
(
    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,
    
    description VARCHAR(255),
    
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE activity
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