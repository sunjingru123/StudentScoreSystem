package com.student.studentscoresystem.common.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.dto.ScoreApplyAddDTO;
import com.student.studentscoresystem.dto.ScoreApplyAuditDTO;
import com.student.studentscoresystem.entity.Activity;
import com.student.studentscoresystem.entity.ScoreApply;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.mapper.ActivityMapper;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.ScoreRuleMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.service.IScoreApplyService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreApplyVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/scoreApply")
public class ScoreApplyController {

    private final IScoreApplyService scoreApplyService;
    private final SysUserMapper sysUserMapper;
    private final ActivityMapper activityMapper;
    private final ScoreRuleMapper scoreRuleMapper;
    private final ScoreRecordMapper scoreRecordMapper;

    public ScoreApplyController(IScoreApplyService scoreApplyService,
                                SysUserMapper sysUserMapper,
                                ActivityMapper activityMapper,
                                ScoreRuleMapper scoreRuleMapper,
                                ScoreRecordMapper scoreRecordMapper) {
        this.scoreApplyService = scoreApplyService;
        this.sysUserMapper = sysUserMapper;
        this.activityMapper = activityMapper;
        this.scoreRuleMapper = scoreRuleMapper;
        this.scoreRecordMapper = scoreRecordMapper;
    }

    /**
     * 个人证书加分申报接口
     * 限制：仅学生可提交，无任何部门/干事/部长逻辑
     * 流程：登录学生 → 创建申请 status=0 applyType=CERTIFICATE → 等待档案部审核
     */
    @PostMapping("/add")
    public Result<Void> add(@RequestBody ScoreApplyAddDTO dto, HttpServletRequest request) {
        // 1. 解析当前登录用户ID
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.fail("请登录后再提交");
        }
        token = token.substring(7);
        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            return Result.fail("登录凭证失效，请重新登录");
        }
        Long currentUserId = claims.get("userId", Long.class);

        // 2. 校验当前用户是否存在
        SysUser loginUser = sysUserMapper.selectById(currentUserId);
        if (loginUser == null) {
            return Result.fail("用户不存在");
        }

        // 3. 基础参数校验（个人证书不需要activityId）
        if (dto.getRuleId() == null) {
            return Result.fail("请选择加分项目");
        }
        if (dto.getApplyScore() == null || dto.getApplyScore().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail("申报分值必须大于0");
        }
        if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
            return Result.fail("请填写证书获奖说明");
        }

        // 4. 校验加分规则是否正常启用
        ScoreRule rule = scoreRuleMapper.selectById(dto.getRuleId());
        if (rule == null || rule.getStatus() != 1) {
            return Result.fail("所选加分项目不存在或已停用");
        }

        // 5. 重复申报拦截：同一学生+同一加分规则+相同说明，待审/通过不可重复提交
        LambdaQueryWrapper<ScoreApply> dupWrapper = new LambdaQueryWrapper<>();
        dupWrapper.eq(ScoreApply::getStudentId, currentUserId);
        dupWrapper.eq(ScoreApply::getRuleId, dto.getRuleId());
        dupWrapper.eq(ScoreApply::getDescription, dto.getDescription().trim());
        dupWrapper.in(ScoreApply::getStatus, List.of((short) 0, (short) 1));
        Long dupCount = scoreApplyService.count(dupWrapper);
        if (dupCount > 0) {
            return Result.fail("该证书项目已提交，请勿重复申报");
        }

        // 6. 构建申报数据，标记申请类型为证书
        ScoreApply apply = new ScoreApply();
        apply.setStudentId(currentUserId);
        apply.setActivityId(null);
        apply.setApplyType("CERTIFICATE");
        apply.setRuleId(dto.getRuleId());
        apply.setApplyScore(dto.getApplyScore());
        apply.setMaterialFile(dto.getMaterialFile());
        apply.setDescription(dto.getDescription().trim());
        apply.setStatus((short) 0); // 0=待档案部审核
        apply.setCreateTime(LocalDateTime.now());
        apply.setUpdateTime(LocalDateTime.now());

        scoreApplyService.save(apply);
        return Result.success(null);
    }

    /**
     * 根据学生id查询该学生全部证书申请（管理员/档案部使用）
     */
    @GetMapping("/student/{studentId}")
    public Result<List<ScoreApplyVO>> studentApply(@PathVariable Long studentId) {
        List<ScoreApply> list = scoreApplyService.list(new LambdaQueryWrapper<ScoreApply>()
                .eq(ScoreApply::getStudentId, studentId)
                .orderByDesc(ScoreApply::getCreateTime));

        List<ScoreApplyVO> voList = list.stream().map(apply -> {
            ScoreApplyVO vo = new ScoreApplyVO();
            vo.setId(apply.getId());
            vo.setStudentId(apply.getStudentId());
            vo.setApplyType(apply.getApplyType());
            vo.setApplyScore(apply.getApplyScore());
            vo.setMaterialFile(apply.getMaterialFile());
            vo.setDescription(apply.getDescription());
            vo.setStatus(apply.getStatus());
            vo.setCreateTime(apply.getCreateTime());

            SysUser user = sysUserMapper.selectById(apply.getStudentId());
            Activity activity = apply.getActivityId() == null ? null : activityMapper.selectById(apply.getActivityId());
            ScoreRule rule = scoreRuleMapper.selectById(apply.getRuleId());

            if (user != null) vo.setStudentName(user.getRealName());
            if (activity != null) vo.setActivityName(activity.getName());
            if (rule != null) vo.setRuleName(rule.getName());
            return vo;
        }).toList();

        return Result.success(voList);
    }

    /**
     * 档案部待审核列表：只查询证书类待审申请，隔离其他类型单据
     */
    @GetMapping("/pending")
    public Result<List<ScoreApplyVO>> pending() {
        List<ScoreApply> list = scoreApplyService.list(new LambdaQueryWrapper<ScoreApply>()
                .eq(ScoreApply::getStatus, (short) 0)
                .eq(ScoreApply::getApplyType, "CERTIFICATE")
                .orderByDesc(ScoreApply::getCreateTime));

        List<ScoreApplyVO> voList = list.stream().map(apply -> {
            ScoreApplyVO vo = new ScoreApplyVO();
            vo.setId(apply.getId());
            vo.setStudentId(apply.getStudentId());
            vo.setApplyType(apply.getApplyType());
            vo.setApplyScore(apply.getApplyScore());
            vo.setMaterialFile(apply.getMaterialFile());
            vo.setDescription(apply.getDescription());
            vo.setStatus(apply.getStatus());
            vo.setCreateTime(apply.getCreateTime());

            SysUser user = sysUserMapper.selectById(apply.getStudentId());
            Activity activity = apply.getActivityId() == null ? null : activityMapper.selectById(apply.getActivityId());
            ScoreRule rule = scoreRuleMapper.selectById(apply.getRuleId());

            if (user != null) vo.setStudentName(user.getRealName());
            if (activity != null) vo.setActivityName(activity.getName());
            if (rule != null) vo.setRuleName(rule.getName());
            return vo;
        }).toList();

        return Result.success(voList);
    }

    /**
     * 当前登录学生查询自己所有证书申报
     */
    @GetMapping("/my")
    public Result<List<ScoreApplyVO>> my(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return Result.error("请先登录");
        }
        token = token.substring(7);
        Claims claims;
        try {
            claims = JwtUtil.parseToken(token);
        } catch (Exception e) {
            return Result.fail("登录已过期，请重新登录");
        }
        Long studentId = claims.get("userId", Long.class);

        List<ScoreApply> list = scoreApplyService.list(new LambdaQueryWrapper<ScoreApply>()
                .eq(ScoreApply::getStudentId, studentId)
                .orderByDesc(ScoreApply::getCreateTime));

        List<ScoreApplyVO> voList = list.stream().map(apply -> {
            ScoreApplyVO vo = new ScoreApplyVO();
            vo.setId(apply.getId());
            vo.setStudentId(apply.getStudentId());
            vo.setApplyType(apply.getApplyType());
            vo.setApplyScore(apply.getApplyScore());
            vo.setMaterialFile(apply.getMaterialFile());
            vo.setDescription(apply.getDescription());
            vo.setStatus(apply.getStatus());
            vo.setCreateTime(apply.getCreateTime());

            SysUser user = sysUserMapper.selectById(apply.getStudentId());
            Activity activity = apply.getActivityId() == null ? null : activityMapper.selectById(apply.getActivityId());
            ScoreRule rule = scoreRuleMapper.selectById(apply.getRuleId());

            if (user != null) vo.setStudentName(user.getRealName());
            if (activity != null) vo.setActivityName(activity.getName());
            if (rule != null) vo.setRuleName(rule.getName());
            return vo;
        }).toList();

        return Result.success(voList);
    }

    /**
     * 档案部审核通用接口，仅允许处理证书申请
     */
    @PostMapping("/audit")
    public Result<Void> audit(@RequestBody ScoreApplyAuditDTO dto) {
        ScoreApply apply = scoreApplyService.getById(dto.getId());
        if (apply == null) {
            return Result.error("申请不存在");
        }
        if (!Short.valueOf((short) 0).equals(apply.getStatus())) {
            return Result.error("该申请已经处理");
        }
        // 拦截非证书单据
        if (!"CERTIFICATE".equals(apply.getApplyType())) {
            return Result.error("该申请不是证书申报，请走部门活动审核流程");
        }

        // 驳回
        if (dto.getStatus() == 2) {
            apply.setStatus((short) 2);
            scoreApplyService.updateById(apply);
            return Result.success(null);
        }

        // 通过，生成分数记录
        if (dto.getStatus() == 1) {
            apply.setStatus((short) 1);
            scoreApplyService.updateById(apply);

            Long existRecord = scoreRecordMapper.selectCount(
                    new LambdaQueryWrapper<ScoreRecord>()
                            .eq(
                                    ScoreRecord::getSourceType,
                                    "CERTIFICATE"
                            )
                            .eq(
                                    ScoreRecord::getSourceId,
                                    apply.getId()
                            )
            );

            if (existRecord == 0) {
                ScoreRecord record = new ScoreRecord();

                record.setStudentId(apply.getStudentId());
                record.setRuleId(apply.getRuleId());
                record.setScore(apply.getApplyScore());
                record.setSourceType("CERTIFICATE");
                record.setSourceId(apply.getId());
                record.setAdminHidden((short) 0);
                record.setCreateTime(LocalDateTime.now());

                scoreRecordMapper.insert(record);
            }
        }
        return Result.success(null);
    }

    /**
     * 单独通过接口，仅允许处理证书申请
     */
    @PutMapping("/pass/{id}")
    public Result<Void> pass(@PathVariable Long id) {
        ScoreApply apply = scoreApplyService.getById(id);
        if (apply == null) {
            return Result.error("申请不存在");
        }
        if (!Short.valueOf((short) 0).equals(apply.getStatus())) {
            return Result.fail("仅待审核单据可操作");
        }
        // 拦截非证书单据
        if (!"CERTIFICATE".equals(apply.getApplyType())) {
            return Result.fail("该申请不是证书申报，请走部门活动审核流程");
        }

        apply.setStatus((short) 1);
        scoreApplyService.updateById(apply);

        Long existRecord = scoreRecordMapper.selectCount(
                new LambdaQueryWrapper<ScoreRecord>()
                        .eq(
                                ScoreRecord::getSourceType,
                                "CERTIFICATE"
                        )
                        .eq(
                                ScoreRecord::getSourceId,
                                apply.getId()
                        )
        );

        if (existRecord == 0) {
            ScoreRecord record = new ScoreRecord();

            record.setStudentId(apply.getStudentId());
            record.setRuleId(apply.getRuleId());
            record.setScore(apply.getApplyScore());
            record.setSourceType("CERTIFICATE");
            record.setSourceId(apply.getId());
            record.setAdminHidden((short) 0);
            record.setCreateTime(LocalDateTime.now());

            scoreRecordMapper.insert(record);
        }
        return Result.success(null);
    }

    /**
     * 单独驳回接口，仅允许处理证书申请
     */
    @PutMapping("/reject/{id}")
    public Result<Void> reject(@PathVariable Long id) {
        ScoreApply apply = scoreApplyService.getById(id);
        if (apply == null) {
            return Result.error("申请不存在");
        }
        if (!Short.valueOf((short) 0).equals(apply.getStatus())) {
            return Result.error("该申请已经处理，不能重复审核");
        }
        // 拦截非证书单据
        if (!"CERTIFICATE".equals(apply.getApplyType())) {
            return Result.fail("该申请不是证书申报，请走部门活动审核流程");
        }

        apply.setStatus((short) 2);
        scoreApplyService.updateById(apply);
        return Result.success(null);
    }
}