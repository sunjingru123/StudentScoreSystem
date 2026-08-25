--
-- PostgreSQL database dump
--

\restrict mpTSWMkFqOBVpcqtRITgUZf5SFf8Nx3OElsqTOh0xCdJiNuBMJNzYzDMka3rcVC

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: activity; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.activity (
    id bigint NOT NULL,
    template_id bigint,
    name character varying(100) NOT NULL,
    location character varying(255),
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    organizer_id bigint,
    status smallint DEFAULT 1,
    description text,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.activity OWNER TO postgres;

--
-- Name: TABLE activity; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.activity IS '活动信息表';


--
-- Name: activity_archive; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.activity_archive (
    id bigint NOT NULL,
    activity_id bigint NOT NULL,
    file_name character varying(255),
    file_path character varying(500),
    file_type character varying(50),
    uploader_id bigint,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.activity_archive OWNER TO postgres;

--
-- Name: TABLE activity_archive; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.activity_archive IS '活动档案表';


--
-- Name: activity_archive_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.activity_archive_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.activity_archive_id_seq OWNER TO postgres;

--
-- Name: activity_archive_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.activity_archive_id_seq OWNED BY public.activity_archive.id;


--
-- Name: activity_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.activity_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.activity_id_seq OWNER TO postgres;

--
-- Name: activity_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.activity_id_seq OWNED BY public.activity.id;


--
-- Name: activity_student; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.activity_student (
    id bigint NOT NULL,
    activity_id bigint NOT NULL,
    student_id bigint NOT NULL,
    join_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    status smallint DEFAULT 1,
    score numeric(10,2)
);


ALTER TABLE public.activity_student OWNER TO postgres;

--
-- Name: TABLE activity_student; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.activity_student IS '活动参与学生表';


--
-- Name: activity_student_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.activity_student_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.activity_student_id_seq OWNER TO postgres;

--
-- Name: activity_student_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.activity_student_id_seq OWNED BY public.activity_student.id;


--
-- Name: activity_template; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.activity_template (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(255),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.activity_template OWNER TO postgres;

--
-- Name: TABLE activity_template; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.activity_template IS '活动模板表';


--
-- Name: activity_template_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.activity_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.activity_template_id_seq OWNER TO postgres;

--
-- Name: activity_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.activity_template_id_seq OWNED BY public.activity_template.id;


--
-- Name: course; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.course (
    id bigint NOT NULL,
    course_name character varying(100) NOT NULL,
    teacher_id bigint,
    credit integer,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.course OWNER TO postgres;

--
-- Name: course_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.course_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.course_id_seq OWNER TO postgres;

--
-- Name: course_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.course_id_seq OWNED BY public.course.id;


--
-- Name: department_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.department_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.department_id_seq OWNER TO postgres;

--
-- Name: department; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.department (
    id bigint DEFAULT nextval('public.department_id_seq'::regclass) NOT NULL,
    name character varying(100) NOT NULL,
    teacher_id bigint,
    status smallint DEFAULT 1 NOT NULL,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.department OWNER TO postgres;

--
-- Name: department_score_apply; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.department_score_apply (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    department_id bigint NOT NULL,
    score_type smallint NOT NULL,
    score numeric(10,2) NOT NULL,
    title character varying(200) NOT NULL,
    description character varying(1000),
    evidence_url character varying(1000),
    status smallint DEFAULT 0 NOT NULL,
    reviewer_id bigint,
    review_remark character varying(1000),
    review_time timestamp without time zone,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    final_status smallint DEFAULT 0 NOT NULL,
    final_reviewer_id bigint,
    final_review_remark character varying(1000),
    final_review_time timestamp without time zone,
    applicant_id bigint,
    CONSTRAINT ck_department_score_apply_score CHECK ((score > (0)::numeric)),
    CONSTRAINT ck_department_score_apply_status CHECK ((status = ANY (ARRAY[0, 1, 2]))),
    CONSTRAINT ck_department_score_apply_type CHECK ((score_type = ANY (ARRAY['-1'::integer, 1])))
);


ALTER TABLE public.department_score_apply OWNER TO postgres;

--
-- Name: department_score_apply_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.department_score_apply ALTER COLUMN id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME public.department_score_apply_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: department_score_template; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.department_score_template (
    id bigint NOT NULL,
    department_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    description character varying(1000),
    score_type smallint NOT NULL,
    score numeric(10,2) NOT NULL,
    status smallint DEFAULT 1 NOT NULL,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.department_score_template OWNER TO postgres;

--
-- Name: department_score_template_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.department_score_template_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.department_score_template_id_seq OWNER TO postgres;

--
-- Name: department_score_template_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.department_score_template_id_seq OWNED BY public.department_score_template.id;


--
-- Name: file_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.file_info (
    id bigint NOT NULL,
    file_name character varying(255) NOT NULL,
    file_type character varying(50),
    file_size bigint,
    file_path character varying(500),
    uploader_id bigint,
    business_type character varying(50),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.file_info OWNER TO postgres;

--
-- Name: TABLE file_info; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.file_info IS '文件信息表';


--
-- Name: file_info_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.file_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.file_info_id_seq OWNER TO postgres;

--
-- Name: file_info_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.file_info_id_seq OWNED BY public.file_info.id;


--
-- Name: notice_message; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notice_message (
    id bigint NOT NULL,
    title character varying(200) NOT NULL,
    content text,
    sender_id bigint,
    receiver_id bigint,
    read_status integer DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.notice_message OWNER TO postgres;

--
-- Name: TABLE notice_message; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.notice_message IS '通知消息表';


--
-- Name: notice_message_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.notice_message_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.notice_message_id_seq OWNER TO postgres;

--
-- Name: notice_message_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.notice_message_id_seq OWNED BY public.notice_message.id;


--
-- Name: operation_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.operation_log (
    id bigint NOT NULL,
    user_id bigint,
    operation character varying(200),
    method character varying(200),
    request_url character varying(500),
    ip character varying(50),
    description text,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.operation_log OWNER TO postgres;

--
-- Name: TABLE operation_log; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.operation_log IS '系统操作日志表';


--
-- Name: operation_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.operation_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.operation_log_id_seq OWNER TO postgres;

--
-- Name: operation_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.operation_log_id_seq OWNED BY public.operation_log.id;


--
-- Name: score; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    course_id bigint NOT NULL,
    score integer,
    semester character varying(50),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.score OWNER TO postgres;

--
-- Name: score_admin_adjustment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_admin_adjustment (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    admin_id bigint NOT NULL,
    adjust_type smallint NOT NULL,
    score numeric(10,2) NOT NULL,
    reason character varying(500),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_adjust_type CHECK ((adjust_type = ANY (ARRAY['-1'::integer, 1])))
);


ALTER TABLE public.score_admin_adjustment OWNER TO postgres;

--
-- Name: score_admin_adjustment_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_admin_adjustment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_admin_adjustment_id_seq OWNER TO postgres;

--
-- Name: score_admin_adjustment_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_admin_adjustment_id_seq OWNED BY public.score_admin_adjustment.id;


--
-- Name: score_apply; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_apply (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    activity_id bigint,
    rule_id bigint NOT NULL,
    apply_score numeric(10,2),
    material_file character varying(500),
    description text,
    status smallint DEFAULT 0,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    apply_type character varying(30)
);


ALTER TABLE public.score_apply OWNER TO postgres;

--
-- Name: TABLE score_apply; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.score_apply IS '学生自主申报表';


--
-- Name: score_apply_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_apply_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_apply_id_seq OWNER TO postgres;

--
-- Name: score_apply_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_apply_id_seq OWNED BY public.score_apply.id;


--
-- Name: score_audit; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_audit (
    id bigint NOT NULL,
    apply_id bigint NOT NULL,
    auditor_id bigint,
    audit_status smallint DEFAULT 0,
    audit_comment text,
    audit_time timestamp without time zone
);


ALTER TABLE public.score_audit OWNER TO postgres;

--
-- Name: TABLE score_audit; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.score_audit IS '综合测评审核表';


--
-- Name: score_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_audit_id_seq OWNER TO postgres;

--
-- Name: score_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_audit_id_seq OWNED BY public.score_audit.id;


--
-- Name: score_flow; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_flow (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    change_score numeric(10,2),
    before_score numeric(10,2),
    after_score numeric(10,2),
    change_type character varying(50),
    description text,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.score_flow OWNER TO postgres;

--
-- Name: TABLE score_flow; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.score_flow IS '综合测评流水表';


--
-- Name: score_flow_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_flow_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_flow_id_seq OWNER TO postgres;

--
-- Name: score_flow_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_flow_id_seq OWNED BY public.score_flow.id;


--
-- Name: score_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_id_seq OWNER TO postgres;

--
-- Name: score_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_id_seq OWNED BY public.score.id;


--
-- Name: score_modify_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_modify_log (
    id bigint NOT NULL,
    record_id bigint NOT NULL,
    old_score numeric(10,2),
    new_score numeric(10,2),
    modifier_id bigint,
    reason text,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.score_modify_log OWNER TO postgres;

--
-- Name: TABLE score_modify_log; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.score_modify_log IS '综合测评分数修改记录表';


--
-- Name: score_modify_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_modify_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_modify_log_id_seq OWNER TO postgres;

--
-- Name: score_modify_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_modify_log_id_seq OWNED BY public.score_modify_log.id;


--
-- Name: score_record; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_record (
    id bigint NOT NULL,
    student_id bigint NOT NULL,
    rule_id bigint NOT NULL,
    score numeric(10,2) NOT NULL,
    semester_id bigint,
    source_type character varying(50),
    source_id bigint,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    status smallint DEFAULT 1,
    admin_hidden smallint DEFAULT 0
);


ALTER TABLE public.score_record OWNER TO postgres;

--
-- Name: TABLE score_record; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.score_record IS '综合测评记录表';


--
-- Name: COLUMN score_record.status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.score_record.status IS '成绩记录状态：1正常，0隐藏';


--
-- Name: score_record_admin_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_record_admin_log (
    id bigint NOT NULL,
    score_record_id bigint NOT NULL,
    admin_id bigint NOT NULL,
    operation character varying(50) NOT NULL,
    reason character varying(500),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.score_record_admin_log OWNER TO postgres;

--
-- Name: score_record_admin_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_record_admin_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_record_admin_log_id_seq OWNER TO postgres;

--
-- Name: score_record_admin_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_record_admin_log_id_seq OWNED BY public.score_record_admin_log.id;


--
-- Name: score_record_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_record_id_seq OWNER TO postgres;

--
-- Name: score_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_record_id_seq OWNED BY public.score_record.id;


--
-- Name: score_record_operation_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_record_operation_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_record_operation_log_id_seq OWNER TO postgres;

--
-- Name: score_record_operation_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_record_operation_log (
    id bigint DEFAULT nextval('public.score_record_operation_log_id_seq'::regclass) NOT NULL,
    score_record_id bigint NOT NULL,
    operator_id bigint NOT NULL,
    operation character varying(50) NOT NULL,
    reason character varying(500),
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.score_record_operation_log OWNER TO postgres;

--
-- Name: score_rule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.score_rule (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    category character varying(50),
    score numeric(10,2) NOT NULL,
    description text,
    status smallint DEFAULT 1,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.score_rule OWNER TO postgres;

--
-- Name: TABLE score_rule; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.score_rule IS '综合测评规则表';


--
-- Name: score_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.score_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.score_rule_id_seq OWNER TO postgres;

--
-- Name: score_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.score_rule_id_seq OWNED BY public.score_rule.id;


--
-- Name: sys_department; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_department (
    id bigint NOT NULL,
    name character varying(100) NOT NULL,
    code character varying(50),
    description character varying(255),
    status smallint DEFAULT 1,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_department OWNER TO postgres;

--
-- Name: TABLE sys_department; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_department IS '部门组织表';


--
-- Name: sys_department_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_department_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sys_department_id_seq OWNER TO postgres;

--
-- Name: sys_department_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_department_id_seq OWNED BY public.sys_department.id;


--
-- Name: sys_position; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_position (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    description character varying(255),
    status smallint DEFAULT 1,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_position OWNER TO postgres;

--
-- Name: TABLE sys_position; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_position IS '岗位信息表';


--
-- Name: sys_position_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_position_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sys_position_id_seq OWNER TO postgres;

--
-- Name: sys_position_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_position_id_seq OWNED BY public.sys_position.id;


--
-- Name: sys_semester; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_semester (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    start_date date,
    end_date date,
    status smallint DEFAULT 1,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_semester OWNER TO postgres;

--
-- Name: TABLE sys_semester; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_semester IS '学期信息表';


--
-- Name: sys_semester_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_semester_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sys_semester_id_seq OWNER TO postgres;

--
-- Name: sys_semester_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_semester_id_seq OWNED BY public.sys_semester.id;


--
-- Name: sys_user; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_user (
    id bigint NOT NULL,
    student_no character varying(20) NOT NULL,
    username character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    real_name character varying(50) NOT NULL,
    gender smallint,
    phone character varying(20),
    email character varying(100),
    class_name character varying(100),
    status smallint DEFAULT 1,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_user OWNER TO postgres;

--
-- Name: TABLE sys_user; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_user IS '系统用户表';


--
-- Name: sys_user_department_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_user_department_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sys_user_department_id_seq OWNER TO postgres;

--
-- Name: sys_user_department; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_user_department (
    id bigint DEFAULT nextval('public.sys_user_department_id_seq'::regclass) NOT NULL,
    user_id bigint NOT NULL,
    department_id bigint NOT NULL,
    "position" character varying(30) DEFAULT '干事'::character varying NOT NULL,
    status smallint DEFAULT 1 NOT NULL,
    join_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_user_department OWNER TO postgres;

--
-- Name: sys_user_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sys_user_id_seq OWNER TO postgres;

--
-- Name: sys_user_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_user_id_seq OWNED BY public.sys_user.id;


--
-- Name: sys_user_position; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.sys_user_position (
    id bigint NOT NULL,
    user_id bigint NOT NULL,
    department_id bigint NOT NULL,
    position_id bigint NOT NULL,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.sys_user_position OWNER TO postgres;

--
-- Name: TABLE sys_user_position; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.sys_user_position IS '用户岗位关联表';


--
-- Name: sys_user_position_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.sys_user_position_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sys_user_position_id_seq OWNER TO postgres;

--
-- Name: sys_user_position_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.sys_user_position_id_seq OWNED BY public.sys_user_position.id;


--
-- Name: system_notice; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.system_notice (
    id bigint NOT NULL,
    title character varying(200) NOT NULL,
    content text,
    publisher_id bigint,
    status integer DEFAULT 1,
    create_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.system_notice OWNER TO postgres;

--
-- Name: TABLE system_notice; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.system_notice IS '系统公告表';


--
-- Name: system_notice_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.system_notice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.system_notice_id_seq OWNER TO postgres;

--
-- Name: system_notice_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.system_notice_id_seq OWNED BY public.system_notice.id;


--
-- Name: activity id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity ALTER COLUMN id SET DEFAULT nextval('public.activity_id_seq'::regclass);


--
-- Name: activity_archive id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_archive ALTER COLUMN id SET DEFAULT nextval('public.activity_archive_id_seq'::regclass);


--
-- Name: activity_student id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_student ALTER COLUMN id SET DEFAULT nextval('public.activity_student_id_seq'::regclass);


--
-- Name: activity_template id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_template ALTER COLUMN id SET DEFAULT nextval('public.activity_template_id_seq'::regclass);


--
-- Name: course id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course ALTER COLUMN id SET DEFAULT nextval('public.course_id_seq'::regclass);


--
-- Name: department_score_template id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.department_score_template ALTER COLUMN id SET DEFAULT nextval('public.department_score_template_id_seq'::regclass);


--
-- Name: file_info id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_info ALTER COLUMN id SET DEFAULT nextval('public.file_info_id_seq'::regclass);


--
-- Name: notice_message id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notice_message ALTER COLUMN id SET DEFAULT nextval('public.notice_message_id_seq'::regclass);


--
-- Name: operation_log id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.operation_log ALTER COLUMN id SET DEFAULT nextval('public.operation_log_id_seq'::regclass);


--
-- Name: score id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score ALTER COLUMN id SET DEFAULT nextval('public.score_id_seq'::regclass);


--
-- Name: score_admin_adjustment id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_admin_adjustment ALTER COLUMN id SET DEFAULT nextval('public.score_admin_adjustment_id_seq'::regclass);


--
-- Name: score_apply id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_apply ALTER COLUMN id SET DEFAULT nextval('public.score_apply_id_seq'::regclass);


--
-- Name: score_audit id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_audit ALTER COLUMN id SET DEFAULT nextval('public.score_audit_id_seq'::regclass);


--
-- Name: score_flow id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_flow ALTER COLUMN id SET DEFAULT nextval('public.score_flow_id_seq'::regclass);


--
-- Name: score_modify_log id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_modify_log ALTER COLUMN id SET DEFAULT nextval('public.score_modify_log_id_seq'::regclass);


--
-- Name: score_record id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record ALTER COLUMN id SET DEFAULT nextval('public.score_record_id_seq'::regclass);


--
-- Name: score_record_admin_log id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record_admin_log ALTER COLUMN id SET DEFAULT nextval('public.score_record_admin_log_id_seq'::regclass);


--
-- Name: score_rule id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_rule ALTER COLUMN id SET DEFAULT nextval('public.score_rule_id_seq'::regclass);


--
-- Name: sys_department id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_department ALTER COLUMN id SET DEFAULT nextval('public.sys_department_id_seq'::regclass);


--
-- Name: sys_position id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_position ALTER COLUMN id SET DEFAULT nextval('public.sys_position_id_seq'::regclass);


--
-- Name: sys_semester id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_semester ALTER COLUMN id SET DEFAULT nextval('public.sys_semester_id_seq'::regclass);


--
-- Name: sys_user id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user ALTER COLUMN id SET DEFAULT nextval('public.sys_user_id_seq'::regclass);


--
-- Name: sys_user_position id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_position ALTER COLUMN id SET DEFAULT nextval('public.sys_user_position_id_seq'::regclass);


--
-- Name: system_notice id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_notice ALTER COLUMN id SET DEFAULT nextval('public.system_notice_id_seq'::regclass);


--
-- Data for Name: activity; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.activity (id, template_id, name, location, start_time, end_time, organizer_id, status, description, create_time, update_time) FROM stdin;
1	\N	程序设计竞赛	\N	\N	\N	\N	1	\N	2026-08-09 16:07:49.923248	2026-08-09 16:07:49.923248
\.


--
-- Data for Name: activity_archive; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.activity_archive (id, activity_id, file_name, file_path, file_type, uploader_id, create_time) FROM stdin;
\.


--
-- Data for Name: activity_student; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.activity_student (id, activity_id, student_id, join_time, status, score) FROM stdin;
\.


--
-- Data for Name: activity_template; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.activity_template (id, name, description, create_time, update_time) FROM stdin;
\.


--
-- Data for Name: course; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.course (id, course_name, teacher_id, credit, create_time) FROM stdin;
1	Java程序设计	2	4	2026-08-08 00:16:05.474887
2	数据库原理	2	3	2026-08-08 00:18:18.006568
\.


--
-- Data for Name: department; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.department (id, name, teacher_id, status, create_time) FROM stdin;
1	学习部	2	1	2026-08-21 21:43:37.913936
\.


--
-- Data for Name: department_score_apply; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.department_score_apply (id, student_id, department_id, score_type, score, title, description, evidence_url, status, reviewer_id, review_remark, review_time, create_time, update_time, final_status, final_reviewer_id, final_review_remark, final_review_time, applicant_id) FROM stdin;
\.


--
-- Data for Name: department_score_template; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.department_score_template (id, department_id, name, description, score_type, score, status, create_time, update_time) FROM stdin;
1	1	部门活动迟到	参加部门活动迟到一次，扣除个人综合测评分。	-1	1.00	1	2026-08-21 23:07:30.709626	2026-08-21 23:07:30.709626
2	1	部门活动优秀表现	积极参加部门活动并表现优秀，可申请加分。	1	2.00	1	2026-08-21 23:07:30.709626	2026-08-21 23:07:30.709626
3	1	无故缺席部门活动	无正当理由缺席部门活动，扣除个人综合测评分。	-1	2.00	1	2026-08-21 23:07:30.709626	2026-08-21 23:07:30.709626
\.


--
-- Data for Name: file_info; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.file_info (id, file_name, file_type, file_size, file_path, uploader_id, business_type, create_time) FROM stdin;
\.


--
-- Data for Name: notice_message; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notice_message (id, title, content, sender_id, receiver_id, read_status, create_time) FROM stdin;
1	成绩通知	你的成绩审核已完成	1	3	0	2026-08-08 21:39:05.336521
2	测试通知2	测试自增ID	1	3	0	2026-08-08 21:39:16.286199
3	考试安排通知	请同学们按时参加期末考试	1	3	0	2026-08-08 21:41:42.709226
4	成绩申请审核通知	你的综合测评申请已审核通过	1	3	0	2026-08-09 16:23:20.371101
5	成绩申请审核通知	你的综合测评申请已审核通过	1	3	0	2026-08-09 16:40:43.076262
6	综合测评审核通知	你的综合测评申请已通过，获得5.00分	1	3	0	2026-08-09 21:26:37.95796
7	综合测评审核通知	你的综合测评申请已通过，获得5.00分	1	3	0	2026-08-09 21:29:59.555651
8	综合测评审核通知	你的综合测评申请已通过，获得5.00分	1	3	0	2026-08-09 21:37:20.867196
\.


--
-- Data for Name: operation_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.operation_log (id, user_id, operation, method, request_url, ip, description, create_time) FROM stdin;
\.


--
-- Data for Name: score; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score (id, student_id, course_id, score, semester, create_time) FROM stdin;
2	3	1	95	2026春	2026-08-08 00:32:53.188866
3	3	2	88	2026春	2026-08-08 00:32:53.188866
\.


--
-- Data for Name: score_admin_adjustment; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_admin_adjustment (id, student_id, admin_id, adjust_type, score, reason, create_time) FROM stdin;
\.


--
-- Data for Name: score_apply; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_apply (id, student_id, activity_id, rule_id, apply_score, material_file, description, status, create_time, update_time, apply_type) FROM stdin;
70	4	1	1	5.00		11111111111111111	0	2026-08-12 22:16:40.651686	2026-08-12 22:16:40.651686	\N
69	2	1	1	5.00		啊啊啊啊啊啊啊啊	2	2026-08-12 22:16:12.712844	2026-08-12 22:16:12.712844	\N
67	3	1	1	5.00		蓝桥杯1111111	1	2026-08-12 22:10:55.752613	2026-08-12 22:10:55.752613	\N
34	4	1	1	5.00		蓝桥杯三等奖	1	2026-08-12 00:53:05.048348	2026-08-12 00:53:05.048348	\N
\.


--
-- Data for Name: score_audit; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_audit (id, apply_id, auditor_id, audit_status, audit_comment, audit_time) FROM stdin;
\.


--
-- Data for Name: score_flow; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_flow (id, student_id, change_score, before_score, after_score, change_type, description, create_time) FROM stdin;
1	3	5.00	0.00	5.00	apply	自主申报审核通过	2026-08-09 21:26:37.945514
2	3	5.00	0.00	5.00	apply	自主申报审核通过	2026-08-09 21:29:59.545764
3	3	5.00	20.00	25.00	apply	自主申报审核通过	2026-08-09 21:37:20.862094
\.


--
-- Data for Name: score_modify_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_modify_log (id, record_id, old_score, new_score, modifier_id, reason, create_time) FROM stdin;
\.


--
-- Data for Name: score_record; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_record (id, student_id, rule_id, score, semester_id, source_type, source_id, create_time, status, admin_hidden) FROM stdin;
1	3	1	5.00	\N	apply	67	2026-08-12 22:11:40.913141	1	0
3	4	1	5.00	\N	apply	34	2026-08-12 22:55:38.938524	1	0
2	3	1	5.00	\N	apply	67	2026-08-12 22:19:54.078603	1	1
\.


--
-- Data for Name: score_record_admin_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_record_admin_log (id, score_record_id, admin_id, operation, reason, create_time) FROM stdin;
\.


--
-- Data for Name: score_record_operation_log; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_record_operation_log (id, score_record_id, operator_id, operation, reason, create_time) FROM stdin;
2088529238319390721	2	1	HIDE	\N	2026-08-15 15:32:28.066404
2088529258326220802	2	1	RESTORE	\N	2026-08-15 15:32:32.850109
2090101219112235010	2	1	HIDE	\N	2026-08-19 23:38:57.493385
2090460392706514946	2	1	RESTORE	\N	2026-08-20 23:26:11.149234
2090460408351268866	2	1	HIDE	\N	2026-08-20 23:26:14.891411
\.


--
-- Data for Name: score_rule; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.score_rule (id, name, category, score, description, status, create_time, update_time) FROM stdin;
1	竞赛获奖	\N	5.00	\N	1	2026-08-09 16:08:01.18635	2026-08-09 16:08:01.18635
\.


--
-- Data for Name: sys_department; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_department (id, name, code, description, status, create_time, update_time) FROM stdin;
1	计算机学院	\N	\N	1	2026-07-25 18:36:58.919002	2026-07-25 18:36:58.919002
2	软件工程系	\N	\N	1	2026-07-25 18:36:58.919002	2026-07-25 18:36:58.919002
3	学生工作办公室	\N	\N	1	2026-07-25 18:36:58.919002	2026-07-25 18:36:58.919002
\.


--
-- Data for Name: sys_position; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_position (id, name, description, status, create_time) FROM stdin;
1	管理员	\N	1	2026-07-25 18:36:58.919002
2	辅导员	\N	1	2026-07-25 18:36:58.919002
3	班主任	\N	1	2026-07-25 18:36:58.919002
4	学生	\N	1	2026-07-25 18:36:58.919002
\.


--
-- Data for Name: sys_semester; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_semester (id, name, start_date, end_date, status, create_time) FROM stdin;
1	2026-2027第一学期	\N	\N	1	2026-07-26 20:33:06.104883
\.


--
-- Data for Name: sys_user; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_user (id, student_no, username, password, real_name, gender, phone, email, class_name, status, create_time, update_time) FROM stdin;
1	1001	admin	123456	系统管理员	1	\N	\N	教职工	1	2026-07-25 18:41:28.620859	2026-07-25 18:41:28.620859
2	1002	teacher1	123456	张老师	1	\N	\N	教职工	1	2026-07-25 18:41:28.620859	2026-07-25 18:41:28.620859
3	2026001	student1	123456	小明	1	\N	\N	软件工程1班	1	2026-08-07 23:50:58.539938	2026-08-07 23:50:58.539938
4	2026002	student2	123456	李四	\N		\N	软件工程2班	1	2026-08-12 00:23:06.032479	2026-08-12 00:23:06.032479
\.


--
-- Data for Name: sys_user_department; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_user_department (id, user_id, department_id, "position", status, join_time) FROM stdin;
7	3	1	副部长	1	2026-08-21 21:44:52.271742
8	4	1	干事	1	2026-08-21 21:44:52.271742
\.


--
-- Data for Name: sys_user_position; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.sys_user_position (id, user_id, department_id, position_id, create_time) FROM stdin;
1	1	1	1	2026-07-25 18:41:56.899125
2	2	1	2	2026-07-25 18:41:56.899125
4	3	1	4	2026-08-07 23:54:18.171903
6	4	1	4	2026-08-12 00:23:06.05091
\.


--
-- Data for Name: system_notice; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.system_notice (id, title, content, publisher_id, status, create_time, update_time) FROM stdin;
\.


--
-- Name: activity_archive_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.activity_archive_id_seq', 1, false);


--
-- Name: activity_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.activity_id_seq', 1, true);


--
-- Name: activity_student_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.activity_student_id_seq', 1, false);


--
-- Name: activity_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.activity_template_id_seq', 1, false);


--
-- Name: course_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.course_id_seq', 2, true);


--
-- Name: department_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.department_id_seq', 2, true);


--
-- Name: department_score_apply_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.department_score_apply_id_seq', 1, false);


--
-- Name: department_score_template_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.department_score_template_id_seq', 3, true);


--
-- Name: file_info_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.file_info_id_seq', 1, false);


--
-- Name: notice_message_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.notice_message_id_seq', 8, true);


--
-- Name: operation_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.operation_log_id_seq', 1, false);


--
-- Name: score_admin_adjustment_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_admin_adjustment_id_seq', 5, true);


--
-- Name: score_apply_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_apply_id_seq', 70, true);


--
-- Name: score_audit_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_audit_id_seq', 3, true);


--
-- Name: score_flow_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_flow_id_seq', 3, true);


--
-- Name: score_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_id_seq', 4, true);


--
-- Name: score_modify_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_modify_log_id_seq', 1, false);


--
-- Name: score_record_admin_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_record_admin_log_id_seq', 1, false);


--
-- Name: score_record_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_record_id_seq', 8, true);


--
-- Name: score_record_operation_log_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_record_operation_log_id_seq', 1, false);


--
-- Name: score_rule_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.score_rule_id_seq', 1, true);


--
-- Name: sys_department_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_department_id_seq', 3, true);


--
-- Name: sys_position_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_position_id_seq', 4, true);


--
-- Name: sys_semester_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_semester_id_seq', 1, true);


--
-- Name: sys_user_department_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_user_department_id_seq', 8, true);


--
-- Name: sys_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_user_id_seq', 4, true);


--
-- Name: sys_user_position_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.sys_user_position_id_seq', 6, true);


--
-- Name: system_notice_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.system_notice_id_seq', 1, false);


--
-- Name: activity_archive activity_archive_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_archive
    ADD CONSTRAINT activity_archive_pkey PRIMARY KEY (id);


--
-- Name: activity activity_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity
    ADD CONSTRAINT activity_pkey PRIMARY KEY (id);


--
-- Name: activity_student activity_student_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_student
    ADD CONSTRAINT activity_student_pkey PRIMARY KEY (id);


--
-- Name: activity_template activity_template_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_template
    ADD CONSTRAINT activity_template_pkey PRIMARY KEY (id);


--
-- Name: course course_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.course
    ADD CONSTRAINT course_pkey PRIMARY KEY (id);


--
-- Name: department department_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT department_pkey PRIMARY KEY (id);


--
-- Name: department_score_template department_score_template_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.department_score_template
    ADD CONSTRAINT department_score_template_pkey PRIMARY KEY (id);


--
-- Name: file_info file_info_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_info
    ADD CONSTRAINT file_info_pkey PRIMARY KEY (id);


--
-- Name: notice_message notice_message_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notice_message
    ADD CONSTRAINT notice_message_pkey PRIMARY KEY (id);


--
-- Name: operation_log operation_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.operation_log
    ADD CONSTRAINT operation_log_pkey PRIMARY KEY (id);


--
-- Name: department_score_apply pk_department_score_apply; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.department_score_apply
    ADD CONSTRAINT pk_department_score_apply PRIMARY KEY (id);


--
-- Name: score_admin_adjustment score_admin_adjustment_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_admin_adjustment
    ADD CONSTRAINT score_admin_adjustment_pkey PRIMARY KEY (id);


--
-- Name: score_apply score_apply_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_apply
    ADD CONSTRAINT score_apply_pkey PRIMARY KEY (id);


--
-- Name: score_audit score_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_audit
    ADD CONSTRAINT score_audit_pkey PRIMARY KEY (id);


--
-- Name: score_flow score_flow_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_flow
    ADD CONSTRAINT score_flow_pkey PRIMARY KEY (id);


--
-- Name: score_modify_log score_modify_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_modify_log
    ADD CONSTRAINT score_modify_log_pkey PRIMARY KEY (id);


--
-- Name: score score_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score
    ADD CONSTRAINT score_pkey PRIMARY KEY (id);


--
-- Name: score_record_admin_log score_record_admin_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record_admin_log
    ADD CONSTRAINT score_record_admin_log_pkey PRIMARY KEY (id);


--
-- Name: score_record_operation_log score_record_operation_log_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record_operation_log
    ADD CONSTRAINT score_record_operation_log_pkey PRIMARY KEY (id);


--
-- Name: score_record score_record_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record
    ADD CONSTRAINT score_record_pkey PRIMARY KEY (id);


--
-- Name: score_rule score_rule_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_rule
    ADD CONSTRAINT score_rule_pkey PRIMARY KEY (id);


--
-- Name: sys_department sys_department_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_department
    ADD CONSTRAINT sys_department_code_key UNIQUE (code);


--
-- Name: sys_department sys_department_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_department
    ADD CONSTRAINT sys_department_pkey PRIMARY KEY (id);


--
-- Name: sys_position sys_position_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_position
    ADD CONSTRAINT sys_position_pkey PRIMARY KEY (id);


--
-- Name: sys_semester sys_semester_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_semester
    ADD CONSTRAINT sys_semester_pkey PRIMARY KEY (id);


--
-- Name: sys_user_department sys_user_department_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_department
    ADD CONSTRAINT sys_user_department_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_pkey PRIMARY KEY (id);


--
-- Name: sys_user_position sys_user_position_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_position
    ADD CONSTRAINT sys_user_position_pkey PRIMARY KEY (id);


--
-- Name: sys_user sys_user_student_no_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_student_no_key UNIQUE (student_no);


--
-- Name: sys_user sys_user_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT sys_user_username_key UNIQUE (username);


--
-- Name: system_notice system_notice_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_notice
    ADD CONSTRAINT system_notice_pkey PRIMARY KEY (id);


--
-- Name: department uk_department_name; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT uk_department_name UNIQUE (name);


--
-- Name: sys_user_department uk_user_department; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_department
    ADD CONSTRAINT uk_user_department UNIQUE (user_id, department_id);


--
-- Name: idx_department_teacher_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_department_teacher_id ON public.department USING btree (teacher_id);


--
-- Name: idx_user_department_department_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_department_department_id ON public.sys_user_department USING btree (department_id);


--
-- Name: idx_user_department_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_user_department_status ON public.sys_user_department USING btree (status);


--
-- Name: activity fk_activity_organizer; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity
    ADD CONSTRAINT fk_activity_organizer FOREIGN KEY (organizer_id) REFERENCES public.sys_user(id);


--
-- Name: activity_student fk_activity_student_activity; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_student
    ADD CONSTRAINT fk_activity_student_activity FOREIGN KEY (activity_id) REFERENCES public.activity(id);


--
-- Name: activity_student fk_activity_student_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_student
    ADD CONSTRAINT fk_activity_student_user FOREIGN KEY (student_id) REFERENCES public.sys_user(id);


--
-- Name: score_admin_adjustment fk_adjust_admin; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_admin_adjustment
    ADD CONSTRAINT fk_adjust_admin FOREIGN KEY (admin_id) REFERENCES public.sys_user(id);


--
-- Name: score_admin_adjustment fk_adjust_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_admin_adjustment
    ADD CONSTRAINT fk_adjust_student FOREIGN KEY (student_id) REFERENCES public.sys_user(id);


--
-- Name: score_apply fk_apply_activity; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_apply
    ADD CONSTRAINT fk_apply_activity FOREIGN KEY (activity_id) REFERENCES public.activity(id);


--
-- Name: score_apply fk_apply_rule; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_apply
    ADD CONSTRAINT fk_apply_rule FOREIGN KEY (rule_id) REFERENCES public.score_rule(id);


--
-- Name: score_apply fk_apply_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_apply
    ADD CONSTRAINT fk_apply_student FOREIGN KEY (student_id) REFERENCES public.sys_user(id);


--
-- Name: activity_archive fk_archive_activity; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_archive
    ADD CONSTRAINT fk_archive_activity FOREIGN KEY (activity_id) REFERENCES public.activity(id);


--
-- Name: activity_archive fk_archive_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.activity_archive
    ADD CONSTRAINT fk_archive_user FOREIGN KEY (uploader_id) REFERENCES public.sys_user(id);


--
-- Name: score_audit fk_audit_apply; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_audit
    ADD CONSTRAINT fk_audit_apply FOREIGN KEY (apply_id) REFERENCES public.score_apply(id);


--
-- Name: score_audit fk_audit_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_audit
    ADD CONSTRAINT fk_audit_user FOREIGN KEY (auditor_id) REFERENCES public.sys_user(id);


--
-- Name: sys_user_position fk_department; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_position
    ADD CONSTRAINT fk_department FOREIGN KEY (department_id) REFERENCES public.sys_department(id);


--
-- Name: department_score_apply fk_department_score_apply_reviewer; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.department_score_apply
    ADD CONSTRAINT fk_department_score_apply_reviewer FOREIGN KEY (reviewer_id) REFERENCES public.sys_user(id);


--
-- Name: department fk_department_teacher; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.department
    ADD CONSTRAINT fk_department_teacher FOREIGN KEY (teacher_id) REFERENCES public.sys_user(id);


--
-- Name: file_info fk_file_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.file_info
    ADD CONSTRAINT fk_file_user FOREIGN KEY (uploader_id) REFERENCES public.sys_user(id);


--
-- Name: score_flow fk_flow_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_flow
    ADD CONSTRAINT fk_flow_student FOREIGN KEY (student_id) REFERENCES public.sys_user(id);


--
-- Name: score_modify_log fk_modify_record; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_modify_log
    ADD CONSTRAINT fk_modify_record FOREIGN KEY (record_id) REFERENCES public.score_record(id);


--
-- Name: score_modify_log fk_modify_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_modify_log
    ADD CONSTRAINT fk_modify_user FOREIGN KEY (modifier_id) REFERENCES public.sys_user(id);


--
-- Name: notice_message fk_notice_receiver; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notice_message
    ADD CONSTRAINT fk_notice_receiver FOREIGN KEY (receiver_id) REFERENCES public.sys_user(id);


--
-- Name: notice_message fk_notice_sender; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notice_message
    ADD CONSTRAINT fk_notice_sender FOREIGN KEY (sender_id) REFERENCES public.sys_user(id);


--
-- Name: operation_log fk_operation_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.operation_log
    ADD CONSTRAINT fk_operation_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: sys_user_position fk_position; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_position
    ADD CONSTRAINT fk_position FOREIGN KEY (position_id) REFERENCES public.sys_position(id);


--
-- Name: score_record fk_record_rule; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record
    ADD CONSTRAINT fk_record_rule FOREIGN KEY (rule_id) REFERENCES public.score_rule(id);


--
-- Name: score_record fk_record_semester; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record
    ADD CONSTRAINT fk_record_semester FOREIGN KEY (semester_id) REFERENCES public.sys_semester(id);


--
-- Name: score_record fk_record_student; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record
    ADD CONSTRAINT fk_record_student FOREIGN KEY (student_id) REFERENCES public.sys_user(id);


--
-- Name: score_record_admin_log fk_score_record_admin_log_admin; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record_admin_log
    ADD CONSTRAINT fk_score_record_admin_log_admin FOREIGN KEY (admin_id) REFERENCES public.sys_user(id);


--
-- Name: score_record_admin_log fk_score_record_admin_log_record; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record_admin_log
    ADD CONSTRAINT fk_score_record_admin_log_record FOREIGN KEY (score_record_id) REFERENCES public.score_record(id);


--
-- Name: score_record_operation_log fk_score_record_operation_record; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record_operation_log
    ADD CONSTRAINT fk_score_record_operation_record FOREIGN KEY (score_record_id) REFERENCES public.score_record(id);


--
-- Name: score_record_operation_log fk_score_record_operation_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.score_record_operation_log
    ADD CONSTRAINT fk_score_record_operation_user FOREIGN KEY (operator_id) REFERENCES public.sys_user(id);


--
-- Name: system_notice fk_system_notice_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_notice
    ADD CONSTRAINT fk_system_notice_user FOREIGN KEY (publisher_id) REFERENCES public.sys_user(id);


--
-- Name: sys_user_position fk_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_position
    ADD CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- Name: sys_user_department fk_user_department_department; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_department
    ADD CONSTRAINT fk_user_department_department FOREIGN KEY (department_id) REFERENCES public.department(id);


--
-- Name: sys_user_department fk_user_department_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.sys_user_department
    ADD CONSTRAINT fk_user_department_user FOREIGN KEY (user_id) REFERENCES public.sys_user(id);


--
-- PostgreSQL database dump complete
--

\unrestrict mpTSWMkFqOBVpcqtRITgUZf5SFf8Nx3OElsqTOh0xCdJiNuBMJNzYzDMka3rcVC

