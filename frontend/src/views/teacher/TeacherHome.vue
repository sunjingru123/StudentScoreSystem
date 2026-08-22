<template>
  <div class="teacher-home">
    <!-- 欢迎区域 -->
    <el-card class="welcome-card">
      <div class="welcome-content">
        <div class="welcome-left">
          <div class="welcome-title">👋 欢迎，{{ teacherName }}老师</div>

          <div class="welcome-subtitle">学生综合测评管理工作台</div>

          <div class="welcome-tip">今天也辛苦啦，下面是当前需要处理的工作。</div>
        </div>

        <div class="welcome-icon">🎓</div>
      </div>
    </el-card>

    <!-- 数据统计 -->
    <div class="statistics">
      <!-- 待审核 -->
      <el-card class="stat-card clickable" @click="$router.push('/teacher/audit')">
        <div class="stat-content">
          <div class="stat-icon warning">
            <el-icon>
              <Clock />
            </el-icon>
          </div>

          <div class="stat-info">
            <div class="stat-label">待审核申请</div>

            <div class="stat-number">
              {{ pendingCount }}
            </div>

            <div class="stat-desc">等待您处理</div>
          </div>
        </div>
      </el-card>

      <!-- 今日申请 -->
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon primary">
            <el-icon>
              <Document />
            </el-icon>
          </div>

          <div class="stat-info">
            <div class="stat-label">今日申请</div>

            <div class="stat-number">
              {{ todayCount }}
            </div>

            <div class="stat-desc">今日新增申请</div>
          </div>
        </div>
      </el-card>

      <!-- 已审核 -->
      <el-card class="stat-card">
        <div class="stat-content">
          <div class="stat-icon success">
            <el-icon>
              <CircleCheck />
            </el-icon>
          </div>

          <div class="stat-info">
            <div class="stat-label">已审核</div>

            <div class="stat-number">
              {{ auditedCount }}
            </div>

            <div class="stat-desc">已完成审核</div>
          </div>
        </div>
      </el-card>

      <!-- 学生人数 -->
      <el-card class="stat-card clickable" @click="$router.push('/teacher/student')">
        <div class="stat-content">
          <div class="stat-icon info">
            <el-icon>
              <User />
            </el-icon>
          </div>

          <div class="stat-info">
            <div class="stat-label">学生人数</div>

            <div class="stat-number">
              {{ studentCount }}
            </div>

            <div class="stat-desc">当前管理学生</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 下方内容 -->
    <div class="content-grid">
      <!-- 待审核申请 -->
      <el-card class="panel">
        <template #header>
          <div class="panel-header">
            <span> 待审核申请 </span>

            <el-button type="primary" link @click="$router.push('/teacher/audit')">
              查看全部 →
            </el-button>
          </div>
        </template>

        <el-table v-loading="loading" :data="pendingList" style="width: 100%">
          <el-table-column prop="studentName" label="学生" min-width="100" />

          <el-table-column prop="activityName" label="活动" min-width="130" />

          <el-table-column prop="ruleName" label="项目" min-width="100" />

          <el-table-column prop="applyScore" label="申请分数" width="100">
            <template #default="scope">
              <span class="score"> +{{ scope.row.applyScore }} </span>
            </template>
          </el-table-column>

          <el-table-column prop="createTime" label="申请时间" min-width="160" />
        </el-table>

        <!-- 没有数据 -->
        <el-empty v-if="!loading && pendingList.length === 0" description="暂无待审核申请" />
      </el-card>

      <!-- 快捷入口 -->
      <el-card class="panel quick-panel">
        <template #header>
          <div class="panel-title">快捷操作</div>
        </template>

        <div class="quick-actions">
          <div class="quick-item" @click="$router.push('/teacher/audit')">
            <div class="quick-icon audit">
              <el-icon>
                <Checked />
              </el-icon>
            </div>

            <div>
              <div class="quick-title">加分审核</div>

              <div class="quick-desc">查看并处理学生申请</div>
            </div>
          </div>

          <div class="quick-item" @click="$router.push('/teacher/student')">
            <div class="quick-icon student">
              <el-icon>
                <User />
              </el-icon>
            </div>

            <div>
              <div class="quick-title">学生管理</div>

              <div class="quick-desc">查看学生基本信息</div>
            </div>
          </div>

          <div class="quick-item" @click="$router.push('/teacher/activity')">
            <div class="quick-icon activity">
              <el-icon>
                <Calendar />
              </el-icon>
            </div>

            <div>
              <div class="quick-title">活动管理</div>

              <div class="quick-desc">查看学生参与活动</div>
            </div>
          </div>

          <div class="quick-item" @click="$router.push('/teacher/score')">
            <div class="quick-icon score">
              <el-icon>
                <Trophy />
              </el-icon>
            </div>

            <div>
              <div class="quick-title">成绩查看</div>

              <div class="quick-desc">查看学生综合测评成绩</div>
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import request from '@/utils/request'

import {
  Clock,
  Document,
  CircleCheck,
  User,
  Checked,
  Calendar,
  Trophy,
} from '@element-plus/icons-vue'

const router = useRouter()

const teacherName = ref('辅导员')

const pendingCount = ref(0)

const todayCount = ref(0)

const auditedCount = ref(0)

const studentCount = ref(0)

const pendingList = ref([])

const loading = ref(false)

/**
 * 获取当前老师
 */
function loadTeacher() {
  const userStr = localStorage.getItem('user')

  if (!userStr) {
    router.replace('/login')

    return
  }

  try {
    const user = JSON.parse(userStr)

    console.log('当前辅导员：', user)

    teacherName.value = user.realName || user.username || '辅导员'
  } catch (error) {
    console.error('读取用户信息失败：', error)
  }
}

/**
 * 获取待审核申请
 */
function loadPending() {
  loading.value = true

  request
    .get('/scoreApply/pending')
    .then((res) => {
      console.log('待审核申请：', res)

      const data = res.data.data || []

      pendingList.value = data

      pendingCount.value = data.length
    })
    .catch((error) => {
      console.error('获取待审核申请失败：', error)

      pendingList.value = []

      pendingCount.value = 0
    })
    .finally(() => {
      loading.value = false
    })
}

/**
 * 获取全部申请
 *
 * 用于统计已经审核的数量
 */
function loadAllApply() {
  request
    .get('/scoreApply/list')
    .then((res) => {
      console.log('全部申请：', res)

      const data = res.data.data || []

      // 已审核
      auditedCount.value = data.filter((item) => item.status === 1 || item.status === 2).length

      // 今天
      const today = new Date()

      const year = today.getFullYear()

      const month = String(today.getMonth() + 1).padStart(2, '0')

      const day = String(today.getDate()).padStart(2, '0')

      const todayStr = `${year}-${month}-${day}`

      todayCount.value = data.filter((item) => {
        if (!item.createTime) {
          return false
        }

        return item.createTime.startsWith(todayStr)
      }).length
    })
    .catch((error) => {
      console.error('获取申请统计失败：', error)
    })
}

/**
 * 获取学生人数
 *
 * 注意：
 * 这里先尝试调用老师学生接口。
 *
 * 如果你的后端还没有这个接口，
 * 不会影响首页其它内容。
 */
function loadStudentCount() {
  request
    .get('/user/student/list')
    .then((res) => {
      console.log('学生列表：', res)

      const data = res.data.data || []

      studentCount.value = data.length
    })
    .catch((error) => {
      console.warn('学生数量接口暂不可用：', error)

      studentCount.value = 0
    })
}

onMounted(() => {
  loadTeacher()

  loadPending()

  loadAllApply()

  loadStudentCount()
})
</script>

<style scoped>
.teacher-home {
  width: 100%;
}

/* =========================
   欢迎区域
========================= */

.welcome-card {
  margin-bottom: 20px;

  border: none;

  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);

  color: white;

  overflow: hidden;
}

.welcome-content {
  min-height: 150px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  padding: 10px 20px;
}

.welcome-left {
  display: flex;

  flex-direction: column;

  gap: 10px;
}

.welcome-title {
  font-size: 28px;

  font-weight: bold;
}

.welcome-subtitle {
  font-size: 18px;

  opacity: 0.95;
}

.welcome-tip {
  font-size: 14px;

  opacity: 0.8;
}

.welcome-icon {
  font-size: 90px;

  opacity: 0.18;

  margin-right: 50px;
}

/* =========================
   数据统计
========================= */

.statistics {
  display: grid;

  grid-template-columns: repeat(4, 1fr);

  gap: 20px;

  margin-bottom: 20px;
}

.stat-card {
  border: none;

  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);

  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.clickable {
  cursor: pointer;
}

.stat-content {
  display: flex;

  align-items: center;

  gap: 18px;
}

.stat-icon {
  width: 60px;

  height: 60px;

  border-radius: 12px;

  display: flex;

  align-items: center;

  justify-content: center;

  font-size: 30px;
}

.stat-icon.warning {
  background: #fdf6ec;

  color: #e6a23c;
}

.stat-icon.primary {
  background: #ecf5ff;

  color: #409eff;
}

.stat-icon.success {
  background: #f0f9eb;

  color: #67c23a;
}

.stat-icon.info {
  background: #f4f4f5;

  color: #909399;
}

.stat-label {
  font-size: 14px;

  color: #909399;

  margin-bottom: 5px;
}

.stat-number {
  font-size: 30px;

  font-weight: bold;

  color: #303133;
}

.stat-desc {
  font-size: 12px;

  color: #c0c4cc;

  margin-top: 2px;
}

/* =========================
   下方区域
========================= */

.content-grid {
  display: grid;

  grid-template-columns: 2fr 1fr;

  gap: 20px;
}

.panel {
  border: none;
}

.panel-header {
  display: flex;

  align-items: center;

  justify-content: space-between;

  font-size: 17px;

  font-weight: bold;
}

.panel-title {
  font-size: 17px;

  font-weight: bold;
}

/* =========================
   表格
========================= */

.score {
  color: #67c23a;

  font-weight: bold;
}

/* =========================
   快捷操作
========================= */

.quick-actions {
  display: flex;

  flex-direction: column;

  gap: 5px;
}

.quick-item {
  display: flex;

  align-items: center;

  gap: 15px;

  padding: 15px;

  border-radius: 8px;

  cursor: pointer;

  transition: all 0.2s;
}

.quick-item:hover {
  background: #f5f7fa;

  transform: translateX(3px);
}

.quick-icon {
  width: 45px;

  height: 45px;

  border-radius: 10px;

  display: flex;

  align-items: center;

  justify-content: center;

  font-size: 22px;
}

.quick-icon.audit {
  background: #ecf5ff;

  color: #409eff;
}

.quick-icon.student {
  background: #f0f9eb;

  color: #67c23a;
}

.quick-icon.activity {
  background: #fdf6ec;

  color: #e6a23c;
}

.quick-icon.score {
  background: #f4f4f5;

  color: #909399;
}

.quick-title {
  font-size: 15px;

  font-weight: bold;

  color: #303133;
}

.quick-desc {
  font-size: 12px;

  color: #909399;

  margin-top: 4px;
}

/* =========================
   响应式
========================= */

@media (max-width: 1100px) {
  .statistics {
    grid-template-columns: repeat(2, 1fr);
  }

  .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 700px) {
  .statistics {
    grid-template-columns: 1fr;
  }

  .welcome-title {
    font-size: 22px;
  }

  .welcome-icon {
    display: none;
  }
}
</style>
