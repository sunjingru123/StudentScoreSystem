-- ==========================================
-- 基础岗位种子数据（幂等，可重复执行）
-- ==========================================
--
-- 说明：
--
-- 1. 本文件用于「初始化数据」，与 sql/02_create_table.sql（表结构）分开。
-- 2. 生产部署流程只会幂等执行 02_create_table.sql，
--    因此系统运行必需的岗位种子数据同时在 02_create_table.sql 中保留一份，
--    保证仅靠部署流程也能拿到新岗位。
-- 3. sys_position 没有 UNIQUE(name)，这里统一用 NOT EXISTS 保证幂等，
--    不会重复插入，也不会覆盖已有岗位配置。
-- 4. 只新增岗位，不改动任何已有岗位或用户岗位关系。

INSERT INTO public.sys_position (name, description, status)
SELECT '高级辅导员', '高级辅导员（可跨年级 / 部门 / 班级导出，并审核成绩删除申请）', 1
WHERE NOT EXISTS (
    SELECT 1 FROM public.sys_position WHERE name = '高级辅导员'
);
