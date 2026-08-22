<template>
  <div class="score-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>成绩管理</h2>

        <p>查看和管理学生综合测评成绩</p>
      </div>
    </div>

    <!-- 搜索 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true">
        <el-form-item label="搜索">
          <el-input
            v-model="keyword"
            placeholder="学号 / 姓名 / 班级"
            clearable
            style="width: 280px"
            @keyup.enter="search"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="search"> 搜索 </el-button>

          <el-button @click="reset"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 学生列表 -->
    <el-card shadow="never" class="table-card">
      <el-table :data="filteredStudents" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="60" />

        <el-table-column prop="studentNo" label="学号" width="150" />

        <el-table-column prop="realName" label="姓名" width="120" />

        <el-table-column prop="className" label="班级" />

        <el-table-column label="综合评分" width="130">
          <template #default="{ row }">
            <span class="score">
              {{ row.totalScore }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)"> 查看成绩 </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredStudents.length === 0" description="暂无学生数据" />
    </el-card>

    <!-- 成绩详情 -->
    <el-drawer v-model="drawerVisible" title="学生成绩明细" size="720px">
      <div v-if="currentStudent" class="student-info">
        <div>
          <span>姓名：</span>

          <strong>
            {{ currentStudent.realName }}
          </strong>
        </div>

        <div>
          <span>学号：</span>

          <strong>
            {{ currentStudent.studentNo }}
          </strong>
        </div>

        <div>
          <span>班级：</span>

          <strong>
            {{ currentStudent.className }}
          </strong>
        </div>

        <div class="total-score">
          综合评分：

          <strong>
            {{ currentTotal }}
          </strong>
        </div>
      </div>

      <el-divider />

      <el-table :data="scoreDetails" border stripe v-loading="detailLoading">
        <el-table-column prop="ruleName" label="评分项目" min-width="150" />

        <el-table-column label="分数" width="100">
          <template #default="{ row }">
            <span :class="row.score >= 0 ? 'score-plus' : 'score-minus'">
              {{ row.score >= 0 ? '+' : '' }}
              {{ row.score }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="来源" width="100">
          <template #default="{ row }">
            {{ formatSource(row.sourceType) }}
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.adminHidden === 1" type="danger"> 已隐藏 </el-tag>

            <el-tag v-else type="success"> 正常 </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button v-if="row.adminHidden === 0" type="danger" link @click="handleHide(row)">
              隐藏
            </el-button>

            <el-button v-else type="success" link @click="handleShow(row)"> 恢复 </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

import { ElMessage, ElMessageBox } from 'element-plus'

import { getAdminStudentScores, getAdminStudentTotal, hideScore, showScore } from '@/api/adminScore'

import request from '@/api/request'

const loading = ref(false)

const detailLoading = ref(false)

const keyword = ref('')

const students = ref([])

const drawerVisible = ref(false)

const currentStudent = ref(null)

const scoreDetails = ref([])

const currentTotal = ref(0)

/**
 * 搜索后的学生
 */
const filteredStudents = computed(() => {
  const key = keyword.value.trim().toLowerCase()

  if (!key) {
    return students.value
  }

  return students.value.filter((item) => {
    return (
      String(item.studentNo || '')
        .toLowerCase()
        .includes(key) ||
      String(item.realName || '')
        .toLowerCase()
        .includes(key) ||
      String(item.className || '')
        .toLowerCase()
        .includes(key)
    )
  })
})


/**
 * 获取学生列表
 *
 * 直接使用你已经存在的：
 * /user/student/list
 */
async function loadStudents() {
  loading.value = true

  try {
    const res = await request.get('/user/student/list')

    const list = res.data?.data || res.data || []

    const result = []

    for (const student of list) {
      let total = 0

      try {
        const totalRes = await getAdminStudentTotal(student.id)

        total = totalRes.data?.data ?? totalRes.data ?? 0
      } catch (e) {
        console.error('获取学生总成绩失败', e)
      }

      result.push({
        ...student,

        totalScore: total,
      })
    }

    students.value = result
  } catch (error) {
    console.error(error)

    ElMessage.error('学生列表加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 搜索
 */
function search() {
  // computed 会自动过滤
}

/**
 * 重置
 */
function reset() {
  keyword.value = ''
}

/**
 * 查看成绩
 */
async function openDetail(student) {
  currentStudent.value = student

  drawerVisible.value = true

  detailLoading.value = true

  try {
    const [detailRes, totalRes] = await Promise.all([
      getAdminStudentScores(student.id),

      getAdminStudentTotal(student.id),
    ])

    scoreDetails.value = detailRes.data?.data || detailRes.data || []

    currentTotal.value = totalRes.data?.data ?? totalRes.data ?? 0
  } catch (error) {
    console.error(error)

    ElMessage.error('成绩加载失败')
  } finally {
    detailLoading.value = false
  }
}

/**
 * 隐藏
 */
async function handleHide(row) {
  try {
    await ElMessageBox.confirm(
      '隐藏后，学生和辅导员将无法看到这条成绩明细，确定继续吗？',
      '隐藏成绩',
      {
        type: 'warning',
      },
    )

    await hideScore(row.id)

    const [detailRes, totalRes] = await Promise.all([
      getAdminStudentScores(row.studentId || row.userId || row.id),
      getAdminStudentTotal(row.studentId || row.userId || row.id),
    ])

    scoreDetails.value = detailRes.data?.data || detailRes.data || []
    currentTotal.value = totalRes.data?.data ?? totalRes.data ?? 0

    if (currentStudent.value && (currentStudent.value.id === (row.studentId || row.userId || row.id))) {
      currentStudent.value.totalScore = currentTotal.value
    }

    await loadStudents()

    ElMessage.success('成绩已隐藏')
  } catch (error) {
    if (error === 'cancel') {
      return
    }

    console.error(error)

    ElMessage.error('隐藏失败')
  }
}

/**
 * 恢复
 */
async function handleShow(row) {
  try {
    await ElMessageBox.confirm(
      '恢复后，学生和辅导员将重新看到这条成绩明细，确定继续吗？',
      '恢复成绩',
      {
        type: 'info',
      },
    )

    await showScore(row.id)

    const studentId = row.studentId || row.userId || row.id

    const [detailRes, totalRes] = await Promise.all([
      getAdminStudentScores(studentId),
      getAdminStudentTotal(studentId),
    ])

    scoreDetails.value = detailRes.data?.data || detailRes.data || []
    currentTotal.value = totalRes.data?.data ?? totalRes.data ?? 0

    if (currentStudent.value && currentStudent.value.id === studentId) {
      currentStudent.value.totalScore = currentTotal.value
    }

    await loadStudents()

    ElMessage.success('成绩已恢复')
  } catch (error) {
    if (error === 'cancel') {
      return
    }

    console.error(error)

    ElMessage.error('恢复失败')
  }
}

/**
 * 来源名称
 */
function formatSource(sourceType) {
  if (sourceType === 'apply') {
    return '自主申请'
  }

  if (sourceType === 'system') {
    return '系统'
  }

  if (sourceType === 'admin') {
    return '管理员'
  }

  return sourceType || '-'
}

onMounted(() => {
  loadStudents()
})
</script>

<style scoped>
.score-page {
  padding: 24px;
}

/* 页面标题 */

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;

  font-size: 24px;

  color: #303133;
}

.page-header p {
  margin: 8px 0 0;

  color: #909399;

  font-size: 14px;
}

/* 搜索 */

.search-card {
  margin-bottom: 18px;
}

/* 表格 */

.table-card {
  border-radius: 8px;
}

/* 总分 */

.score {
  font-size: 18px;

  font-weight: 600;

  color: #409eff;
}

/* 学生信息 */

.student-info {
  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 15px;

  padding: 5px 0;
}

.student-info span {
  color: #909399;
}

.total-score {
  grid-column: 1 / 3;

  padding: 14px;

  background: #f0f9ff;

  border-radius: 8px;

  color: #606266;
}

.total-score strong {
  color: #409eff;

  font-size: 25px;

  margin-left: 8px;
}

/* 分数 */

.score-plus {
  color: #67c23a;

  font-weight: 600;
}

.score-minus {
  color: #f56c6c;

  font-weight: 600;
}
</style>
