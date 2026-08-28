<template>
  <div class="department-audit-page">

    <el-card shadow="never">

      <!-- =========================
           页面头部
      ========================== -->
      <template #header>

        <div class="page-header">

          <div>

            <h2>
              部门加减分终审
            </h2>

            <p>
              审核部长 / 副部长初审通过的部门加减分申报
            </p>

          </div>


          <div class="header-actions">

            <span class="selected-text">
              已选择 {{ selectedRows.length }} 条
            </span>


            <!-- 一键终审通过 -->
            <el-button
              type="success"
              :disabled="selectedRows.length === 0"
              :loading="batchLoading"
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

        <!-- 多选 -->
        <el-table-column
          type="selection"
          width="55"
          align="center"
        />


        <!-- 序号 -->
        <el-table-column
          type="index"
          label="#"
          width="70"
          align="center"
        />


        <!-- 学生 -->
        <el-table-column
          prop="studentName"
          label="学生"
          min-width="120"
        >

          <template #default="{ row }">

            <div class="student-cell">

              <el-avatar :size="36">

                {{
                  row.studentName
                    ? row.studentName.substring(0, 1)
                    : '?'
                }}

              </el-avatar>

              <span>

                {{
                  row.studentName ||
                  '未知学生'
                }}

              </span>

            </div>

          </template>

        </el-table-column>


        <!-- 申报人 -->
        <el-table-column
          prop="applicantName"
          label="申报人"
          min-width="120"
        >

          <template #default="{ row }">

            {{ row.applicantName || '—' }}

          </template>

        </el-table-column>


        <!-- 部门 -->
        <el-table-column
          prop="departmentName"
          label="部门"
          min-width="130"
        >

          <template #default="{ row }">

            {{ row.departmentName || '—' }}

          </template>

        </el-table-column>


        <!-- 类型 -->
        <el-table-column
          label="类型"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              :type="
                Number(row.scoreType) === 1
                  ? 'success'
                  : 'danger'
              "
            >

              {{
                Number(row.scoreType) === 1
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
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <span
              :class="
                Number(row.scoreType) === 1
                  ? 'score-plus'
                  : 'score-minus'
              "
            >

              {{
                Number(row.scoreType) === 1
                  ? '+'
                  : '-'
              }}{{ row.score ?? 0 }}

            </span>

          </template>

        </el-table-column>


        <!-- 项目 -->
        <el-table-column
          prop="title"
          label="加减分项目"
          min-width="180"
        >

          <template #default="{ row }">

            {{ row.title || '—' }}

          </template>

        </el-table-column>


        <!-- 说明 -->
        <el-table-column
          prop="description"
          label="申请说明"
          min-width="220"
          show-overflow-tooltip
        >

          <template #default="{ row }">

            {{ row.description || '—' }}

          </template>

        </el-table-column>


        <!-- 证明材料 -->
        <el-table-column
          label="证明材料"
          width="120"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              v-if="row.evidenceUrl"
              type="primary"
              link
              @click="openEvidence(row.evidenceUrl)"
            >
              查看材料
            </el-button>

            <span
              v-else
              class="no-evidence"
            >
              无
            </span>

          </template>

        </el-table-column>


        <!-- 申请时间 -->
        <el-table-column
          prop="createTime"
          label="申请时间"
          width="180"
          align="center"
        />


        <!-- 状态 -->
        <el-table-column
          label="状态"
          width="130"
          align="center"
        >

          <template #default>

            <el-tag type="warning">
              待终审
            </el-tag>

          </template>

        </el-table-column>


        <!-- 操作 -->
        <el-table-column
          label="操作"
          width="190"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <!-- 通过 -->
            <el-button
              type="success"
              link
              :loading="processingId === row.id"
              :disabled="
                processingId !== null &&
                processingId !== row.id
              "
              @click="handleAudit(row, 1)"
            >

              <el-icon>
                <CircleCheck />
              </el-icon>

              通过

            </el-button>


            <!-- 驳回 -->
            <el-button
              type="danger"
              link
              :disabled="processingId !== null"
              @click="handleAudit(row, 2)"
            >

              <el-icon>
                <CircleClose />
              </el-icon>

              驳回

            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- 空数据 -->
      <el-empty
        v-if="
          !loading &&
          list.length === 0
        "
        description="暂无待终审的部门加减分申报"
      />

    </el-card>


    <!-- =========================
         终审意见弹窗
    ========================== -->
    <el-dialog
      v-model="dialogVisible"
      :title="
        auditStatus === 1
          ? '通过申请'
          : '驳回申请'
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
          @click="dialogVisible = false"
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

          确认

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
  CircleClose
} from '@element-plus/icons-vue'

import request from '@/utils/request'


/* =========================================================
   数据
========================================================= */

const list = ref([])

const selectedRows = ref([])

const tableRef = ref(null)

const loading = ref(false)

const batchLoading = ref(false)

const dialogVisible = ref(false)

const auditing = ref(false)

const currentApply = ref(null)

const auditStatus = ref(null)

const reviewRemark = ref('')

const processingId = ref(null)


/* =========================================================
   加载终审列表
========================================================= */

async function loadList() {

  loading.value = true

  try {

    console.log(
      '开始加载辅导员终审列表'
    )


    /*
     * ★★★ 核心 ★★★
     *
     * 这里不再调用：
     *
     * /departmentScoreApply/audit/list
     *
     * 而是调用：
     *
     * /departmentScoreApply/final-audit/list
     */

    const res =
      await request.get(
        '/departmentScoreApply/final-audit/list'
      )


    console.log(
      '终审列表响应：',
      res
    )


    if (
      res.data?.code !== 200
    ) {

      ElMessage.error(
        res.data?.message ||
        res.data?.msg ||
        '获取终审列表失败'
      )

      list.value = []

      return

    }


    const data =
      res.data?.data


    if (Array.isArray(data)) {

      list.value = data

    }

    else if (
      data &&
      Array.isArray(data.records)
    ) {

      list.value =
        data.records

    }

    else {

      list.value = []

    }


    selectedRows.value = []


    if (tableRef.value) {

      tableRef.value.clearSelection()

    }


    console.log(
      '当前待终审数据：',
      list.value
    )

  }

  catch (error) {

    console.error(
      '获取终审列表失败：',
      error
    )

    list.value = []

    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取终审列表失败'
    )

  }

  finally {

    loading.value = false

  }

}


/* =========================================================
   选择
========================================================= */

function handleSelectionChange(rows) {

  selectedRows.value =
    rows || []

}


/* =========================================================
   单条审核
========================================================= */

function handleAudit(
  row,
  status
) {

  currentApply.value = row

  auditStatus.value = status

  reviewRemark.value = ''

  dialogVisible.value = true

}


/* =========================================================
   提交终审
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
        ? '确定通过这条部门加减分申报吗？'
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

  }

  catch {

    return

  }


  const id =
    currentApply.value.id


  processingId.value =
    id

  auditing.value = true


  try {

    /*
     * ★★★ 这里才是终审接口 ★★★
     */

    const res =
      await request.put(

        `/departmentScoreApply/final-audit/${id}`,

        {
          status:
          auditStatus.value,

          reviewRemark:
            reviewRemark.value.trim()
        }

      )


    console.log(
      '终审响应：',
      res
    )


    if (
      res.data?.code === 200
    ) {

      ElMessage.success(

        auditStatus.value === 1
          ? '终审通过'
          : '已驳回'

      )


      dialogVisible.value =
        false


      currentApply.value =
        null


      await loadList()

    }

    else {

      ElMessage.error(

        res.data?.message ||
        res.data?.msg ||
        '终审失败'

      )

    }

  }

  catch (error) {

    console.error(
      '终审请求失败：',
      error
    )


    ElMessage.error(

      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '终审失败，请检查后端接口'

    )

  }

  finally {

    auditing.value = false

    processingId.value = null

  }

}


/* =========================================================
   一键终审通过
========================================================= */

async function batchPass() {

  /*
   * 直接从表格获取真正勾选的行
   */

  const rows =
    tableRef.value?.getSelectionRows?.() ||
    selectedRows.value ||
    []


  if (!rows.length) {

    ElMessage.warning(
      '请先勾选要通过的申请'
    )

    return

  }


  /*
   * 防止重复点击
   */

  if (batchLoading.value) {

    return

  }


  /*
   * 二次确认
   */

  try {

    await ElMessageBox.confirm(

      `确定通过选中的 ${rows.length} 条部门加减分申报吗？`,

      '一键终审',

      {
        confirmButtonText:
          '全部通过',

        cancelButtonText:
          '取消',

        type:
          'success'
      }

    )

  }

  catch {

    return

  }


  batchLoading.value = true


  let successCount = 0

  let failCount = 0


  try {

    /*
     * 一个一个调用后端终审接口
     */

    for (
      const row of rows
      ) {

      if (
        !row ||
        !row.id
      ) {

        failCount++

        continue

      }


      try {

        console.log(
          `开始终审申请：${row.id}`
        )


        const res =
          await request.put(

            `/departmentScoreApply/final-audit/${row.id}`,

            {
              status: 1,
              reviewRemark: ''
            }

          )


        console.log(
          `申请 ${row.id} 终审结果：`,
          res
        )


        if (
          res.data?.code === 200
        ) {

          successCount++

        }

        else {

          failCount++

          console.error(
            `申请 ${row.id} 终审失败：`,
            res.data
          )

        }

      }

      catch (error) {

        failCount++


        console.error(
          `申请 ${row.id} 请求失败：`,
          error
        )

      }

    }


    /*
     * 提示结果
     */

    if (
      successCount > 0 &&
      failCount === 0
    ) {

      ElMessage.success(
        `成功通过 ${successCount} 条申请`
      )

    }

    else if (
      successCount > 0 &&
      failCount > 0
    ) {

      ElMessage.warning(
        `成功通过 ${successCount} 条，失败 ${failCount} 条`
      )

    }

    else {

      ElMessage.error(
        `一键审核失败，共 ${failCount} 条`
      )

    }


    /*
     * 重新加载
     */

    await loadList()

  }

  finally {

    batchLoading.value = false

  }

}


/* =========================================================
   查看证明材料
========================================================= */

function openEvidence(url) {

  if (!url) {

    ElMessage.warning(
      '没有证明材料'
    )

    return

  }


  window.open(
    url,
    '_blank'
  )

}


/* =========================================================
   初始化
========================================================= */

onMounted(() => {

  loadList()

})

</script>


<style scoped>

.department-audit-page {

  width: 100%;

  padding: 30px;

  min-height:
    calc(100vh - 60px);

  background:
    #f5f7fa;

  box-sizing: border-box;

}


/* =========================
   头部
========================= */

.page-header {

  display: flex;

  justify-content:
    space-between;

  align-items:
    center;

}


.page-header h2 {

  margin:
    0 0 8px;

  font-size:
    24px;

  color:
    #303133;

}


.page-header p {

  margin: 0;

  color:
    #909399;

  font-size:
    14px;

}


/* =========================
   右侧按钮
========================= */

.header-actions {

  display: flex;

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


/* =========================
   学生
========================= */

.student-cell {

  display: flex;

  align-items:
    center;

  gap:
    10px;

}


.student-cell span {

  color:
    #303133;

  font-weight:
    500;

}


/* =========================
   分数
========================= */

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


/* =========================
   无材料
========================= */

.no-evidence {

  color:
    #c0c4cc;

}


/* =========================
   空数据
========================= */

.el-empty {

  padding:
    50px 0;

}


/* =========================
   响应式
========================= */

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

  }

}

</style>
