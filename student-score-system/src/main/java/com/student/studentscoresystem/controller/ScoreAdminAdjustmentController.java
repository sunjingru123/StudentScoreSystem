package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreAdminAdjustment;
import com.student.studentscoresystem.entity.ScoreFlow;
import com.student.studentscoresystem.entity.ScoreRecord;
import com.student.studentscoresystem.entity.SysPosition;
import com.student.studentscoresystem.entity.SysUser;
import com.student.studentscoresystem.entity.SysUserPosition;
import com.student.studentscoresystem.dto.ScoreAdminAdjustmentAddDTO;
import com.student.studentscoresystem.mapper.ScoreRecordMapper;
import com.student.studentscoresystem.mapper.SysPositionMapper;
import com.student.studentscoresystem.mapper.SysUserMapper;
import com.student.studentscoresystem.mapper.SysUserPositionMapper;
import com.student.studentscoresystem.service.IScoreAdminAdjustmentService;
import com.student.studentscoresystem.service.IScoreFlowService;
import com.student.studentscoresystem.service.IScoreRecordService;
import com.student.studentscoresystem.utils.JwtUtil;
import com.student.studentscoresystem.vo.ScoreAdminAdjustmentVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/scoreAdjustment")
public class ScoreAdminAdjustmentController {

    private final IScoreAdminAdjustmentService adjustmentService;

    private final IScoreRecordService scoreRecordService;

    private final IScoreFlowService scoreFlowService;

    private final ScoreRecordMapper scoreRecordMapper;

    private final SysUserMapper sysUserMapper;

    private final SysUserPositionMapper userPositionMapper;

    private final SysPositionMapper positionMapper;

    public ScoreAdminAdjustmentController(
            IScoreAdminAdjustmentService adjustmentService,
            IScoreRecordService scoreRecordService,
            IScoreFlowService scoreFlowService,
            ScoreRecordMapper scoreRecordMapper,
            SysUserMapper sysUserMapper,
            SysUserPositionMapper userPositionMapper,
            SysPositionMapper positionMapper
    ) {

        this.adjustmentService =
                adjustmentService;

        this.scoreRecordService =
                scoreRecordService;

        this.scoreFlowService =
                scoreFlowService;

        this.scoreRecordMapper =
                scoreRecordMapper;

        this.sysUserMapper =
                sysUserMapper;

        this.userPositionMapper =
                userPositionMapper;

        this.positionMapper =
                positionMapper;
    }

    /**
     * =========================================================
     * 获取当前登录用户 ID
     * =========================================================
     */
    private Long getUserId(
            HttpServletRequest request
    ) {

        String token =
                request.getHeader("Authorization");

        if (
                token == null
                        || !token.startsWith("Bearer ")
        ) {
            return null;
        }

        token =
                token.substring(7);

        try {

            Claims claims =
                    JwtUtil.parseToken(token);

            return claims.get(
                    "userId",
                    Long.class
            );

        } catch (Exception e) {

            return null;
        }
    }

    /**
     * =========================================================
     * 判断管理员权限
     * =========================================================
     */
    private boolean isAdmin(
            Long userId
    ) {

        if (userId == null) {
            return false;
        }

        SysPosition adminPosition =
                positionMapper.selectOne(
                        new LambdaQueryWrapper<SysPosition>()
                                .eq(
                                        SysPosition::getName,
                                        "管理员"
                                )
                );

        if (adminPosition == null) {
            return false;
        }

        Long count =
                userPositionMapper.selectCount(
                        new LambdaQueryWrapper<SysUserPosition>()
                                .eq(
                                        SysUserPosition::getUserId,
                                        userId
                                )
                                .eq(
                                        SysUserPosition::getPositionId,
                                        adminPosition.getId()
                                )
                );

        return count != null
                && count > 0;
    }

    /**
     * =========================================================
     * 管理员新增成绩调整
     * =========================================================
     *
     * 流程：
     *
     * 管理员
     *   ↓
     * 权限校验
     *   ↓
     * 保存 ScoreAdminAdjustment
     *   ↓
     * 生成 ScoreRecord
     *   ↓
     * 生成 ScoreFlow
     *
     * adjustType：
     *
     * 1  = 加分
     * -1 = 减分
     */
    @PostMapping("/add")
    public Result<Void> add(
            @RequestBody ScoreAdminAdjustmentAddDTO dto,
            HttpServletRequest request
    ) {

        /*
         * =====================================================
         * 1. 当前管理员
         * =====================================================
         */

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        /*
         * =====================================================
         * 2. 管理员权限
         * =====================================================
         */

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        /*
         * =====================================================
         * 3. 参数校验
         * =====================================================
         */

        if (dto == null) {
            return Result.error("调整参数不能为空");
        }

        if (dto.getStudentId() == null) {
            return Result.error("请选择学生");
        }

        if (dto.getAdjustType() == null) {
            return Result.error("请选择调整类型");
        }

        if (
                dto.getAdjustType() != 1
                        && dto.getAdjustType() != -1
        ) {
            return Result.error("调整类型错误");
        }

        if (
                dto.getScore() == null
                        || dto.getScore()
                        .compareTo(BigDecimal.ZERO) <= 0
        ) {
            return Result.error(
                    "调整分数必须大于0"
            );
        }

        /*
         * =====================================================
         * 4. 学生存在性
         * =====================================================
         */

        SysUser student =
                sysUserMapper.selectById(
                        dto.getStudentId()
                );

        if (student == null) {
            return Result.error("学生不存在");
        }

        if (
                student.getStatus() != null
                        && !Short.valueOf((short) 1)
                        .equals(student.getStatus())
        ) {
            return Result.error(
                    "该学生当前不是正常状态"
            );
        }

        /*
         * =====================================================
         * 5. 创建管理员调整记录
         * =====================================================
         */

        ScoreAdminAdjustment adjustment =
                new ScoreAdminAdjustment();

        adjustment.setStudentId(
                dto.getStudentId()
        );

        adjustment.setAdminId(
                adminId
        );

        adjustment.setAdjustType(
                dto.getAdjustType()
        );

        adjustment.setScore(
                dto.getScore()
        );

        adjustment.setReason(
                dto.getReason()
        );

        adjustment.setCreateTime(
                LocalDateTime.now()
        );

        adjustmentService.save(
                adjustment
        );

        /*
         * =====================================================
         * 6. 计算真正进入成绩表的分数
         * =====================================================
         */

        BigDecimal realScore =
                dto.getScore();

        if (dto.getAdjustType() == -1) {

            realScore =
                    realScore.negate();
        }

        /*
         * =====================================================
         * 7. 防止重复生成 ScoreRecord
         * =====================================================
         *
         * 一次管理员调整对应一个 adjustmentId。
         *
         * ScoreRecord：
         *
         * sourceType = ADMIN_ADJUSTMENT
         * sourceId   = adjustment.id
         */

        Long existRecord =
                scoreRecordMapper.selectCount(
                        new LambdaQueryWrapper<ScoreRecord>()
                                .eq(
                                        ScoreRecord::getSourceType,
                                        "ADMIN_ADJUSTMENT"
                                )
                                .eq(
                                        ScoreRecord::getSourceId,
                                        adjustment.getId()
                                )
                );

        if (existRecord != null
                && existRecord > 0) {

            return Result.success(null);
        }

        /*
         * =====================================================
         * 8. 创建 ScoreRecord
         * =====================================================
         */

        ScoreRecord record =
                new ScoreRecord();

        record.setStudentId(
                dto.getStudentId()
        );

        /*
         * 管理员调整没有对应规则
         */
        record.setRuleId(null);

        record.setScore(
                realScore
        );

        /*
         * 有效
         */
        record.setStatus(
                (short) 1
        );

        /*
         * 正常显示
         */
        record.setAdminHidden(
                (short) 0
        );

        record.setSemesterId(
                null
        );

        record.setSourceType(
                "ADMIN_ADJUSTMENT"
        );

        record.setSourceId(
                adjustment.getId()
        );

        record.setCreateTime(
                LocalDateTime.now()
        );

        scoreRecordService.save(
                record
        );

        /*
         * =====================================================
         * 9. 计算调整前成绩
         * =====================================================
         *
         * 注意：
         *
         * 当前刚插入的 record 也会被查询出来，
         * 所以这里最后要减掉本次 realScore。
         */

        BigDecimal beforeScore =
                scoreRecordService.list(
                                new LambdaQueryWrapper<ScoreRecord>()
                                        .eq(
                                                ScoreRecord::getStudentId,
                                                dto.getStudentId()
                                        )
                                        .eq(
                                                ScoreRecord::getStatus,
                                                (short) 1
                                        )
                                        .eq(
                                                ScoreRecord::getAdminHidden,
                                                (short) 0
                                        )
                        )
                        .stream()
                        .map(
                                ScoreRecord::getScore
                        )
                        .filter(
                                score -> score != null
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        )
                        .subtract(
                                realScore
                        );

        /*
         * =====================================================
         * 10. 调整后成绩
         * =====================================================
         */

        BigDecimal afterScore =
                beforeScore.add(
                        realScore
                );

        /*
         * =====================================================
         * 11. 创建 ScoreFlow
         * =====================================================
         */

        ScoreFlow flow =
                new ScoreFlow();

        flow.setStudentId(
                dto.getStudentId()
        );

        flow.setBeforeScore(
                beforeScore
        );

        flow.setChangeScore(
                realScore
        );

        flow.setAfterScore(
                afterScore
        );

        flow.setChangeType(
                "ADMIN_ADJUSTMENT"
        );

        String description =
                dto.getReason();

        if (
                description == null
                        || description.trim().isEmpty()
        ) {

            description =
                    "管理员成绩调整";
        }

        flow.setDescription(
                description.trim()
        );

        flow.setCreateTime(
                LocalDateTime.now()
        );

        scoreFlowService.save(
                flow
        );

        return Result.success(null);
    }

    /**
     * =========================================================
     * 管理员查看全部成绩调整
     * =========================================================
     */
    @GetMapping("/list")
    public Result<List<ScoreAdminAdjustmentVO>> list(
            HttpServletRequest request
    ) {

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        List<ScoreAdminAdjustment> list =
                adjustmentService.list(
                        new LambdaQueryWrapper<ScoreAdminAdjustment>()
                                .orderByDesc(
                                        ScoreAdminAdjustment::getCreateTime
                                )
                );

        return Result.success(
                convertVO(list)
        );
    }

    /**
     * =========================================================
     * 管理员查看某个学生的调整记录
     * =========================================================
     */
    @GetMapping("/student/{studentId}")
    public Result<List<ScoreAdminAdjustmentVO>> student(
            @PathVariable Long studentId,
            HttpServletRequest request
    ) {

        Long adminId =
                getUserId(request);

        if (adminId == null) {
            return Result.error("请先登录");
        }

        if (!isAdmin(adminId)) {
            return Result.error("没有管理员权限");
        }

        SysUser student =
                sysUserMapper.selectById(
                        studentId
                );

        if (student == null) {
            return Result.error("学生不存在");
        }

        List<ScoreAdminAdjustment> list =
                adjustmentService.list(
                        new LambdaQueryWrapper<ScoreAdminAdjustment>()
                                .eq(
                                        ScoreAdminAdjustment::getStudentId,
                                        studentId
                                )
                                .orderByDesc(
                                        ScoreAdminAdjustment::getCreateTime
                                )
                );

        return Result.success(
                convertVO(list)
        );
    }

    /**
     * =========================================================
     * Entity → VO
     * =========================================================
     */
    private List<ScoreAdminAdjustmentVO> convertVO(
            List<ScoreAdminAdjustment> list
    ) {

        return list.stream()
                .map(item -> {

                    ScoreAdminAdjustmentVO vo =
                            new ScoreAdminAdjustmentVO();

                    vo.setId(
                            item.getId()
                    );

                    vo.setStudentId(
                            item.getStudentId()
                    );

                    vo.setAdminId(
                            item.getAdminId()
                    );

                    vo.setAdjustType(
                            item.getAdjustType()
                    );

                    vo.setScore(
                            item.getScore()
                    );

                    vo.setReason(
                            item.getReason()
                    );

                    vo.setCreateTime(
                            item.getCreateTime()
                    );

                    /*
                     * 学生
                     */
                    SysUser student =
                            sysUserMapper.selectById(
                                    item.getStudentId()
                            );

                    if (student != null) {

                        vo.setStudentName(
                                student.getRealName()
                        );

                        vo.setStudentNo(
                                student.getStudentNo()
                        );
                    }

                    /*
                     * 管理员
                     */
                    SysUser admin =
                            sysUserMapper.selectById(
                                    item.getAdminId()
                            );

                    if (admin != null) {

                        vo.setAdminName(
                                admin.getRealName()
                        );
                    }

                    return vo;
                })
                .toList();
    }
}