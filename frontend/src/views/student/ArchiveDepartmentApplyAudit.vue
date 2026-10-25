<template>
  <div class="audit-page">

    <!-- =====================================================
         页面标题
    ====================================================== -->
    <el-card class="title-card">

      <div class="title-box">

        <div>

          <h2>
            部门加减分申报审核
          </h2>

          <p>
            档案部部长 / 副部长审核各部门初审通过的加减分申报，
            通过后才进入辅导员终审
          </p>

        </div>

        <el-button
          type="primary"
          :icon="Refresh"
          :loading="loading"
          @click="loadList"
        >
          刷新
        </el-button>

      </div>

    </el-card>


    <!-- =====================================================
         统计
    ====================================================== -->
    <div class="statistics">

      <el-card class="stat-card">

        <div class="stat-icon pending">

          <el-icon size="28">
            <Clock />
          </el-icon>

        </div>

        <div class="stat-content">

          <span>
            待档案部审核
          </span>

          <strong>
            {{ list.length }}
          </strong>

        </div>

      </el-card>


      <el-card class="stat-card">

        <div class="stat-icon selected">

          <el-icon size="28">
            <Checked />
          </el-icon>

        </div>

        <div class="stat-content">

          <span>
            已通过
          </span>

          <strong>
            {{ processedCount }}
          </strong>

        </div>

      </el-card>

    </div>


    <!-- =====================================================
         待审核列表
    ====================================================== -->
    <el-card class="table-card">

      <template #header>

        <div class="toolbar">

          <el-input
            v-model="keyword"
            placeholder="搜索学生姓名 / 部门 / 项目"
            clearable
            style="width: 300px"
          >

            <template #prefix>

              <el-icon>
                <Search />
              </el-icon>

            </template>

          </el-input>


          <div class="toolbar-right">

            <el-button
              type="success"
              :disabled="
                list.length === 0 ||
                batchLoading ||
                processingId !== null
              "
              :loading="batchLoading"
              @click="batchPass"
            >

              <el-icon>
                <CircleCheck />
              </el-icon>

              一键通过全部

            </el-button>

          </div>

        </div>

      </template>


      <!-- =====================================================
           表格
      ====================================================== -->
      <el-table
        v-loading="loading"
        :data="filteredList"
        row-key="id"
        border
        stripe
        style="width: 100%"
      >

        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
        />


        <!-- 学生 -->
        <el-table-column
          prop="studentName"
          label="学生"
          min-width="130"
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
              class="score"
              :class="
                Number(row.scoreType) === 1
                  ? 'score-add'
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
              待档案部审核
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

            <el-button
              type="success"
              link
              :loading="processingId === row.id && !rejectLoading"
              :disabled="
                processingId !== null ||
                batchLoading
              "
              @click="pass(row)"
            >

              <el-icon>
                <CircleCheck />
              </el-icon>

              通过

            </el-button>


            <el-button
              type="danger"
              link
              :loading="processingId === row.id && rejectLoading"
              :disabled="
                processingId !== null ||
                batchLoading
              "
              @click="reject(row)"
            >

              <el-icon>
                <CircleClose />
              </el-icon>

              驳回

            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- =====================================================
           空数据
      ====================================================== -->
      <el-empty
        v-if="
          !loading &&
          filteredList.length === 0
        "
        description="暂无待档案部审核的部门申报"
      />

    </el-card>

  </div>
</template>


<script setup>

import {
  ref,
  computed,
  onMounted,
  onBeforeUnmount
} from 'vue'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  Clock,
  Checked,
  CircleCheck,
  CircleClose,
  Search,
  Refresh
} from '@element-plus/icons-vue'

import request from '@/utils/request'

import {
  openProtectedFile,
  releaseAllProtectedFileUrls
} from '@/utils/filePreview'


/* =========================================================
   数据
========================================================= */

const list = ref([])

const keyword = ref('')

const loading = ref(false)

const batchLoading = ref(false)

const processingId = ref(null)

const rejectLoading = ref(false)

const processedCount = ref(0)


/* =========================================================
   获取待审核列表
========================================================= */

async function loadList() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/departmentScoreApply/archive-audit/list'
      )


    /*
     * =====================================================
     * 统一处理后端 Result
     * =====================================================
     */

    if (
      !res ||
      Number(res.code) !== 200
    ) {

      throw new Error(
        res?.message ||
        res?.msg ||
        '部门申报审核接口返回失败'
      )

    }


    const data =
      res.data


    if (Array.isArray(data)) {

      list.value =
        data

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

  }

  catch (error) {

    console.error(
      '获取部门申报审核列表失败：',
      error
    )

    list.value = []

    ElMessage.error(
      error?.message ||
      '获取部门申报审核列表失败，请检查后端服务'
    )

  }

  finally {

    loading.value = false

  }

}


/* =========================================================
   搜索
========================================================= */

const filteredList =
  computed(() => {

    const key =
      keyword.value
        .trim()
        .toLowerCase()


    if (!key) {

      return list.value

    }


    return list.value.filter(
      item => {

        const studentName =
          String(
            item.studentName || ''
          ).toLowerCase()


        const applicantName =
          String(
            item.applicantName || ''
          ).toLowerCase()


        const departmentName =
          String(
            item.departmentName || ''
          ).toLowerCase()


        const title =
          String(
            item.title || ''
          ).toLowerCase()


        const description =
          String(
            item.description || ''
          ).toLowerCase()


        return (

          studentName.includes(key) ||

          applicantName.includes(key) ||

          departmentName.includes(key) ||

          title.includes(key) ||

          description.includes(key)

        )

      }
    )

  })


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


  openProtectedFile(
    url
  )

}


/* =========================================================
   单条审核请求
========================================================= */

async function archiveAuditRequest(
  row,
  status,
  remark
) {

  if (
    !row ||
    row.id == null
  ) {

    throw new Error(
      '申报记录不存在'
    )

  }


  const res =
    await request.put(

      `/departmentScoreApply/archive-audit/${row.id}`,

      {
        status: status,
        reviewRemark: remark || ''
      }

    )


  if (
    !res ||
    Number(res.code) !== 200
  ) {

    throw new Error(
      res?.message ||
      res?.msg ||
      `申报 ${row.id} 审核失败`
    )

  }


  return res

}


/* =========================================================
   单条通过
========================================================= */

async function pass(row) {

  if (
    !row ||
    row.id == null
  ) {

    ElMessage.error(
      '申报记录不存在'
    )

    return

  }


  if (
    processingId.value !== null ||
    batchLoading.value
  ) {

    return

  }


  try {

    await ElMessageBox.confirm(

      `确定通过「${
        row.studentName ||
        '该学生'
      }」的部门加减分申报吗？`,

      '档案部审核确认',

      {
        confirmButtonText:
          '确定通过',

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


  processingId.value =
    row.id

  rejectLoading.value =
    false


  try {

    await archiveAuditRequest(
      row,
      1,
      ''
    )


    ElMessage.success(
      '审核通过'
    )


    processedCount.value++


    await loadList()

  }

  catch (error) {

    console.error(
      '部门申报审核通过失败：',
      error
    )


    ElMessage.error(
      error?.message ||
      '审核失败'
    )

  }

  finally {

    processingId.value =
      null

  }

}


/* =========================================================
   单条驳回
========================================================= */

async function reject(row) {

  if (
    !row ||
    row.id == null
  ) {

    ElMessage.error(
      '申报记录不存在'
    )

    return

  }


  if (
    processingId.value !== null ||
    batchLoading.value
  ) {

    return

  }


  /*
   * 驳回必须填写原因，方便申报部门修正
   */

  let reason = ''

  try {

    const result =
      await ElMessageBox.prompt(

        `请填写驳回「${
          row.studentName ||
          '该学生'
        }」的原因`,

        '档案部审核驳回',

        {
          confirmButtonText:
            '确定驳回',

          cancelButtonText:
            '取消',

          inputPlaceholder:
            '例如：材料不齐全 / 不属于本部门项目',

          inputValidator: value =>
            (value && value.trim().length > 0) ||
            '请填写驳回原因',

          type:
            'warning'
        }

      )


    reason =
      result?.value || ''

  }

  catch {

    return

  }


  processingId.value =
    row.id

  rejectLoading.value =
    true


  try {

    await archiveAuditRequest(
      row,
      2,
      reason
    )


    ElMessage.success(
      '已驳回'
    )


    await loadList()

  }

  catch (error) {

    console.error(
      '部门申报驳回失败：',
      error
    )


    ElMessage.error(
      error?.message ||
      '驳回失败'
    )

  }

  finally {

    processingId.value =
      null

    rejectLoading.value =
      false

  }

}


/* =========================================================
   一键通过全部待审核
========================================================= */

async function batchPass() {

  if (batchLoading.value) {

    return

  }


  if (processingId.value !== null) {

    return

  }


  if (list.value.length === 0) {

    ElMessage.warning(
      '当前没有待审核的部门申报'
    )

    return

  }


  try {

    await ElMessageBox.confirm(

      `确定通过当前全部 ${list.value.length} 条待审核的部门申报吗？`,

      '一键通过确认',

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


  batchLoading.value =
    true


  try {

    const res =
      await request.put(
        '/departmentScoreApply/archive-audit/batch-pass'
      )


    if (
      !res ||
      Number(res.code) !== 200
    ) {

      throw new Error(
        res?.message ||
        res?.msg ||
        '一键通过失败'
      )

    }


    const result =
      res.data || {}


    const successCount =
      Number(result.successCount || 0)

    const failCount =
      Number(result.failCount || 0)


    if (failCount === 0) {

      ElMessage.success(
        `成功通过 ${successCount} 条部门申报`
      )

    }

    else {

      ElMessage.warning(
        `成功通过 ${successCount} 条，失败 ${failCount} 条`
      )


      if (
        Array.isArray(result.failures) &&
        result.failures.length > 0
      ) {

        console.warn(
          '一键通过失败明细：',
          result.failures
        )

      }

    }


    processedCount.value +=
      successCount


    await loadList()

  }

  catch (error) {

    console.error(
      '一键通过失败：',
      error
    )


    ElMessage.error(
      error?.message ||
      '一键通过失败'
    )

  }

  finally {

    batchLoading.value =
      false

  }

}


/* =========================================================
   页面初始化
========================================================= */

onMounted(() => {

  loadList()

})


onBeforeUnmount(
  releaseAllProtectedFileUrls
)

</script>


<style scoped>

/* =========================================================
   页面
========================================================= */

.audit-page {

  width: 100%;

  padding: 30px;

  min-height:
    calc(100vh - 60px);

  background:
    #f5f7fa;

  box-sizing: border-box;

}


/* =========================================================
   标题
========================================================= */

.title-card {

  margin-bottom:
    20px;

}


.title-box {

  display: flex;

  align-items:
    center;

  justify-content:
    space-between;

}


.title-box h2 {

  margin:
    0 0 8px;

  font-size:
    24px;

  color:
    #303133;

}


.title-box p {

  margin:
    0;

  color:
    #909399;

  font-size:
    14px;

}


/* =========================================================
   统计
========================================================= */

.statistics {

  display: grid;

  grid-template-columns:
    repeat(2, 1fr);

  gap:
    20px;

  margin-bottom:
    20px;

}


.stat-card {

  border:
    none;

}


.stat-card :deep(.el-card__body) {

  display: flex;

  align-items:
    center;

  padding:
    20px;

}


.stat-icon {

  width:
    55px;

  height:
    55px;

  border-radius:
    12px;

  display: flex;

  align-items:
    center;

  justify-content:
    center;

  margin-right:
    15px;

}


.stat-icon.pending {

  background:
    #fdf6ec;

  color:
    #e6a23c;

}


.stat-icon.selected {

  background:
    #ecf5ff;

  color:
    #409eff;

}


.stat-content {

  display: flex;

  flex-direction:
    column;

  gap:
    5px;

}


.stat-content span {

  color:
    #909399;

  font-size:
    14px;

}


.stat-content strong {

  color:
    #303133;

  font-size:
    28px;

}


/* =========================================================
   工具栏
========================================================= */

.toolbar {

  display: flex;

  align-items:
    center;

  width:
    100%;

  gap:
    12px;

}


.toolbar-right {

  margin-left:
    auto;

  display: flex;

  align-items:
    center;

  gap:
    15px;

}


/* =========================================================
   学生
========================================================= */

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


/* =========================================================
   分值
========================================================= */

.score {

  font-size:
    17px;

  font-weight:
    bold;

}


.score-add {

  color:
    #67c23a;

}


.score-minus {

  color:
    #f56c6c;

}


/* =========================================================
   无材料
========================================================= */

.no-evidence {

  color:
    #c0c4cc;

}


/* =========================================================
   表格
========================================================= */

.table-card {

  width:
    100%;

}


/* =========================================================
   空数据
========================================================= */

.el-empty {

  padding:
    40px 0;

}


/* =========================================================
   响应式
========================================================= */

@media (max-width: 1000px) {

  .audit-page {

    padding:
      20px;

  }


  .statistics {

    grid-template-columns:
      1fr;

  }


  .title-box {

    flex-direction:
      column;

    align-items:
      stretch;

    gap:
      15px;

  }

}


@media (max-width: 700px) {

  .toolbar {

    flex-direction:
      column;

    align-items:
      stretch;

  }


  .toolbar :deep(.el-input) {

    width:
      100% !important;

  }


  .toolbar-right {

    margin-left:
      0;

    justify-content:
      space-between;

  }

}

</style>
