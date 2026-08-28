<template>
  <div class="department-final-audit-page">

    <el-card shadow="never">

      <!-- =========================
           页面头部
      ========================== -->
      <template #header>

        <div class="page-header">

          <div>
            <h2>部门加减分终审</h2>
            <p>审核部门干部初审通过的加减分申报</p>
          </div>

          <div class="header-actions">

            <!-- 已选择 -->
            <span class="selected-text">
              已选择 {{ selectedRows.length }} 条
            </span>

            <!-- =========================
                 一键通过
            ========================== -->
            <el-button
              type="success"
              :disabled="selectedRows.length === 0"
              :loading="batchAuditing"
              @click="batchPass"
            >
              <el-icon>
                <CircleCheck />
              </el-icon>

              一键通过
            </el-button>

            <!-- 刷新 -->
            <el-button
              :loading="loading"
              @click="loadList"
            >
              <el-icon>
                <Refresh />
              </el-icon>

              刷新
            </el-button>

          </div>

        </div>

      </template>


      <!-- =========================
           终审列表
      ========================== -->
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="list"
        row-key="id"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >

        <!-- =========================
             多选框
        ========================== -->
        <el-table-column
          type="selection"
          width="55"
          align="center"
        />


        <!-- 学生 -->
        <el-table-column
          prop="studentName"
          label="学生"
          min-width="110"
        />


        <!-- 申报人 -->
        <el-table-column
          prop="applicantName"
          label="申报人"
          min-width="110"
        />


        <!-- 部门 -->
        <el-table-column
          prop="departmentName"
          label="部门"
          min-width="120"
        />


        <!-- 类型 -->
        <el-table-column
          label="类型"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              :type="
                row.scoreType === 1
                  ? 'success'
                  : 'danger'
              "
            >

              {{
                row.scoreType === 1
                  ? '加分'
                  : '减分'
              }}

            </el-tag>

          </template>

        </el-table-column>


        <!-- 分值 -->
        <el-table-column
          prop="score"
          label="分值"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <span
              :class="
                row.scoreType === 1
                  ? 'score-plus'
                  : 'score-minus'
              "
            >

              {{
                row.scoreType === 1
                  ? '+'
                  : '-'
              }}{{ row.score }}

            </span>

          </template>

        </el-table-column>


        <!-- 项目 -->
        <el-table-column
          prop="title"
          label="项目"
          min-width="180"
        />


        <!-- 项目说明 -->
        <el-table-column
          prop="description"
          label="项目说明"
          min-width="220"
          show-overflow-tooltip
        />


        <!-- 证明材料 -->
        <el-table-column
          prop="evidenceUrl"
          label="证明材料"
          min-width="120"
          align="center"
        >

          <template #default="{ row }">

            <el-link
              v-if="row.evidenceUrl"
              :href="row.evidenceUrl"
              target="_blank"
              type="primary"
            >
              查看材料
            </el-link>

            <span v-else>
              无
            </span>

          </template>

        </el-table-column>


        <!-- 申报时间 -->
        <el-table-column
          prop="createTime"
          label="申报时间"
          min-width="170"
        />


        <!-- =========================
             操作
        ========================== -->
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <!-- 单条通过 -->
            <el-button
              type="success"
              size="small"
              :loading="
                auditingId === row.id
              "
              @click="
                handleAudit(row, 1)
              "
            >
              通过
            </el-button>


            <!-- 单条驳回 -->
            <el-button
              type="danger"
              size="small"
              :disabled="
                auditingId === row.id
              "
              @click="
                handleAudit(row, 2)
              "
            >
              驳回
            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- =========================
           空数据
      ========================== -->
      <el-empty
        v-if="
          !loading &&
          list.length === 0
        "
        description="暂无待终审的部门申报"
      />

    </el-card>


    <!-- =========================
         终审意见弹窗
    ========================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="
        auditStatus === 1
          ? '通过终审'
          : '驳回终审'
      "
      width="500px"
    >

      <el-form>

        <el-form-item label="审核意见">

          <el-input
            v-model="reviewRemark"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            :placeholder="
              auditStatus === 1
                ? '请输入审核意见（可不填）'
                : '请输入驳回原因'
            "
          />

        </el-form-item>

      </el-form>


      <template #footer>

        <el-button
          @click="
            dialogVisible = false
          "
        >
          取消
        </el-button>


        <el-button
          :type="
            auditStatus === 1
              ? 'success'
              : 'danger'
          "
          :loading="auditing"
          @click="submitAudit"
        >

          {{
            auditStatus === 1
              ? '确认通过'
              : '确认驳回'
          }}

        </el-button>

      </template>

    </el-dialog>

  </div>
</template>


<script setup>

import {
  ref,
  onMounted
} from 'vue'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  CircleCheck,
  Refresh
} from '@element-plus/icons-vue'

import request from '@/api/request'


/* =========================================================
   数据
========================================================= */

const list = ref([])

/*
 * 当前选中的行
 */
const selectedRows = ref([])

/*
 * 表格引用
 */
const tableRef = ref(null)

/*
 * 页面加载
 */
const loading = ref(false)

/*
 * 批量审核加载
 */
const batchAuditing = ref(false)

/*
 * 单条审核加载
 */
const auditing = ref(false)

/*
 * 当前正在审核的 ID
 */
const auditingId = ref(null)

/*
 * 弹窗
 */
const dialogVisible = ref(false)

/*
 * 当前审核申请
 */
const currentApply = ref(null)

/*
 * 当前审核状态
 *
 * 1 = 通过
 * 2 = 驳回
 */
const auditStatus = ref(null)

/*
 * 审核意见
 */
const reviewRemark = ref('')


/* =========================================================
   加载终审列表
========================================================= */

async function loadList() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/departmentScoreApply/final-audit/list'
      )

    console.log(
      '=============================='
    )

    console.log(
      '部门加减分终审列表响应：',
      res
    )

    console.log(
      '终审列表数据：',
      res.data?.data
    )

    console.log(
      '=============================='
    )


    if (
      res.data?.code === 200
    ) {

      list.value =
        Array.isArray(res.data.data)
          ? res.data.data
          : []


      /*
       * 重新加载以后清空选择
       */
      selectedRows.value = []


      /*
       * 清空 Element Plus 表格勾选
       */
      if (tableRef.value) {

        tableRef.value.clearSelection()

      }

    } else {

      ElMessage.error(
        res.data?.message ||
        '获取终审列表失败'
      )

    }

  } catch (error) {

    console.error(
      '获取终审列表失败：',
      error
    )

    list.value = []

    ElMessage.error(
      error?.response?.data?.message ||
      '获取终审列表失败'
    )

  } finally {

    loading.value = false

  }

}


/* =========================================================
   多选
========================================================= */

function handleSelectionChange(rows) {

  selectedRows.value =
    Array.isArray(rows)
      ? rows
      : []


  console.log(
    '当前选择的终审申请：',
    selectedRows.value
  )

  console.log(
    '当前选择数量：',
    selectedRows.value.length
  )

}


/* =========================================================
   打开单条审核
========================================================= */

function handleAudit(row, status) {

  currentApply.value = row

  auditStatus.value = status

  reviewRemark.value = ''

  dialogVisible.value = true

}


/* =========================================================
   提交单条终审
========================================================= */

async function submitAudit() {

  if (!currentApply.value) {

    return

  }


  /*
   * 驳回必须填写原因
   */

  if (
    auditStatus.value === 2 &&
    !reviewRemark.value.trim()
  ) {

    ElMessage.warning(
      '请输入驳回原因'
    )

    return

  }


  /*
   * 二次确认
   */

  try {

    await ElMessageBox.confirm(

      auditStatus.value === 1

        ? '确定通过这条部门加减分申报吗？通过后将正式计入学生综合测评成绩。'

        : '确定驳回这条部门加减分申报吗？',

      '确认终审',

      {
        confirmButtonText:
          auditStatus.value === 1
            ? '确定通过'
            : '确定驳回',

        cancelButtonText:
          '取消',

        type:
          auditStatus.value === 1
            ? 'success'
            : 'warning'

      }

    )

  } catch {

    return

  }


  auditing.value = true

  auditingId.value =
    currentApply.value.id


  try {

    const res =
      await request.put(

        `/departmentScoreApply/final-audit/${currentApply.value.id}`,

        {
          status:
          auditStatus.value,

          reviewRemark:
            reviewRemark.value.trim()
        }

      )


    console.log(
      '单条终审响应：',
      res
    )


    if (
      res.data?.code === 200
    ) {

      ElMessage.success(

        auditStatus.value === 1

          ? '终审通过，已计入综合测评成绩'

          : '终审已驳回'

      )


      dialogVisible.value =
        false

      currentApply.value =
        null

      await loadList()

    } else {

      ElMessage.error(

        res.data?.message ||
        '终审失败'

      )

    }

  } catch (error) {

    console.error(
      '终审失败：',
      error
    )

    ElMessage.error(

      error?.response?.data?.message ||
      '终审失败'

    )

  } finally {

    auditing.value = false

    auditingId.value = null

  }

}


/* =========================================================
   一键通过
========================================================= */

async function batchPass() {

  /*
   * 直接从表格获取选中的行
   *
   * 这样即使 selectedRows 没同步，
   * 也能正常获取用户勾选的数据。
   */

  const rows =
    tableRef.value?.getSelectionRows?.() || []


  console.log(
    '================================'
  )

  console.log(
    '点击一键通过'
  )

  console.log(
    '当前选中行：',
    rows
  )

  console.log(
    '当前选中数量：',
    rows.length
  )

  console.log(
    '================================'
  )


  /*
   * 没有选择
   */

  if (rows.length === 0) {

    ElMessage.warning(
      '请先勾选要通过的申请'
    )

    return

  }


  /*
   * 二次确认
   */

  try {

    await ElMessageBox.confirm(

      `确定通过选中的 ${rows.length} 条部门加减分申报吗？通过后将正式计入学生综合测评成绩。`,

      '一键通过',

      {
        confirmButtonText:
          '全部通过',

        cancelButtonText:
          '取消',

        type:
          'success'

      }

    )

  } catch {

    return

  }


  /*
   * 开始批量审核
   */

  batchAuditing.value =
    true


  let successCount = 0

  let failCount = 0


  try {

    /*
     * 逐条调用现有终审接口
     */
    for (const row of rows) {

      console.log(
        '正在终审通过：',
        row
      )


      try {

        const res =
          await request.put(

            `/departmentScoreApply/final-audit/${row.id}`,

            {
              status: 1,

              reviewRemark: ''
            }

          )


        console.log(
          `申请 ${row.id} 终审响应：`,
          res
        )


        if (
          res.data?.code === 200
        ) {

          successCount++

        } else {

          failCount++

          console.error(
            `申请 ${row.id} 终审失败：`,
            res.data
          )

        }

      } catch (error) {

        failCount++

        console.error(
          `申请 ${row.id} 终审请求失败：`,
          error
        )

      }

    }


    /*
     * 显示结果
     */

    if (
      successCount > 0
    ) {

      ElMessage.success(
        `成功通过 ${successCount} 条申请`
      )

    }


    if (
      failCount > 0
    ) {

      ElMessage.warning(
        `${failCount} 条申请审核失败`
      )

    }


    /*
     * 重新加载
     */

    await loadList()

  } finally {

    batchAuditing.value =
      false

  }

}


/* =========================================================
   初始化
========================================================= */

onMounted(() => {

  loadList()

})

</script>


<style scoped>

.department-final-audit-page {

  padding:
    30px;

  min-height:
    calc(100vh - 60px);

  background:
    #f5f7fa;

}


/* =========================================================
   页面头部
========================================================= */

.page-header {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    center;

}


.page-header h2 {

  margin:
    0 0 8px;

}


.page-header p {

  margin:
    0;

  color:
    #909399;

  font-size:
    14px;

}


/* =========================================================
   头部按钮区域
========================================================= */

.header-actions {

  display:
    flex;

  align-items:
    center;

  gap:
    12px;

}


.selected-text {

  color:
    #909399;

  font-size:
    14px;

}


/* =========================================================
   分值
========================================================= */

.score-plus {

  color:
    #67c23a;

  font-weight:
    bold;

}


.score-minus {

  color:
    #f56c6c;

  font-weight:
    bold;

}


/* =========================================================
   响应式
========================================================= */

@media (max-width: 900px) {

  .page-header {

    flex-direction:
      column;

    align-items:
      stretch;

    gap:
      15px;

  }


  .header-actions {

    justify-content:
      flex-end;

    flex-wrap:
      wrap;

  }

}

</style>
