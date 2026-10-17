package com.student.studentscoresystem.vo;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;


/**
 * 批量审核结果
 *
 * 批量操作逐条独立处理，因此可能出现「部分成功、部分失败」的情况，
 * 这里统一返回成功条数、失败条数以及失败原因，便于前端友好提示。
 */
@Data
public class BatchAuditResultVO {


    /**
     * 本次批处理的总条数
     */
    private int total;


    /**
     * 成功条数
     */
    private int successCount;


    /**
     * 失败条数
     */
    private int failCount;


    /**
     * 失败原因（逐条）
     */
    private List<String> failures =
            new ArrayList<>();


    public void addSuccess() {

        this.successCount++;
    }


    public void addFailure(
            String message
    ) {

        this.failCount++;

        if (message != null) {

            this.failures.add(
                    message
            );
        }
    }


}
