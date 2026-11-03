package com.student.studentscoresystem.service;

import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================
 * 贫困生名单导入权限判定
 * =========================================================
 *
 * 只有「指定的辅导员」可以导入贫困生名单。
 *
 * 判定依据：
 *   1. 系统管理员始终可以导入（便于运维）；
 *   2. 其他账号必须是辅导员端账号
 *      （辅导员 / 高级辅导员 / 教师），
 *      且姓名命中配置的允许名单
 *      （application.yml: app.poverty.import-names，默认「邹」）。
 *
 * 说明：
 *
 * 1. 不新增角色、不修改现有角色体系；
 * 2. 姓名允许名单来自配置，便于后续调整，不需要改代码；
 * 3. 前端只依据本判定控制按钮显示，
 *    真正的拦截始终由后端接口完成。
 * =========================================================
 */
@Component
public class PovertyImportPermissionService {

    private final SysUserMapper sysUserMapper;

    private final ScoreManagerPermissionService scoreManagerPermissionService;

    /**
     * 允许导入贫困生名单的辅导员姓名关键字
     *
     * 多个用英文逗号分隔。
     */
    private final List<String> allowedNameKeywords;

    public PovertyImportPermissionService(
            SysUserMapper sysUserMapper,
            ScoreManagerPermissionService scoreManagerPermissionService,
            @Value("${app.poverty.import-names:邹}")
            String allowedNames
    ) {

        this.sysUserMapper = sysUserMapper;

        this.scoreManagerPermissionService = scoreManagerPermissionService;

        this.allowedNameKeywords = parseNames(allowedNames);
    }

    /**
     * 当前用户是否可以导入贫困生名单
     */
    public boolean canImportPovertyList(Long userId) {

        if (userId == null) {

            return false;
        }


        if (scoreManagerPermissionService.isAdmin(userId)) {

            return true;
        }


        /*
         * 必须先是辅导员端账号，
         * 再校验姓名是否在允许名单内。
         */
        if (!scoreManagerPermissionService.isCounselor(userId)
                && !scoreManagerPermissionService.isSeniorCounselor(userId)) {

            return false;
        }


        SysUser user =
                sysUserMapper.selectById(userId);


        if (user == null || user.getRealName() == null) {

            return false;
        }


        String realName =
                user.getRealName().trim();


        for (String keyword : allowedNameKeywords) {

            if (!keyword.isEmpty() && realName.contains(keyword)) {

                return true;
            }
        }


        return false;
    }


    private List<String> parseNames(String raw) {

        List<String> names =
                new ArrayList<>();


        if (raw == null || raw.isBlank()) {

            return names;
        }


        for (String part : raw.split(",")) {

            String name =
                    part.trim();

            if (!name.isEmpty()) {

                names.add(name);
            }
        }


        return names;
    }
}