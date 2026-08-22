<template>
  <div class="home">
    <!-- 欢迎卡 -->
    <el-card class="welcome">
      <div class="welcome-box">
        <div>
          <h2>
            👋 欢迎，
            {{ score.studentName }}
          </h2>

          <p>学生综合测评系统</p>
        </div>

        <div class="total">
          <span>综合评分</span>

          <strong>
            {{ score.totalScore }}
          </strong>

          <span>分</span>
        </div>
      </div>
    </el-card>

    <!-- 数据卡片 -->
    <div class="cards">
      <!-- 综合评分 -->
      <el-card class="info-card">
        <el-icon size="40">
          <Trophy />
        </el-icon>

        <p>综合评分</p>

        <strong>
          {{ score.totalScore }}
        </strong>

        <span>分</span>
      </el-card>

      <!-- 申请记录 -->
      <el-card class="info-card clickable" @click="$router.push('/home/record')">
        <el-icon size="40">
          <Document />
        </el-icon>

        <p>申请记录</p>

        <strong>
          {{ applyCount }}
        </strong>

        <span>次</span>
      </el-card>

      <!-- 已通过 -->
      <el-card class="info-card">
        <el-icon size="40">
          <CircleCheck />
        </el-icon>

        <p>已通过</p>

        <strong>
          {{ approvedCount }}
        </strong>

        <span>次</span>
      </el-card>
    </div>

    <!-- 成绩明细 -->
    <el-card class="detail-card">
      <!-- 我的部门 -->
      <el-card class="department-card">
        <div class="department-header">
          <div>
            <h2>我的部门</h2>
            <p>当前担任的部门及职务</p>
          </div>
        </div>

        <!-- 没有部门 -->
        <el-empty v-if="departments.length === 0" description="暂无部门任职" />

        <!-- 有部门 -->
        <div v-else class="department-list">
          <div
            v-for="department in departments"
            :key="department.departmentId"
            class="department-item"
          >
            <div class="department-info">
              <div class="department-name">
                {{ department.departmentName }}
              </div>

              <div class="department-position">
                身份：
                <el-tag :type="getPositionTagType(department.position)">
                  {{ department.position }}
                </el-tag>
              </div>
            </div>

            <el-button
              v-if="canDepartmentApply(department.position)"
              type="primary"
              @click="handleDepartmentApply(department)"
            >
              部门加减分申报
            </el-button>
          </div>
        </div>
      </el-card>
      <h2>成绩明细</h2>

      <el-table :data="score.detail">
        <el-table-column prop="ruleName" label="项目" />

        <el-table-column prop="score" label="分数" />

        <el-table-column prop="sourceType" label="来源" />

        <el-table-column prop="createTime" label="时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'
import { getScore } from '@/api/score'
import { Trophy, Document, CircleCheck } from '@element-plus/icons-vue'
const router = useRouter()
const score = ref({
  studentName: '',
  totalScore: 0,
  maxScore: 0,
  minScore: 0,
  detail: [],
})

const applyCount = ref(0)
const approvedCount = ref(0)
const departments = ref([])

// 函数提到顶层，模板可以访问
function canDepartmentApply(position) {
  return position === '部长' || position === '副部长' || position === '干事'
}

function getPositionTagType(position) {
  if (position === '部长') {
    return 'danger'
  }
  if (position === '副部长') {
    return 'warning'
  }
  return 'info'
}

function handleDepartmentApply(department) {
  router.push({
    path: '/home/department-apply',
    query: {
      departmentId: department.departmentId,
      departmentName: department.departmentName,
    },
  })
}

onMounted(() => {
  const userStr = localStorage.getItem('user')
  if (!userStr) return
  const user = JSON.parse(userStr)
  console.log('当前登录用户:', user)

  departments.value = user?.departments || []

  // 获取成绩
  getScore(user.id).then((res) => {
    console.log('成绩返回:', res)
    score.value = res.data.data
  })

  // 获取申请记录
  request.get('/scoreApply/my').then((res) => {
    console.log('我的申请记录:', res)
    const list = res.data.data || []
    applyCount.value = list.length
    approvedCount.value = list.filter((item) => item.status === 1).length
  })
})
</script>

<style scoped>
.home {
  padding: 30px;
}
/* =========================
   我的部门
   ========================= */

.department-card {
  margin-bottom: 20px;
}

.department-header {
  margin-bottom: 20px;
}

.department-header h2 {
  margin: 0 0 8px;
}

.department-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.department-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.department-item {
  display: flex;
  justify-content: space-between;
  align-items: center;

  padding: 18px 20px;

  border: 1px solid #ebeef5;
  border-radius: 10px;

  background: #fafafa;

  transition: all 0.2s;
}

.department-item:hover {
  border-color: #c6e2ff;
  background: #f5faff;
}

.department-name {
  margin-bottom: 10px;

  font-size: 20px;
  font-weight: 600;

  color: #303133;
}

.department-position {
  color: #606266;
  font-size: 14px;
}
/* =========================
   欢迎卡
   ========================= */

.welcome {
  margin-bottom: 20px;
}

.welcome-box {
  display: flex;

  justify-content: space-between;

  align-items: center;
}

.welcome-box h2 {
  margin: 0 0 12px;

  font-size: 28px;
}

.welcome-box p {
  margin: 0;

  font-size: 18px;

  color: #666;
}

.total {
  text-align: center;

  min-width: 120px;
}

.total span {
  display: block;

  font-size: 16px;

  color: #666;
}

.total strong {
  display: block;

  margin: 5px 0;

  font-size: 45px;

  color: #409eff;
}

/* =========================
   三张数据卡
   ========================= */

.cards {
  display: flex;

  gap: 20px;

  margin-bottom: 20px;
}

.info-card {
  flex: 1;

  text-align: center;

  padding: 10px;
}

.info-card p {
  margin: 15px 0 8px;

  font-size: 20px;
}

.info-card strong {
  display: block;

  font-size: 40px;

  color: #409eff;
}

.info-card span {
  color: #666;
}

/* 申请记录可以点击 */

.clickable {
  cursor: pointer;

  transition: all 0.2s;
}

.clickable:hover {
  transform: translateY(-4px);

  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
}

/* =========================
   成绩明细
   ========================= */

.detail-card {
  margin-bottom: 30px;
}

.detail-card h2 {
  margin-top: 0;

  margin-bottom: 20px;
}
</style>
