<template>
  <div class="audit-page">

    <!-- 页面标题 -->
    <el-card class="title-card">
      <div class="title-box">
        <div>
          <h2>最终审核</h2>
          <p>审核部长初审通过的部门加分申请</p>
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

    <!-- 统计 -->
    <div class="statistics">

      <el-card class="stat-card">
        <div class="stat-icon all">
          <el-icon size="28">
            <Document />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>待最终审核</span>
          <strong>{{ pendingCount }}</strong>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon approved">
          <el-icon size="28">
            <CircleCheck />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>本页申请</span>
          <strong>{{ list.length }}</strong>
        </div>
      </el-card>

    </div>

    <!-- 审核列表 -->
    <el-card class="table-card">

      <template #header>
        <div class="toolbar">

          <el-input
            v-model="keyword"
            placeholder="搜索学生姓名 / 部门"
            clearable
            style="width: 240px"
          >
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>

          <div class="toolbar-right">
            <span class="selected-text">
              已选择 {{ selectedRows.length }} 条
            </span>

            <el-button
              type="success"
              :disabled="selectedRows.length === 0"
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

      <!-- 表格 -->
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="filteredList"
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

        <!-- 学生 -->
        <el-table-column
          prop="studentName"
          label="学生"
          min-width="110"
        />

        <!-- 部门 -->
        <el-table-column
          prop="departmentName"
          label="部门"
          min-width="120"
        />

        <!-- 活动 -->
        <el-table-column
          prop="activityName"
          label="活动"
          min-width="160"
        />

        <!-- 项目 -->
        <el-table-column
          prop="ruleName"
          label="加分项目"
          min-width="130"
        />

        <!-- 分数 -->
        <el-table-column
          prop="applyScore"
          label="申请分数"
          width="110"
          align="center"
        >
          <template #default="scope">
            <span class="score">
              +{{ scope.row.applyScore }}
            </span>
          </template>
        </el-table-column>

        <!-- 申请说明 -->
        <el-table-column
          prop="description"
          label="申请说明"
          min-width="220"
          show-overflow-tooltip
        />

        <!-- 申请时间 -->
        <el-table-column
          prop="createTime"
          label="申请时间"
          width="180"
        />

        <!-- 状态 -->
        <el-table-column
          label="状态"
          width="120"
          align="center"
        >
          <template #default="scope">

            <el-tag
              v-if="scope.row.status === 0"
              type="warning"
            >
              待最终审核
            </el-tag>

            <el-tag
              v-else-if="scope.row.status === 1"
              type="success"
            >
              已通过
            </el-tag>

            <el-tag
              v-else-if="scope.row.status === 2"
              type="danger"
            >
              已拒绝
            </el-tag>

            <el-tag
              v-else
              type="info"
            >
              {{ scope.row.status }}
            </el-tag>

          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column
          label="操作"
          width="180"
          fixed="right"
          align="center"
        >

          <template #default="scope">

            <template v-if="scope.row.status === 0">

              <el-button
                type="success"
                link
                @click="pass(scope.row)"
              >
                通过
              </el-button>

              <el-button
                type="danger"
                link
                @click="reject(scope.row)"
              >
                驳回
              </el-button>

            </template>

            <span
              v-else
              class="processed"
            >
              已处理
            </span>

          </template>

        </el-table-column>

      </el-table>

      <!-- 空数据 -->
      <el-empty
        v-if="!loading && filteredList.length === 0"
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
  Document,
  CircleCheck,
  Search,
  Refresh
} from '@element-plus/icons-vue'

import request from '@/utils/request'


/* =========================
   数据
========================= */

const list = ref([])

const selectedRows = ref([])

const keyword = ref('')

const loading = ref(false)


/* =========================
   当前辅导员
========================= */

function getCurrentTeacher() {

  const userStr = localStorage.getItem('user')

  if (!userStr) {
    return null
  }

  try {

    const user = JSON.parse(userStr)

    console.log('当前辅导员：', user)

    return user

  } catch (error) {

    console.error(
      '读取当前辅导员失败：',
      error
    )

    return null
  }
}


/* =========================
   获取最终审核列表
========================= */

async function loadList() {

  loading.value = true

  try {

    console.log(
      '开始获取辅导员最终审核列表'
    )

    const res =
      await request.get(
        '/departmentScoreApply/final-audit/list'
      )

    console.log(
      '辅导员最终审核完整响应：',
      res
    )

    /*
     * utils/request 已经把 Axios response 拆掉
     *
     * 正确结构：
     *
     * res.code
     * res.message
     * res.data
     *
     * 其中 res.data 就是数组
     */

    if (
      res?.code === 200 &&
      Array.isArray(res.data)
    ) {

      list.value =
        res.data

    } else {

      console.warn(
        '辅导员最终审核接口返回异常：',
        res
      )

      list.value = []
    }

    selectedRows.value = []

    console.log(
      '最终审核列表：',
      list.value
    )

  } catch (error) {

    console.error(
      '获取辅导员最终审核列表失败：',
      error
    )

    list.value = []

    ElMessage.error(
      '获取最终审核列表失败，请检查后端服务'
    )

  } finally {

    loading.value = false

  }
}


/* =========================
   待审核数量
========================= */

const pendingCount = computed(() => {

  return list.value.filter(
    item => item.status === 0
  ).length

})


/* =========================
   搜索
========================= */

const filteredList = computed(() => {

  const key = keyword.value
    .trim()
    .toLowerCase()

  if (!key) {
    return list.value
  }

  return list.value.filter(item => {

    const studentName =
      item.studentName || ''

    const departmentName =
      item.departmentName || ''

    const activityName =
      item.activityName || ''

    return (
      studentName
        .toLowerCase()
        .includes(key) ||

      departmentName
        .toLowerCase()
        .includes(key) ||

      activityName
        .toLowerCase()
        .includes(key)
    )

  })

})


/* =========================
   选择
========================= */

function handleSelectionChange(rows) {

  selectedRows.value =
    rows.filter(
      row => row.status === 0
    )

}


/* =========================
   最终通过
========================= */

async function pass(row) {

  try {

    await ElMessageBox.confirm(
      `确定通过 ${row.studentName || '该学生'} 的最终审核吗？`,
      '最终审核确认',
      {
        confirmButtonText: '确定通过',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    console.log(
      '准备最终通过：',
      row
    )

    /*
     * 这里调用最终审核接口。
     *
     * 如果你的后端最终审核接口就是：
     *
     * /departmentScoreApply/final-audit
     *
     * 就直接使用。
     */

    const res = await request.post(
      '/departmentScoreApply/final-audit',
      {
        id: row.id,
        status: 1
      }
    )

    console.log(
      '最终审核通过响应：',
      res
    )

    ElMessage.success(
      '最终审核通过'
    )

    await loadList()

  } catch (error) {

    /*
     * 点击取消不会提示错误
     */

    if (
      error !== 'cancel' &&
      error !== 'close'
    ) {

      console.error(
        '最终审核通过失败：',
        error
      )

      ElMessage.error(
        '最终审核失败，请检查后端接口'
      )
    }

  }

}


/* =========================
   最终驳回
========================= */

async function reject(row) {

  try {

    await ElMessageBox.confirm(
      `确定驳回 ${row.studentName || '该学生'} 的最终审核吗？`,
      '最终审核确认',
      {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    console.log(
      '准备最终驳回：',
      row
    )

    const res = await request.post(
      '/departmentScoreApply/final-audit',
      {
        id: row.id,
        status: 2
      }
    )

    console.log(
      '最终驳回响应：',
      res
    )

    ElMessage.success(
      '申请已驳回'
    )

    await loadList()

  } catch (error) {

    if (
      error !== 'cancel' &&
      error !== 'close'
    ) {

      console.error(
        '最终驳回失败：',
        error
      )

      ElMessage.error(
        '驳回失败，请检查后端接口'
      )
    }

  }

}


/* =========================
   批量通过
========================= */

async function batchPass() {

  const rows =
    selectedRows.value.filter(
      row => row.status === 0
    )

  if (rows.length === 0) {

    ElMessage.warning(
      '请选择待最终审核申请'
    )

    return
  }

  try {

    await ElMessageBox.confirm(
      `确定通过选中的 ${rows.length} 条申请吗？`,
      '批量最终审核',
      {
        confirmButtonText: '全部通过',
        cancelButtonText: '取消',
        type: 'success'
      }
    )

    let successCount = 0

    for (const row of rows) {

      try {

        await request.post(
          '/departmentScoreApply/final-audit',
          {
            id: row.id,
            status: 1
          }
        )

        successCount++

      } catch (error) {

        console.error(
          '最终审核失败：',
          row,
          error
        )

      }

    }

    ElMessage.success(
      `成功通过 ${successCount} 条申请`
    )

    await loadList()

  } catch (error) {

    // 用户取消，不处理

  }

}


/* =========================
   初始化
========================= */

onMounted(() => {

  loadList()

})

</script>


<style scoped>

.audit-page {
  width: 100%;
}


/* =========================
   标题
========================= */

.title-card {
  margin-bottom: 20px;
}

.title-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title-box h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #303133;
}

.title-box p {
  margin: 0;
  color: #909399;
}


/* =========================
   统计
========================= */

.statistics {
  display: grid;

  grid-template-columns:
    repeat(2, 1fr);

  gap: 20px;

  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;

  padding: 20px;

  border: none;
}

.stat-icon {
  width: 55px;
  height: 55px;

  border-radius: 12px;

  display: flex;
  align-items: center;
  justify-content: center;

  margin-right: 15px;
}

.stat-icon.all {
  background: #ecf5ff;
  color: #409eff;
}

.stat-icon.approved {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.stat-content span {
  color: #909399;
  font-size: 14px;
}

.stat-content strong {
  font-size: 28px;
  color: #303133;
}


/* =========================
   工具栏
========================= */

.toolbar {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 12px;
}

.toolbar-right {
  margin-left: auto;

  display: flex;
  align-items: center;

  gap: 15px;
}

.selected-text {
  color: #909399;
  font-size: 14px;
}


/* =========================
   分数
========================= */

.score {
  color: #409eff;
  font-size: 17px;
  font-weight: bold;
}


/* =========================
   已处理
========================= */

.processed {
  color: #909399;
  font-size: 13px;
}


/* =========================
   空数据
========================= */

.el-empty {
  padding: 40px 0;
}


/* =========================
   响应式
========================= */

@media (max-width: 700px) {

  .statistics {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    margin-left: 0;
  }

}

</style>
