package com.student.studentscoresystem.common;

import java.math.BigDecimal;
import java.util.List;

/**
 * =========================================================
 * 综合测评成绩统一计算器
 *
 * 全系统统一口径：
 *
 *   baseLimit   = 40
 *   bonusScore  = 所有正分之和
 *   deductScore = 所有负分的绝对值之和
 *   actualLimit = max(0, baseLimit - deductScore)
 *   totalScore  = min(bonusScore, actualLimit)
 *
 * 个人中心、辅导员端、管理员端、导出报表必须全部使用本类，
 * 杜绝一处 40 + bonus - deduct、
 * 另一处 min(bonus, 40 - deduct) 的分裂现象。
 * =========================================================
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    /**
     * 统一计算综合评分。
     *
     * @param scores 成绩明细，允许包含 null（按 0 处理）
     */
    public static Summary summarize(
            List<BigDecimal> scores
    ) {

        BigDecimal bonus =
                BigDecimal.ZERO;

        BigDecimal deduct =
                BigDecimal.ZERO;

        if (scores != null) {

            for (BigDecimal score : scores) {

                /*
                 * 空值防御：null 视为 0，跳过。
                 */
                if (score == null) {

                    continue;
                }

                int cmp =
                        score.compareTo(
                                BigDecimal.ZERO
                        );

                if (cmp > 0) {

                    bonus =
                            bonus.add(score);

                } else if (cmp < 0) {

                    deduct =
                            deduct.add(
                                    score.abs()
                            );
                }
            }
        }

        BigDecimal baseLimit =
                ScoreConstants.MAX_SCORE;

        BigDecimal actualLimit =
                baseLimit.subtract(
                        deduct
                );

        if (actualLimit.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            actualLimit =
                    BigDecimal.ZERO;
        }

        BigDecimal total =
                bonus.min(
                        actualLimit
                );

        return new Summary(
                baseLimit,
                bonus,
                deduct,
                actualLimit,
                total
        );
    }

    /**
     * 统一计算结果
     */
    public static final class Summary {

        private final BigDecimal baseLimit;

        private final BigDecimal bonusScore;

        private final BigDecimal deductScore;

        private final BigDecimal actualLimit;

        private final BigDecimal totalScore;

        public Summary(
                BigDecimal baseLimit,
                BigDecimal bonusScore,
                BigDecimal deductScore,
                BigDecimal actualLimit,
                BigDecimal totalScore
        ) {

            this.baseLimit = baseLimit;
            this.bonusScore = bonusScore;
            this.deductScore = deductScore;
            this.actualLimit = actualLimit;
            this.totalScore = totalScore;
        }

        public BigDecimal getBaseLimit() {

            return baseLimit;
        }

        public BigDecimal getBonusScore() {

            return bonusScore;
        }

        public BigDecimal getDeductScore() {

            return deductScore;
        }

        public BigDecimal getActualLimit() {

            return actualLimit;
        }

        public BigDecimal getTotalScore() {

            return totalScore;
        }
    }
}
