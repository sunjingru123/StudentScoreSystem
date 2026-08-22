<template>
  <div class="student-detail-page">
    <!-- 顶部 -->
    <div class="page-header">
      <div>
        <h2>学生详情</h2>
        <p>查看学生基本信息、成绩明细及加分申请记录</p>
      </div>

      <el-button @click="goBack"> 返回学生管理 </el-button>
    </div>

    <!-- 学生基本信息 -->
    <el-card class="info-card">
      <template #header>
        <div class="card-title">
          <span>基本信息</span>

          <el-tag v-if="student.status === 1" type="success"> 正常 </el-tag>

          <el-tag v-else type="danger"> 禁用 </el-tag>
        </div>
      </template>

      <el-descriptions :column="3" border>
        <el-descriptions-item label="学号">
          {{ student.studentNo || '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="姓名">
          {{ student.realName || '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="用户名">
          {{ student.username || '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="班级">
          {{ student.className || '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="手机号">
          {{ student.phone || '-' }}
        </el-descriptions-item>

        <el-descriptions-item label="综合评分">
          <span class="total-score">
            {{ totalScore }}
          </span>
          分
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 成绩统计 -->
    <div class="stat-cards">
      <el-card class="stat-card">
        <div class="stat-icon blue">⭐</div>

        <div>
          <p>综合评分</p>
          <strong>{{ totalScore }}</strong>
          <span>分</span>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon green">✓</div>

        <div>
          <p>已通过加分</p>
          <strong>{{ approvedApplyCount }}</strong>
          <span>次</span>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon orange">⏳</div>

        <div>
          <p>待审核申请</p>
          <strong>{{ pendingApplyCount }}</strong>
          <span>次</span>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon red">×</div>

        <div>
          <p>拒绝申请</p>
          <strong>{{ rejectedApplyCount }}</strong>
          <span>次</span>
        </div>
      </el-card>
    </div>

    <!-- 成绩明细 -->
    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>成绩明细</span>

          <span class="section-count"> 共 {{ scoreList.length }} 条 </span>
        </div>
      </template>

      <el-table :data="scoreList" border stripe v-loading="scoreLoading">
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column prop="ruleName" label="加分项目" min-width="180" />

        <el-table-column prop="score" label="获得分数" width="120" align="center">
          <template #default="scope">
            <span class="score-plus"> +{{ scope.row.score }} </span>
          </template>
        </el-table-column>

        <el-table-column prop="sourceType" label="来源" width="120">
          <template #default="scope">
            <el-tag v-if="scope.row.sourceType === 'apply'" type="success" size="small">
              加分申请
            </el-tag>

            <el-tag v-else size="small">
              {{ scope.row.sourceType || '-' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="管理员状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="Number(scope.row.adminHidden ?? scope.row.adminhidden ?? 0) === 1" type="danger">
              已隐藏
            </el-tag>
            <el-tag v-else type="success"> 正常 </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="获得时间" min-width="180" />
      </el-table>

      <el-empty v-if="!scoreLoading && scoreList.length === 0" description="暂无成绩记录" />
    </el-card>

    <!-- 加分申请记录 -->
    <el-card class="section-card">
      <template #header>
        <div class="section-header">
          <span>加分申请记录</span>

          <span class="section-count"> 共 {{ applyList.length }} 条 </span>
        </div>
      </template>

      <el-table :data="applyList" border stripe v-loading="applyLoading">
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column prop="activityName" label="活动" min-width="180" />

        <el-table-column prop="ruleName" label="申请项目" min-width="150" />

        <el-table-column prop="applyScore" label="申请分数" width="110" align="center">
          <template #default="scope">
            <span class="apply-score"> {{ scope.row.applyScore }} 分 </span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning"> 待审核 </el-tag>

            <el-tag v-else-if="scope.row.status === 1" type="success"> 已通过 </el-tag>

            <el-tag v-else-if="scope.row.status === 2" type="danger"> 已拒绝 </el-tag>

            <el-tag v-else type="info"> 未知状态 </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="申请说明" min-width="240">
          <template #default="scope">
            <el-tooltip
              v-if="scope.row.description"
              :content="scope.row.description"
              placement="top"
            >
              <span class="description">
                {{ scope.row.description }}
              </span>
            </el-tooltip>

            <span v-else class="empty-text"> - </span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="申请时间" min-width="180" />
      </el-table>

      <el-empty v-if="!applyLoading && applyList.length === 0" description="暂无加分申请记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import request from '@/utils/request'

const route = useRoute()
const router = useRouter()

// 当前学生 ID
const studentId = Number(route.params.id)

// =========================
// 学生基本信息
// =========================

const student = ref({
  id: null,
  studentNo: '',
  username: '',
  realName: '',
  phone: '',
  className: '',
  status: 1,
})

// =========================
// 成绩
// =========================

const scoreList = ref([])

const scoreLoading = ref(false)

const totalScore = ref(0)

// =========================
// 加分申请
// =========================

const applyList = ref([])

const applyLoading = ref(false)

// =========================
// 统计
// =========================

const approvedApplyCount = computed(() => {
  return applyList.value.filter((item) => item.status === 1).length
})

const pendingApplyCount = computed(() => {
  return applyList.value.filter((item) => item.status === 0).length
})

const rejectedApplyCount = computed(() => {
  return applyList.value.filter((item) => item.status === 2).length
})

// =========================
// 查询学生信息
// =========================

function loadStudent() {
  if (!studentId) {
    ElMessage.error('学生信息不存在')
    return
  }

  request
    .get('/user/student/list')
    .then((res) => {
      const list = res.data.data || []

      const data = list.find((item) => Number(item.id) === studentId)

      if (!data) {
        ElMessage.error('找不到该学生')
        return
      }

      student.value = data
    })
    .catch((err) => {
      console.error(err)

      ElMessage.error('获取学生信息失败')
    })
}

// =========================
// 查询成绩
// =========================

function loadScore() {
  scoreLoading.value = true

  request
    .get(`/scoreStatistics/admin/${studentId}`)
    .then((res) => {
      console.log('管理员成绩明细接口返回：', res.data)

      const data = res.data?.data || res.data || {}

      totalScore.value = data.totalScore || 0

      scoreList.value = data.detail || data.list || []
    })
    .catch((err) => {
      console.error(err)

      ElMessage.error('获取成绩失败')

      scoreList.value = []
    })
    .finally(() => {
      scoreLoading.value = false
    })
}

// =========================
// 查询加分申请
// =========================

function loadApply() {
  applyLoading.value = true

  request
    .get(`/scoreApply/student/${studentId}`)
    .then((res) => {
      console.log('当前学生申请记录：', res)

      applyList.value =
        res.data.data || []
    })
    .catch((err) => {
      console.error(err)

      ElMessage.error('获取申请记录失败')

      applyList.value = []
    })
    .finally(() => {
      applyLoading.value = false
    })
}

// =========================
// 返回
// =========================

function goBack() {
  router.push('/admin/student')
}

// =========================
// 页面加载
// =========================

onMounted(() => {
  loadStudent()
  loadScore()
  loadApply()
})
</script>

<style scoped>
.student-detail-page {
  padding: 30px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* 页面头部 */

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 26px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
}

/* 基本信息 */

.info-card {
  margin-bottom: 20px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 17px;
  font-weight: bold;
}

.total-score {
  color: #409eff;
  font-size: 20px;
  font-weight: bold;
}

/* 统计卡片 */

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-card {
  border: none;
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px;
}

.stat-icon {
  width: 55px;
  height: 55px;
  border-radius: 12px;

  display: flex;
  align-items: center;
  justify-content: center;

  font-size: 26px;
}

.stat-icon.blue {
  background: #ecf5ff;
}

.stat-icon.green {
  background: #f0f9eb;
}

.stat-icon.orange {
  background: #fdf6ec;
}

.stat-icon.red {
  background: #fef0f0;
}

.stat-card p {
  margin: 0 0 5px;
  color: #909399;
}

.stat-card strong {
  font-size: 28px;
  color: #303133;
}

.stat-card span {
  margin-left: 4px;
  color: #909399;
}

/* 内容卡片 */

.section-card {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  font-size: 17px;
  font-weight: bold;
}

.section-count {
  color: #909399;
  font-size: 14px;
  font-weight: normal;
}

/* 成绩 */

.score-plus {
  color: #67c23a;
  font-weight: bold;
  font-size: 16px;
}

.apply-score {
  color: #409eff;
  font-weight: bold;
}

/* 申请说明 */

.description {
  display: inline-block;
  max-width: 220px;

  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;

  cursor: pointer;
}

.empty-text {
  color: #c0c4cc;
}

/* 响应式 */

@media (max-width: 1100px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 700px) {
  .student-detail-page {
    padding: 15px;
  }

  .stat-cards {
    grid-template-columns: 1fr;
  }
}
</style>
