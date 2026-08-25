<template>
  <div class="student-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>学生管理</h2>
        <p>管理学生信息、综合评分及成绩明细</p>
      </div>

      <el-button type="primary" @click="loadStudentList" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <div class="search-row">
        <el-input v-model="search.studentNo" placeholder="请输入学号" clearable class="search-item" />
        <el-input v-model="search.realName" placeholder="请输入姓名" clearable class="search-item" />
        <el-input v-model="search.className" placeholder="请输入班级" clearable class="search-item" />
        <el-select v-model="search.status" placeholder="账号状态" clearable class="search-item">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch"> 搜索 </el-button>
        <el-button @click="resetSearch"> 重置 </el-button>
      </div>
    </el-card>

    <!-- 学生列表 -->
    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="filteredList" stripe border style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="studentNo" label="学号" min-width="130" />
        <el-table-column prop="realName" label="姓名" min-width="100" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="className" label="班级" min-width="150" />
        <el-table-column prop="phone" label="手机号" min-width="130" />

        <!-- 加分 -->
        <el-table-column label="可见加分" min-width="100" align="center">
          <template #default="{ row }">
            <span class="bonus-score"> {{ formatScoreWithPrefix(row.bonusScore, '+') }} </span>
          </template>
        </el-table-column>

        <!-- 减分 -->
        <el-table-column label="可见减分" min-width="100" align="center">
          <template #default="{ row }">
            <span class="deduct-score"> {{ formatScoreWithPrefix(row.deductScore, '-') }} </span>
          </template>
        </el-table-column>

        <!-- 当前上限 -->
        <el-table-column label="当前上限" min-width="100" align="center">
          <template #default="{ row }">
            <el-tag type="warning" v-if="row.actualLimit !== null"> {{ formatScore(row.actualLimit) }} 分 </el-tag>
            <span v-else>...</span>
          </template>
        </el-table-column>

        <!-- 最终成绩 -->
        <el-table-column label="最终成绩" min-width="110" align="center">
          <template #default="{ row }">
            <span class="total-score"> {{ formatScore(row.totalScore) }} </span>
          </template>
        </el-table-column>

        <el-table-column label="账号状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">正常</el-tag>
            <el-tag v-else type="danger">禁用</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" min-width="320" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" plain @click="showDetail(row)">查看详情</el-button>
            <el-button size="small" type="success" plain @click="showScore(row)">查看成绩</el-button>
            <el-button v-if="row.status === 1" size="small" type="danger" plain @click="disableStudent(row)">禁用</el-button>
            <el-button v-else size="small" type="success" plain @click="enableStudent(row)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && filteredList.length === 0" description="暂无学生数据" />
    </el-card>

    <!-- 成绩明细弹窗 -->
    <el-dialog v-model="scoreVisible" title="学生成绩明细" width="950px">
      <div v-if="currentStudent" class="score-summary">
        <div class="student-name">{{ currentStudent.realName }}</div>
        <div class="summary-item"><span>基础上限</span><strong> {{ formatScore(currentStudent.baseLimit) }} 分 </strong></div>
        <div class="summary-item"><span>当前最高上限</span><strong> {{ formatScore(currentStudent.actualLimit) }} 分 </strong></div>
        <div class="summary-item"><span>可见加分</span><strong class="bonus-score"> +{{ formatScore(currentStudent.bonusScore) }} </strong></div>
        <div class="summary-item"><span>可见减分</span><strong class="deduct-score"> -{{ formatScore(currentStudent.deductScore) }} </strong></div>
        <div class="summary-item final"><span>最终成绩</span><strong> {{ formatScore(currentStudent.totalScore) }} 分 </strong></div>
      </div>
      <el-divider />
      <el-table v-loading="scoreLoading" :data="scoreList" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="ruleName" label="加分项目" min-width="180" />
        <el-table-column label="分值" width="100" align="center">
          <template #default="{ row }">
            <span :class="Number(row.score) >= 0 ? 'bonus-score' : 'deduct-score'">
              {{ Number(row.score) > 0 ? '+' + formatScore(row.score) : formatScore(row.score) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="120" />
        <el-table-column prop="createTime" label="时间" min-width="170" />
        <el-table-column label="管理员状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="Number(row.adminHidden) === 1" type="info">已隐藏</el-tag>
            <el-tag v-else type="success">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="管理员操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="Number(row.adminHidden) === 1" size="small" type="warning" plain @click="showHiddenScore(row)">恢复</el-button>
            <el-button v-else size="small" type="danger" plain @click="hideScore(row)">隐藏</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="scoreVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const search = ref({ studentNo: '', realName: '', className: '', status: null })

const filteredList = computed(() => {
  return list.value.filter((student) => {
    const matchNo = !search.value.studentNo || String(student.studentNo || '').includes(search.value.studentNo)
    const matchName = !search.value.realName || String(student.realName || '').includes(search.value.realName)
    const matchClass = !search.value.className || String(student.className || '').includes(search.value.className)
    const matchStatus = search.value.status === null || student.status === search.value.status
    return matchNo && matchName && matchClass && matchStatus
  })
})

/* =========================================================
 * 加载列表：加载完基础信息立刻关闭 loading
 * ========================================================= */
async function loadStudentList() {
  loading.value = true
  try {
    const res = await request.get('/user/student/list')
    const data = res.data?.data || []

    // 初始化时给分数字段赋值 null，表示“正在加载”
    list.value = data.map(item => ({
      ...item,
      totalScore: null,
      baseLimit: null,
      bonusScore: null,
      deductScore: null,
      actualLimit: null
    }))

    // 列表基础信息到了，先让用户看到列表
    loading.value = false

    // 异步去并行加载成绩数据
    loadAllScoresParallel()
  } catch (error) {
    console.error(error)
    ElMessage.error('获取学生列表失败')
    loading.value = false
  }
}

/* =========================================================
 * 并行加载成绩：不再等待上一个完成，极速加载
 * ========================================================= */
async function loadAllScoresParallel() {
  const promises = list.value.map(async (student) => {
    try {
      const res = await request.get(`/scoreStatistics/admin/${student.id}`)
      const data = res.data?.data
      student.totalScore = data?.totalScore ?? 0
      student.baseLimit = data?.baseLimit ?? 40
      student.bonusScore = data?.bonusScore ?? 0
      student.deductScore = data?.deductScore ?? 0
      student.actualLimit = data?.actualLimit ?? 40
    } catch (error) {
      console.error(`获取学生 ${student.id} 成绩失败`, error)
      student.totalScore = 0
    }
  })
  // 同时发出所有请求，但不阻塞整页 loading
  await Promise.all(promises)
}

/* =========================================================
 * 其他功能逻辑 (保持原样)
 * ========================================================= */
function handleSearch() {} // 前端过滤无需动作
function resetSearch() { search.value = { studentNo: '', realName: '', className: '', status: null } }
function showDetail(row) { router.push(`/admin/student/${row.id}`) }

const scoreVisible = ref(false)
const scoreLoading = ref(false)
const scoreList = ref([])
const currentStudent = ref(null)

async function loadScoreDetail(studentId) {
  try {
    const res = await request.get(`/scoreStatistics/admin/${studentId}`)
    const data = res.data?.data
    scoreList.value = data?.detail || []
    if (currentStudent.value) {
      Object.assign(currentStudent.value, {
        totalScore: data?.totalScore ?? 0,
        baseLimit: data?.baseLimit ?? 40,
        bonusScore: data?.bonusScore ?? 0,
        deductScore: data?.deductScore ?? 0,
        actualLimit: data?.actualLimit ?? 40
      })
    }
  } catch (error) {
    console.error(error); ElMessage.error('获取成绩明细失败')
  }
}

async function showScore(student) {
  currentStudent.value = student
  scoreVisible.value = true
  scoreLoading.value = true
  try { await loadScoreDetail(student.id) } finally { scoreLoading.value = false }
}

async function hideScore(record) {
  try {
    await ElMessageBox.confirm('隐藏后该成绩不再参与计算。确定继续吗？', '提示', { type: 'warning' })
    await request.put(`/scoreRecord/admin/hide/${record.id}`)
    ElMessage.success('已隐藏')
    await loadScoreDetail(currentStudent.value.id)
    loadAllScoresParallel() // 局部静默更新列表分数
  } catch (e) {}
}

async function showHiddenScore(record) {
  try {
    await ElMessageBox.confirm('恢复后该成绩重新参与计算。确定继续吗？', '提示', { type: 'warning' })
    await request.put(`/scoreRecord/admin/show/${record.id}`)
    ElMessage.success('已恢复')
    await loadScoreDetail(currentStudent.value.id)
    loadAllScoresParallel()
  } catch (e) {}
}

async function disableStudent(student) {
  try {
    await ElMessageBox.confirm(`确定禁用“${student.realName}”吗？`, '提示', { type: 'warning' })
    await request.put(`/user/student/disable/${student.id}`)
    ElMessage.success('已禁用'); loadStudentList()
  } catch (e) {}
}

async function enableStudent(student) {
  try {
    await request.put(`/user/student/enable/${student.id}`)
    ElMessage.success('已启用'); loadStudentList()
  } catch (e) {}
}

/* =========================================================
 * 辅助函数：处理“正在加载”状态
 * ========================================================= */
function formatScore(score) {
  if (score === null || score === undefined) return '...'
  const value = Number(score)
  return Number.isInteger(value) ? value : value.toFixed(2)
}

function formatScoreWithPrefix(score, prefix) {
  if (score === null || score === undefined) return '...'
  return `${prefix}${formatScore(score)}`
}

onMounted(() => { loadStudentList() })
</script>

<style scoped>
.student-page { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 60px); }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0 0 6px; font-size: 24px; color: #303133; }
.page-header p { margin: 0; color: #909399; font-size: 14px; }
.search-card { margin-bottom: 20px; }
.search-row { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
.search-item { width: 180px; }
.table-card { border-radius: 8px; }
.bonus-score { color: #67c23a; font-weight: 600; }
.deduct-score { color: #f56c6c; font-weight: 600; }
.total-score { color: #409eff; font-size: 18px; font-weight: 700; }
.score-summary { display: flex; align-items: center; gap: 28px; padding: 10px 0; flex-wrap: wrap; }
.student-name { font-size: 20px; font-weight: 700; color: #303133; margin-right: 10px; }
.summary-item { display: flex; flex-direction: column; gap: 5px; color: #909399; font-size: 13px; }
.summary-item strong { font-size: 17px; color: #303133; }
.summary-item.final strong { color: #409eff; font-size: 22px; }
</style>
