<template>
  <div class="student-page">
    <!-- 页面标题 -->
    <el-card class="title-card">
      <div class="title-box">
        <div>
          <h2>学生管理</h2>
          <p>查看和管理学生基本信息、综合评分及账号状态</p>
        </div>

        <el-button :icon="Refresh" :loading="loading" @click="loadStudents"> 刷新 </el-button>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="statistics">
      <el-card class="stat-card">
        <div class="stat-icon total">
          <el-icon size="28">
            <User />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>学生总数</span>
          <strong>{{ studentList.length }}</strong>
          <small>人</small>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon normal">
          <el-icon size="28">
            <CircleCheck />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>正常账号</span>
          <strong>{{ normalCount }}</strong>
          <small>人</small>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon disabled">
          <el-icon size="28">
            <CircleClose />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>禁用账号</span>
          <strong>{{ disabledCount }}</strong>
          <small>人</small>
        </div>
      </el-card>

      <el-card class="stat-card">
        <div class="stat-icon score">
          <el-icon size="28">
            <Trophy />
          </el-icon>
        </div>

        <div class="stat-content">
          <span>已有成绩</span>
          <strong>{{ scoreStudentCount }}</strong>
          <small>人</small>
        </div>
      </el-card>
    </div>

    <!-- 学生列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="toolbar">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索学号、姓名、班级或账号"
            style="width: 320px"
          >
            <template #prefix>
              <el-icon>
                <Search />
              </el-icon>
            </template>
          </el-input>

          <span class="result-text"> 当前显示 {{ filteredList.length }} 人 </span>
        </div>
      </template>

      <el-table v-loading="loading" :data="filteredList" border stripe style="width: 100%">
        <!-- 学生 -->
        <el-table-column label="学生" min-width="180">
          <template #default="scope">
            <div class="student-cell">
              <el-avatar :size="42">
                {{ (scope.row.realName || scope.row.username || '学').substring(0, 1) }}
              </el-avatar>

              <div class="student-info">
                <strong>
                  {{ scope.row.realName || '未填写姓名' }}
                </strong>

                <span>
                  {{ scope.row.username }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 学号 -->
        <el-table-column prop="studentNo" label="学号" width="140" align="center" />

        <!-- 班级 -->
        <el-table-column prop="className" label="班级" min-width="150">
          <template #default="scope">
            {{ scope.row.className || '未填写' }}
          </template>
        </el-table-column>

        <!-- 综合评分 -->
        <el-table-column label="综合评分" width="130" align="center">
          <template #default="scope">
            <span v-if="scope.row.scoreLoaded" class="score-value">
              {{ scope.row.totalScore }}
            </span>

            <span v-else class="not-loaded"> 暂无 </span>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="账号状态" width="120" align="center">
          <template #default="scope">
            <el-tag v-if="isNormal(scope.row)" type="success"> 正常 </el-tag>

            <el-tag v-else type="danger"> 已禁用 </el-tag>
          </template>
        </el-table-column>

        <!-- 操作 -->
        <el-table-column label="操作" width="310" fixed="right" align="center">
          <template #default="scope">
            <el-button type="primary" link @click="showDetail(scope.row)"> 查看详情 </el-button>

            <el-button type="success" link @click="viewScore(scope.row)"> 查看成绩 </el-button>

            <el-button
              v-if="isNormal(scope.row)"
              type="danger"
              link
              @click="disableStudent(scope.row)"
            >
              禁用账号
            </el-button>

            <el-button v-else type="success" link @click="enableStudent(scope.row)">
              启用账号
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredList.length === 0" description="没有找到学生" />
    </el-card>

    <!-- =========================
         学生详情抽屉
    ========================== -->
    <el-drawer v-model="drawerVisible" title="学生详情" size="450px">
      <div v-if="currentStudent" class="detail">
        <!-- 学生基本信息 -->
        <div class="detail-user">
          <el-avatar :size="72">
            {{ (currentStudent.realName || currentStudent.username || '学').substring(0, 1) }}
          </el-avatar>

          <div>
            <h2>
              {{ currentStudent.realName || '未填写姓名' }}
            </h2>

            <p>
              {{ currentStudent.username }}
            </p>
          </div>
        </div>

        <el-divider />

        <div class="detail-item">
          <span>学生ID</span>
          <strong>
            {{ currentStudent.id }}
          </strong>
        </div>

        <div class="detail-item">
          <span>学号</span>
          <strong>
            {{ currentStudent.studentNo || '未填写' }}
          </strong>
        </div>

        <div class="detail-item">
          <span>姓名</span>
          <strong>
            {{ currentStudent.realName || '未填写' }}
          </strong>
        </div>

        <div class="detail-item">
          <span>登录账号</span>
          <strong>
            {{ currentStudent.username || '未填写' }}
          </strong>
        </div>

        <div class="detail-item">
          <span>班级</span>
          <strong>
            {{ currentStudent.className || '未填写' }}
          </strong>
        </div>

        <div class="detail-item">
          <span>联系电话</span>
          <strong>
            {{ currentStudent.phone || '未填写' }}
          </strong>
        </div>

        <div class="detail-item">
          <span>账号状态</span>

          <el-tag v-if="isNormal(currentStudent)" type="success"> 正常 </el-tag>

          <el-tag v-else type="danger"> 已禁用 </el-tag>
        </div>

        <el-divider />

        <!-- 综合评分 -->
        <div class="score-box">
          <span> 综合评分 </span>

          <strong>
            {{ currentStudent.totalScore || 0 }}
          </strong>

          <small> 分 </small>
        </div>

        <el-button type="primary" class="drawer-button" @click="viewScore(currentStudent)">
          查看完整成绩
        </el-button>
      </div>
    </el-drawer>

    <!-- =========================
         成绩弹窗
    ========================== -->
    <el-dialog v-model="scoreDialogVisible" title="学生成绩" width="700px">
      <div v-if="currentStudent" class="score-dialog">
        <div class="score-header">
          <div>
            <strong>
              {{ currentStudent.realName || '未填写姓名' }}
            </strong>

            <span> 学号：{{ currentStudent.studentNo || '未填写' }} </span>
          </div>

          <div class="score-total">
            <span>综合评分</span>

            <strong>
              {{ scoreData.totalScore || 0 }}
            </strong>

            <small>分</small>
          </div>
        </div>

        <el-divider />

        <el-table :data="scoreData.detail || []" border stripe>
          <el-table-column prop="ruleName" label="项目" min-width="150" />

          <el-table-column prop="score" label="分数" width="100" align="center" />

          <el-table-column prop="sourceType" label="来源" width="120" align="center" />

          <el-table-column prop="createTime" label="时间" min-width="170" />
        </el-table>

        <el-empty
          v-if="!scoreLoading && (!scoreData.detail || scoreData.detail.length === 0)"
          description="暂无成绩记录"
        />
      </div>

      <template #footer>
        <el-button @click="scoreDialogVisible = false"> 关闭 </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import { User, Search, Trophy, CircleCheck, CircleClose, Refresh } from '@element-plus/icons-vue'

import request from '@/utils/request'

/* =========================
   学生数据
========================= */

const studentList = ref([])

const keyword = ref('')

const loading = ref(false)

/* =========================
   详情
========================= */

const drawerVisible = ref(false)

const currentStudent = ref(null)

/* =========================
   成绩
========================= */

const scoreDialogVisible = ref(false)

const scoreLoading = ref(false)

const scoreData = ref({
  studentName: '',
  totalScore: 0,
  avgScore: 0,
  maxScore: 0,
  minScore: 0,
  detail: [],
})

/* =========================
   查询学生列表
========================= */

function loadStudents() {
  loading.value = true

  request
    .get('/user/student/list')
    .then((res) => {
      console.log('学生列表返回：', res)

      const data = res.data.data || []

      studentList.value = data.map((student) => ({
        ...student,

        scoreLoaded: false,

        totalScore: 0,
      }))

      /*
       * 查询每个学生的综合评分
       */
      loadStudentScores()
    })
    .catch((err) => {
      console.error('学生列表加载失败：', err)

      ElMessage.error('学生列表加载失败')
    })
    .finally(() => {
      loading.value = false
    })
}

/* =========================
   查询所有学生成绩
========================= */

async function loadStudentScores() {
  for (const student of studentList.value) {
    try {
      const res = await request.get(`/scoreStatistics/${student.id}`)

      const data = res.data.data

      if (data) {
        student.totalScore = data.totalScore || 0

        student.scoreLoaded = true
      }
    } catch (error) {
      console.log(`学生 ${student.id} 成绩获取失败`, error)

      student.scoreLoaded = false
    }
  }
}

/* =========================
   搜索
========================= */

const filteredList = computed(() => {
  const text = keyword.value.trim().toLowerCase()

  if (!text) {
    return studentList.value
  }

  return studentList.value.filter((student) => {
    const studentNo = String(student.studentNo || '').toLowerCase()

    const realName = String(student.realName || '').toLowerCase()

    const username = String(student.username || '').toLowerCase()

    const className = String(student.className || '').toLowerCase()

    return (
      studentNo.includes(text) ||
      realName.includes(text) ||
      username.includes(text) ||
      className.includes(text)
    )
  })
})


/* =========================
   正常账号数量
========================= */

const normalCount = computed(() => {
  return studentList.value.filter((student) => isNormal(student)).length
})

/* =========================
   禁用账号数量
========================= */

const disabledCount = computed(() => {
  return studentList.value.filter((student) => !isNormal(student)).length
})

/* =========================
   已有成绩数量
========================= */

const scoreStudentCount = computed(() => {
  return studentList.value.filter((student) => student.scoreLoaded).length
})

/* =========================
   判断账号是否正常
========================= */

function isNormal(student) {
  /*
   * 你的后端：
   *
   * disable -> status = 0
   * enable  -> status = 1
   */

  return Number(student.status) === 1
}

/* =========================
   查看详情
========================= */

function showDetail(student) {
  currentStudent.value = student

  drawerVisible.value = true
}

/* =========================
   查看成绩
========================= */

function viewScore(student) {
  currentStudent.value = student

  scoreDialogVisible.value = true

  scoreLoading.value = true

  request
    .get(`/scoreStatistics/${student.id}`)
    .then((res) => {
      console.log('学生成绩：', res)

      scoreData.value = res.data.data || {
        studentName: student.realName,

        totalScore: 0,

        avgScore: 0,

        maxScore: 0,

        minScore: 0,

        detail: [],
      }

      /*
       * 同步更新列表中的综合评分
       */

      student.totalScore = scoreData.value.totalScore || 0

      student.scoreLoaded = true
    })
    .catch((err) => {
      console.error('成绩获取失败：', err)

      ElMessage.error('获取学生成绩失败')
    })
    .finally(() => {
      scoreLoading.value = false
    })
}

/* =========================
   禁用账号
========================= */

function disableStudent(student) {
  ElMessageBox.confirm(
    `确定要禁用「${student.realName || student.username}」的账号吗？`,
    '禁用账号',
    {
      confirmButtonText: '确定禁用',
      cancelButtonText: '取消',
      type: 'warning',
    },
  )
    .then(() => {
      return request.put(`/user/student/disable/${student.id}`)
    })
    .then(() => {
      /*
       * 只修改当前学生
       */
      student.status = 0

      ElMessage.success('账号已禁用')
    })
    .catch((error) => {
      /*
       * 用户点击取消时，
       * Element Plus 会 reject，
       * 这种情况不用提示错误。
       */

      if (error !== 'cancel' && error !== 'close') {
        console.error('禁用失败：', error)

        ElMessage.error('禁用账号失败')
      }
    })
}

/* =========================
   启用账号
========================= */

function enableStudent(student) {
  ElMessageBox.confirm(
    `确定要启用「${student.realName || student.username}」的账号吗？`,
    '启用账号',
    {
      confirmButtonText: '确定启用',
      cancelButtonText: '取消',
      type: 'success',
    },
  )
    .then(() => {
      return request.put(`/user/student/enable/${student.id}`)
    })
    .then(() => {
      /*
       * 只修改当前学生
       */
      student.status = 1

      ElMessage.success('账号已启用')
    })
    .catch((error) => {
      if (error !== 'cancel' && error !== 'close') {
        console.error('启用失败：', error)

        ElMessage.error('启用账号失败')
      }
    })
}

/* =========================
   初始化
========================= */

onMounted(() => {
  loadStudents()
})
</script>

<style scoped>
.student-page {
  width: 100%;
}

/* =========================
   顶部标题
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

  font-size: 14px;
}

/* =========================
   统计卡片
========================= */

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

.stat-icon.total {
  background: #ecf5ff;

  color: #409eff;
}

.stat-icon.normal {
  background: #f0f9eb;

  color: #67c23a;
}

.stat-icon.disabled {
  background: #fef0f0;

  color: #f56c6c;
}

.stat-icon.score {
  background: #fdf6ec;

  color: #e6a23c;
}

.stat-content {
  display: flex;

  align-items: baseline;

  flex-wrap: wrap;

  gap: 6px;
}

.stat-content span {
  width: 100%;

  color: #909399;

  font-size: 14px;
}

.stat-content strong {
  font-size: 28px;

  color: #303133;
}

.stat-content small {
  color: #909399;
}

/* =========================
   搜索栏
========================= */

.toolbar {
  display: flex;

  align-items: center;

  gap: 15px;
}

.result-text {
  color: #909399;

  font-size: 14px;
}

/* =========================
   学生单元格
========================= */

.student-cell {
  display: flex;

  align-items: center;

  gap: 12px;
}

.student-info {
  display: flex;

  flex-direction: column;

  gap: 4px;
}

.student-info strong {
  color: #303133;

  font-size: 15px;
}

.student-info span {
  color: #909399;

  font-size: 13px;
}

/* =========================
   综合评分
========================= */

.score-value {
  color: #409eff;

  font-size: 18px;

  font-weight: bold;
}

.not-loaded {
  color: #c0c4cc;

  font-size: 13px;
}

/* =========================
   详情抽屉
========================= */

.detail {
  padding: 5px 10px;
}

.detail-user {
  display: flex;

  align-items: center;

  gap: 18px;
}

.detail-user h2 {
  margin: 0 0 8px;

  color: #303133;
}

.detail-user p {
  margin: 0;

  color: #909399;
}

.detail-item {
  display: flex;

  justify-content: space-between;

  align-items: center;

  padding: 15px 0;

  border-bottom: 1px solid #f0f0f0;
}

.detail-item span {
  color: #909399;
}

.detail-item strong {
  color: #303133;
}

/* =========================
   抽屉成绩
========================= */

.score-box {
  text-align: center;

  padding: 20px;

  background: #f5f7fa;

  border-radius: 10px;

  margin-bottom: 20px;
}

.score-box span {
  display: block;

  color: #909399;

  margin-bottom: 10px;
}

.score-box strong {
  color: #409eff;

  font-size: 42px;

  margin-right: 5px;
}

.score-box small {
  color: #909399;

  font-size: 15px;
}

.drawer-button {
  width: 100%;

  height: 45px;
}

/* =========================
   成绩弹窗
========================= */

.score-dialog {
  min-height: 250px;
}

.score-header {
  display: flex;

  justify-content: space-between;

  align-items: center;
}

.score-header > div:first-child {
  display: flex;

  flex-direction: column;

  gap: 8px;
}

.score-header strong {
  font-size: 22px;

  color: #303133;
}

.score-header span {
  color: #909399;

  font-size: 14px;
}

.score-total {
  text-align: center;
}

.score-total span {
  display: block;

  margin-bottom: 4px;
}

.score-total strong {
  color: #409eff;

  font-size: 36px;

  margin-right: 4px;
}

.score-total small {
  color: #909399;
}

/* =========================
   响应式
========================= */

@media (max-width: 1100px) {
  .statistics {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 700px) {
  .statistics {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;

    align-items: flex-start;
  }
}
</style>
