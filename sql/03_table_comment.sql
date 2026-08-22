-- ==========================================
-- 综合测评管理系统表注释
-- PostgreSQL
-- Author: 茹茹宝贝
-- ==========================================

COMMENT ON TABLE sys_user 
IS '系统用户表';


COMMENT ON TABLE sys_department
IS '部门组织表';


COMMENT ON TABLE sys_position
IS '岗位信息表';


COMMENT ON TABLE sys_user_position
IS '用户岗位关联表';


COMMENT ON TABLE sys_semester
IS '学期信息表';


COMMENT ON TABLE activity
IS '活动信息表';


COMMENT ON TABLE activity_template
IS '活动模板表';


COMMENT ON TABLE activity_student
IS '活动参与学生表';


COMMENT ON TABLE activity_archive
IS '活动档案表';


COMMENT ON TABLE score_rule
IS '综合测评规则表';


COMMENT ON TABLE score_apply
IS '学生自主申报表';


COMMENT ON TABLE score_record
IS '综合测评记录表';


COMMENT ON TABLE score_audit
IS '综合测评审核表';


COMMENT ON TABLE score_modify_log
IS '综合测评分数修改记录表';


COMMENT ON TABLE score_flow
IS '综合测评流水表';


COMMENT ON TABLE file_info
IS '文件信息表';


COMMENT ON TABLE notice_message
IS '通知消息表';


COMMENT ON TABLE system_notice
IS '系统公告表';


COMMENT ON TABLE operation_log
IS '系统操作日志表';