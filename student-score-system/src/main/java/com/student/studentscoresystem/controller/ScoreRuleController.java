package com.student.studentscoresystem.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.student.studentscoresystem.common.Result;
import com.student.studentscoresystem.entity.ScoreRule;
import com.student.studentscoresystem.service.IScoreRuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/scoreRule")
public class ScoreRuleController {

    private final IScoreRuleService scoreRuleService;

    public ScoreRuleController(IScoreRuleService scoreRuleService) {
        this.scoreRuleService = scoreRuleService;
    }

    @GetMapping("/list")
    public Result<List<ScoreRule>> list() {
        List<ScoreRule> rules = scoreRuleService.list(
                new LambdaQueryWrapper<ScoreRule>()
                        .orderByDesc(ScoreRule::getCreateTime)
        );
        return Result.success(rules);
    }

    @GetMapping("/{id}")
    public Result<ScoreRule> detail(@PathVariable Long id) {
        ScoreRule rule = scoreRuleService.getById(id);
        if (rule == null) {
            return Result.fail("规则不存在");
        }
        return Result.success(rule);
    }
}
