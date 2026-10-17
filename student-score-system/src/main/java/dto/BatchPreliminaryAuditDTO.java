package com.student.studentscoresystem.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


/**
 * 批量初审请求
 *
 * items 中每个元素对应一份证书申请的认定分值，
 * 初审通过时必须逐份填写，不能使用统一默认值。
 */
@Data
public class BatchPreliminaryAuditDTO {


    /**
     * 待初审的证书申请
     */
    private List<Item> items;


    @Data
    public static class Item {


        /**
         * 申请id
         */
        private Long id;


        /**
         * 档案部认定分值（初审通过时必填，且必须大于0）
         */
        private BigDecimal finalScore;


    }


}
