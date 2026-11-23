package com.student.studentscoresystem.utils;

import java.util.List;

/**
 * 组织职位与系统权限层级。
 *
 * 背景：
 *
 * 部门和社团可以使用不同的职位名称，例如：
 *
 *   部门   ：部长 / 副部长 / 站长 / 副站长
 *   大社团 ：理事长 / 副理事长 / 主席 / 副主席
 *   小社团 ：部长 / 副部长 / 主任 / 副主任
 *
 * 但同一层级的对应职位必须拥有完全一致的系统权限。
 *
 * 因此系统内部统一使用三个层级：
 *
 *   部长   —— 组织正职（部长、站长、理事长、主席、主任）
 *   副部长 —— 组织副职（副部长、副站长、副理事长、副主席、副主任）
 *   干事   —— 普通成员
 *
 * 权限判断一律看「层级」，界面展示一律看「职位名称」。
 * 这是全部组织权限判断的唯一来源，新增职位名称只需要改本类。
 */
public final class OrgPositionLevel {

    /** 组织正职 */
    public static final String LEVEL_LEADER = "部长";

    /** 组织副职 */
    public static final String LEVEL_DEPUTY = "副部长";

    /** 普通成员 */
    public static final String LEVEL_MEMBER = "干事";

    /** 组织类型：部门 */
    public static final short TYPE_DEPARTMENT = 1;

    /** 组织类型：大社团 */
    public static final short TYPE_BIG_CLUB = 2;

    /** 组织类型：小社团 */
    public static final short TYPE_SMALL_CLUB = 3;

    private static final List<String> LEADER_TITLES =
            List.of(
                    "部长",
                    "站长",
                    "理事长",
                    "主席",
                    "主任"
            );

    private static final List<String> DEPUTY_TITLES =
            List.of(
                    "副部长",
                    "副站长",
                    "副理事长",
                    "副主席",
                    "副主任"
            );

    private static final List<String> MEMBER_TITLES =
            List.of(
                    "干事"
            );

    /**
     * 部门可用的职位名称
     */
    private static final List<String> DEPARTMENT_TITLES =
            List.of(
                    "部长",
                    "副部长",
                    "站长",
                    "副站长",
                    "干事"
            );

    /**
     * 大社团可用的职位名称
     */
    private static final List<String> BIG_CLUB_TITLES =
            List.of(
                    "理事长",
                    "副理事长",
                    "主席",
                    "副主席",
                    "干事"
            );

    /**
     * 小社团可用的职位名称
     */
    private static final List<String> SMALL_CLUB_TITLES =
            List.of(
                    "部长",
                    "副部长",
                    "主任",
                    "副主任",
                    "干事"
            );

    private OrgPositionLevel() {
    }

    /**
     * 职位名称 -> 权限层级
     *
     * @return 无法识别时返回 null
     */
    public static String resolveLevel(String title) {
        if (title == null) {
            return null;
        }

        String value = title.trim();

        if (LEADER_TITLES.contains(value)) {
            return LEVEL_LEADER;
        }

        if (DEPUTY_TITLES.contains(value)) {
            return LEVEL_DEPUTY;
        }

        if (MEMBER_TITLES.contains(value)) {
            return LEVEL_MEMBER;
        }

        return null;
    }

    /**
     * 职位名称 -> 权限层级（无法识别时按普通成员处理）
     */
    public static String resolveLevelOrDefault(String title) {
        String level = resolveLevel(title);

        return level == null
                ? LEVEL_MEMBER
                : level;
    }

    /**
     * 取一条组织成员关系的权限层级
     *
     * 优先使用已经归一好的 positionLevel；
     * 历史数据（不包含该字段）则根据职位名称推算。
     */
    public static String levelOf(
            com.student.studentscoresystem.entity.SysUserDepartment relation
    ) {
        if (relation == null) {
            return null;
        }

        String level = relation.getPositionLevel();

        if (level != null && !level.isBlank()) {
            return level;
        }

        return resolveLevelOrDefault(
                relation.getPosition()
        );
    }

    /**
     * 是否为已知职位名称
     */
    public static boolean isValidTitle(String title) {
        return resolveLevel(title) != null;
    }

    /**
     * 职位名称是否适用于指定组织类型
     *
     * @param title   职位名称
     * @param orgType {@link #TYPE_DEPARTMENT} / {@link #TYPE_BIG_CLUB}
     *                / {@link #TYPE_SMALL_CLUB}
     */
    public static boolean isValidTitleForOrgType(
            String title,
            Integer orgType
    ) {
        if (title == null || orgType == null) {
            return false;
        }

        String value = title.trim();

        if (orgType == TYPE_BIG_CLUB) {
            return BIG_CLUB_TITLES.contains(value);
        }

        if (orgType == TYPE_SMALL_CLUB) {
            return SMALL_CLUB_TITLES.contains(value);
        }

        return DEPARTMENT_TITLES.contains(value);
    }

    /**
     * 组织类型允许的职位名称（用于错误提示）
     */
    public static String titlesOfOrgType(Integer orgType) {
        if (orgType != null && orgType == TYPE_BIG_CLUB) {
            return String.join("、", BIG_CLUB_TITLES);
        }

        if (orgType != null && orgType == TYPE_SMALL_CLUB) {
            return String.join("、", SMALL_CLUB_TITLES);
        }

        return String.join("、", DEPARTMENT_TITLES);
    }

    /**
     * 是否为正职（部长层级）
     */
    public static boolean isLeaderLevel(String level) {
        return LEVEL_LEADER.equals(level);
    }

    /**
     * 是否为正职或副职（可申报 / 可审核）
     */
    public static boolean isLeaderOrDeputy(String level) {
        return LEVEL_LEADER.equals(level)
                || LEVEL_DEPUTY.equals(level);
    }

    /**
     * 所有正职职位名称（用于 SQL 预过滤 / 兼容旧数据）
     */
    public static List<String> leaderTitles() {
        return LEADER_TITLES;
    }

    /**
     * 所有副职职位名称
     */
    public static List<String> deputyTitles() {
        return DEPUTY_TITLES;
    }

    /**
     * 所有正职 + 副职职位名称
     */
    public static List<String> leaderOrDeputyTitles() {
        return List.copyOf(
                concatLeaderAndDeputy()
        );
    }

    private static List<String> concatLeaderAndDeputy() {
        return java.util.stream.Stream
                .concat(
                        LEADER_TITLES.stream(),
                        DEPUTY_TITLES.stream()
                )
                .toList();
    }
}
