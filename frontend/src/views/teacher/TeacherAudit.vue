<template>
  <div class="audit-page">
    <!-- 页面标题 -->
    <el-card class="title-card">
      <div class="title-box">
        <div>
          <h2>加分审核</h2>
          <p>审核学生提交的综合测评加分申请</p>
        </div>

        <el-button :icon="Refresh" @click="loadList"> 刷新 </el-button>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="statistics">
      <el-card class="stat-card">
        <div class="stat-icon all">
          <el-icon size="28">
            <Document />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>全部申请</span>
          <strong>{{ allCount }}</strong>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon pending">
          <el-icon size="28">
            <Clock />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>待审核</span>
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
          <span>已通过</span>
          <strong>{{ approvedCount }}</strong>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon rejected">
          <el-icon size="28">
            <CircleClose />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>已拒绝</span>
          <strong>{{ rejectedCount }}</strong>
        </div>
      </el-card>
    </div>

    <!-- 审核列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="toolbar">
          <!-- 搜索 -->
          <el-input v-model="keyword" placeholder="搜索学生姓名" clearable style="width: 220px">
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>

          <!-- 状态筛选 -->
          <el-select v-model="statusFilter" placeholder="申请状态" clearable style="width: 150px">
            <el-option label="待审核" :value="0" />

            <el-option label="已通过" :value="1" />

            <el-option label="已拒绝" :value="2" />
          </el-select>

          <div class="toolbar-right">
            <span class="selected-text"> 已选择 {{ selectedRows.length }} 条 </span>

            <el-button type="success" :disabled="selectedRows.length === 0" @click="batchPass">
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
        :data="filteredList"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <!-- 全选 -->
        <el-table-column type="selection" width="55" align="center" />

        <!-- 学生 -->
        <el-table-column prop="studentName" label="学生" min-width="110">
          <template #default="scope">
            <div class="student-cell">
              <el-avatar :size="36">
                {{ scope.row.studentName?.substring(0, 1) }}
              </el-avatar>

              <span>
                {{ scope.row.studentName }}
              </span>
            </div>
          </template>
        </el-table-column>

        <!-- 活动 -->
        <el-table-column prop="activityName" label="活动" min-width="150" />

        <!-- 项目 -->
        <el-table-column prop="ruleName" label="加分项目" min-width="120" />

        <!-- 分数 -->
        <el-table-column prop="applyScore" label="申请分数" width="110" align="center">
          <template #default="scope">
            <span class="score"> +{{ scope.row.applyScore }} </span>
          </template>
        </el-table-column>

        <!-- 申请说明 -->
        <el-table-column
          prop="description"
          label="申请说明"
          min-width="220"
          show-overflow-tooltip
        />

        <!-- 时间 -->
        <el-table-column prop="createTime" label="申请时间" width="180" />

        <!-- 状态 -->
        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning"> 待审核 </el-tag>

            <el-tag v-else-if="scope.row.status === 1" type="success"> 已通过 </el-tag>

            <el-tag v-else-if="scope.row.status === 2" type="danger"> 已拒绝 </el-tag>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <!-- 待审核 -->
            <template v-if="scope.row.status === 0">
              <el-button type="success" link @click="pass(scope.row)"> 通过 </el-button>

              <el-button type="danger" link @click="reject(scope.row)"> 驳回 </el-button>
            </template>

            <!-- 已审核 -->
            <template v-else>
              <span class="processed"> 已处理 </span>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空数据 -->
      <el-empty v-if="filteredList.length === 0" description="暂无申请记录" />
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadList"
        @size-change="handleSizeChange"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import { Document, Clock, CircleCheck, CircleClose, Search, Refresh } from '@element-plus/icons-vue'

import request from '@/utils/request'

/* =========================
   数据
========================= */

const list = ref([])

const selectedRows = ref([])

const keyword = ref('')

const statusFilter = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
/* =========================
   加载数据
========================= */
function handleSizeChange(size) {
  pageSize.value = size
  pageNum.value = 1
  loadList()
}
function loadList() {
  request
    .get('/scoreApply/list', {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
      },
    })
    .then((res) => {
      console.log('辅导员审核列表：', res)

      const data = res.data.data

      list.value = data?.records || []

      total.value = data?.total || 0

      selectedRows.value = []
    })
    .catch((err) => {
      console.error('获取审核列表失败：', err)

      ElMessage.error('获取申请列表失败')
    })
}

/* =========================
   统计
========================= */

const allCount = computed(() => {
  return list.value.length
})

const pendingCount = computed(() => {
  return list.value.filter((item) => item.status === 0).length
})

const approvedCount = computed(() => {
  return list.value.filter((item) => item.status === 1).length
})

const rejectedCount = computed(() => {
  return list.value.filter((item) => item.status === 2).length
})

/* =========================
   搜索 + 筛选
========================= */

const filteredList = computed(() => {
  return list.value.filter((item) => {
    const matchKeyword =
      !keyword.value || (item.studentName || '').toLowerCase().includes(keyword.value.toLowerCase())

    const matchStatus =
      statusFilter.value === null ||
      statusFilter.value === undefined ||
      item.status === statusFilter.value

    return matchKeyword && matchStatus
  })
})

/* =========================
   选择
========================= */

function handleSelectionChange(rows) {
  /*
   * 只允许选择待审核申请
   */

  selectedRows.value = rows.filter((row) => row.status === 0)
}

/* =========================
   单条通过
========================= */

function pass(row) {
  ElMessageBox.confirm(`确定通过 ${row.studentName} 的这条加分申请吗？`, '审核确认', {
    confirmButtonText: '确定通过',
    cancelButtonText: '取消',
    type: 'success',
  })
    .then(() => {
      request
        .post('/scoreApply/audit', {
          id: row.id,
          status: 1,
        })
        .then(() => {
          ElMessage.success('审核通过')
          loadList()
        })
        .catch(() => {
          ElMessage.error('审核失败')
        })
    })
    .catch(() => {})
}

/* =========================
   单条驳回
========================= */

function reject(row) {
  ElMessageBox.confirm(`确定驳回 ${row.studentName} 的这条加分申请吗？`, '审核确认', {
    confirmButtonText: '确定驳回',
    cancelButtonText: '取消',
    type: 'warning',
  })
    .then(() => {
      request
        .post('/scoreApply/audit', {
          id: row.id,
          status: 2,
        })
        .then(() => {
          ElMessage.success('申请已驳回')
          loadList()
        })
        .catch(() => {
          ElMessage.error('驳回失败')
        })
    })
    .catch(() => {})
}

/* =========================
   批量通过
========================= */

function batchPass() {
  const rows = selectedRows.value.filter((row) => row.status === 0)

  if (rows.length === 0) {
    ElMessage.warning('请选择待审核申请')

    return
  }

  ElMessageBox.confirm(`确定一次通过选中的 ${rows.length} 条申请吗？`, '批量审核', {
    confirmButtonText: '全部通过',
    cancelButtonText: '取消',
    type: 'success',
  })
    .then(async () => {
      let successCount = 0

      for (const row of rows) {
        try {
          await request.post('/scoreApply/audit', {
            id: row.id,
            status: 1,
          })
          successCount++
        } catch (error) {
          console.error('审核失败：', row.id, error)
        }
      }

      ElMessage.success(`成功通过 ${successCount} 条申请`)

      loadList()
    })
    .catch(() => {})
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

/* 标题 */
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

/* 统计 */
.statistics {
  display: grid;

  grid-template-columns: repeat(4, 1fr);

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

/* 不同颜色 */
.stat-icon.all {
  background: #ecf5ff;
  color: #409eff;
}

.stat-icon.pending {
  background: #fdf6ec;
  color: #e6a23c;
}

.stat-icon.approved {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-icon.rejected {
  background: #fef0f0;
  color: #f56c6c;
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

/* 工具栏 */
.toolbar {
  display: flex;

  align-items: center;

  gap: 12px;

  width: 100%;
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

/* 学生 */
.student-cell {
  display: flex;

  align-items: center;

  gap: 10px;
}

/* 分数 */
.score {
  color: #409eff;

  font-size: 17px;

  font-weight: bold;
}

/* 已处理 */
.processed {
  color: #909399;

  font-size: 13px;
}

/* 空数据 */
.el-empty {
  padding: 30px 0;
}

/* 响应式 */
@media (max-width: 1100px) {
  .statistics {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
