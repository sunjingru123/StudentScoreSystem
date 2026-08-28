<template>
  <div class="audit-page">

    <!-- =====================================================
         页面标题
    ====================================================== -->
    <el-card class="title-card">

      <div class="title-box">

        <div>
          <h2>部门加减分最终审核</h2>

          <p>
            审核部长 / 副部长初审通过的部门加减分申请
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

      <!-- 待最终审核 -->
      <el-card class="stat-card">

        <div class="stat-icon pending">

          <el-icon size="28">
            <Clock />
          </el-icon>

        </div>

        <div class="stat-content">

          <span>
            待最终审核
          </span>

          <strong>
            {{ list.length }}
          </strong>

        </div>

      </el-card>


      <!-- 已选择 -->
      <el-card class="stat-card">

        <div class="stat-icon selected">

          <el-icon size="28">
            <Checked />
          </el-icon>

        </div>

        <div class="stat-content">

          <span>
            已选择
          </span>

          <strong>
            {{ selectedRows.length }}
          </strong>

        </div>

      </el-card>

    </div>


    <!-- =====================================================
         最终审核列表
    ====================================================== -->
    <el-card class="table-card">

      <template #header>

        <div class="toolbar">

          <!-- 搜索 -->
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


          <!-- 右侧 -->
          <div class="toolbar-right">

            <span class="selected-text">
              已选择 {{ selectedRows.length }} 条
            </span>


            <!-- 一键最终通过 -->
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

          </div>

        </div>

      </template>


      <!-- =====================================================
           表格
      ====================================================== -->
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="filteredList"
        row-key="id"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >

        <!-- 选择 -->
        <el-table-column
          type="selection"
          width="55"
          align="center"
        />


        <!-- 序号 -->
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

            {{
              row.applicantName ||
              '—'
            }}

          </template>

        </el-table-column>


        <!-- 部门 -->
        <el-table-column
          prop="departmentName"
          label="部门"
          min-width="130"
        >

          <template #default="{ row }">

            {{
              row.departmentName ||
              '—'
            }}

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

            {{
              row.title ||
              '—'
            }}

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

            {{
              row.description ||
              '—'
            }}

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


        <!-- =================================================
             状态
        ================================================== -->
        <el-table-column
          label="状态"
          width="130"
          align="center"
        >

          <template #default>

            <el-tag type="warning">

              待最终审核

            </el-tag>

          </template>

        </el-table-column>


        <!-- =================================================
             操作
        ================================================== -->
        <el-table-column
          label="操作"
          width="190"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <!-- 最终通过 -->
            <el-button
              type="success"
              link
              :loading="processingId === row.id"
              :disabled="
                processingId !== null &&
                processingId !== row.id
              "
              @click="pass(row)"
            >

              <el-icon>
                <CircleCheck />
              </el-icon>

              通过

            </el-button>


            <!-- 最终驳回 -->
            <el-button
              type="danger"
              link
              :disabled="
                processingId !== null
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
        description="暂无待最终审核申请"
      />

    </el-card>

  </div>
</template>


<script setup>

import {
  ref,
  computed,
  onMounted
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


/* =========================================================
   数据
========================================================= */

const list = ref([])

const selectedRows = ref([])

const keyword = ref('')

const loading = ref(false)

const batchLoading = ref(false)

const processingId = ref(null)

const tableRef = ref(null)


/* =========================================================
   获取最终审核列表
========================================================= */

async function loadList() {

  console.log('====================================')

  console.log(
    '开始加载部门加减分最终审核列表'
  )

  loading.value = true

  try {

    /*
     * =====================================================
     * 这里已经改成【最终审核接口】
     *
     * 不再使用：
     *
     * /departmentScoreApply/audit/list
     *
     * =====================================================
     */

    const res =
      await request.get(
        '/departmentScoreApply/final-audit/list'
      )


    console.log(
      '最终审核接口完整响应：',
      res
    )

    console.log(
      'res.data：',
      res.data
    )

    console.log(
      'res.data.data：',
      res.data?.data
    )


    const data =
      res.data?.data


    /*
     * 后端返回数组
     */

    if (Array.isArray(data)) {

      list.value =
        data

    }

    /*
     * 兼容分页
     */

    else if (
      data &&
      Array.isArray(data.records)
    ) {

      list.value =
        data.records

    }

    /*
     * 数据异常
     */

    else {

      console.warn(
        '最终审核数据格式异常：',
        data
      )

      list.value = []

    }


    /*
     * 清除旧选择
     */

    selectedRows.value = []


    if (tableRef.value) {

      tableRef.value.clearSelection()

    }


    console.log(
      '最终审核列表：',
      list.value
    )

    console.log(
      '待最终审核数量：',
      list.value.length
    )

    console.log('====================================')

  }

  catch (error) {

    console.error(
      '获取最终审核列表失败：',
      error
    )

    list.value = []

    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取最终审核列表失败，请检查后端服务'
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
   多选
========================================================= */

function handleSelectionChange(rows) {

  selectedRows.value =
    rows || []


  console.log(
    '当前选择：',
    selectedRows.value
  )

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
   单条最终通过
========================================================= */

async function pass(row) {

  if (!row || !row.id) {

    ElMessage.error(
      '申请记录不存在'
    )

    return

  }


  /*
   * 确认
   */

  try {

    await ElMessageBox.confirm(

      `确定通过「${
        row.studentName ||
        '该学生'
      }」的最终审核吗？`,

      '最终审核确认',

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


  try {

    console.log(
      '准备最终通过：',
      row
    )


    /*
     * =====================================================
     * 最关键：
     *
     * 初审：
     * /departmentScoreApply/audit/{id}
     *
     * 现在改成：
     *
     * /departmentScoreApply/final-audit/{id}
     * =====================================================
     */

    const res =
      await request.put(

        `/departmentScoreApply/final-audit/${row.id}`,

        {
          status: 1,
          reviewRemark: ''
        }

      )


    console.log(
      '最终通过响应：',
      res
    )


    if (
      res.data?.code === 200
    ) {

      ElMessage.success(
        '最终审核通过'
      )

      await loadList()

    }

    else {

      ElMessage.error(

        res.data?.message ||
        res.data?.msg ||
        '最终审核失败'

      )

    }

  }

  catch (error) {

    console.error(
      '最终审核通过失败：',
      error
    )

    ElMessage.error(

      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '最终审核失败，请检查后端接口'

    )

  }

  finally {

    processingId.value =
      null

  }

}


/* =========================================================
   单条最终驳回
========================================================= */

async function reject(row) {

  if (!row || !row.id) {

    ElMessage.error(
      '申请记录不存在'
    )

    return

  }


  /*
   * 确认
   */

  try {

    await ElMessageBox.confirm(

      `确定驳回「${
        row.studentName ||
        '该学生'
      }」的最终审核吗？`,

      '最终审核确认',

      {
        confirmButtonText:
          '确定驳回',

        cancelButtonText:
          '取消',

        type:
          'warning'
      }

    )

  }

  catch {

    return

  }


  processingId.value =
    row.id


  try {

    console.log(
      '准备最终驳回：',
      row
    )


    /*
     * 最终审核接口
     */

    const res =
      await request.put(

        `/departmentScoreApply/final-audit/${row.id}`,

        {
          status: 2,
          reviewRemark: ''
        }

      )


    console.log(
      '最终驳回响应：',
      res
    )


    if (
      res.data?.code === 200
    ) {

      ElMessage.success(
        '申请已驳回'
      )

      await loadList()

    }

    else {

      ElMessage.error(

        res.data?.message ||
        res.data?.msg ||
        '驳回失败'

      )

    }

  }

  catch (error) {

    console.error(
      '最终驳回失败：',
      error
    )

    ElMessage.error(

      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '驳回失败，请检查后端接口'

    )

  }

  finally {

    processingId.value =
      null

  }

}


/* =========================================================
   一键通过
========================================================= */

async function batchPass() {

  const rows =
    tableRef.value?.getSelectionRows?.() ||
    selectedRows.value ||
    []


  console.log(
    '开始一键最终审核'
  )

  console.log(
    '选中的申请：',
    rows
  )


  /*
   * 没有选择
   */

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

      `确定通过选中的 ${rows.length} 条部门加减分申请吗？`,

      '一键最终审核',

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


  /*
   * 开始批量处理
   */

  batchLoading.value =
    true


  let successCount =
    0

  let failCount =
    0


  try {

    /*
     * 后端没有批量接口，
     * 所以逐条调用最终审核接口。
     */

    for (
      const row
      of rows
      ) {

      if (
        !row ||
        !row.id
      ) {

        failCount++

        continue

      }


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
          `申请 ${row.id} 最终审核响应：`,
          res
        )


        if (
          res.data?.code === 200
        ) {

          successCount++

        }

        else {

          failCount++

        }

      }

      catch (error) {

        console.error(
          `申请 ${row.id} 最终审核失败：`,
          error
        )

        failCount++

      }

    }


    /*
     * 结果提示
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
        `批量审核失败，共 ${failCount} 条`
      )

    }


    /*
     * 重新加载
     */

    await loadList()

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


.selected-text {

  color:
    #909399;

  font-size:
    14px;

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
