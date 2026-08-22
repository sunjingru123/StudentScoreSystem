<template>
  <div class="score-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>综合测评成绩</h2>
        <p>查看你的加分、减分及最终综合测评成绩</p>
      </div>

      <button class="refresh-btn" @click="loadScore">↻ 刷新</button>
    </div>

    <!-- 学生信息 -->
    <div class="student-info">
      <div class="avatar">
        {{ studentName ? studentName.substring(0, 1) : '学' }}
      </div>

      <div class="student-text">
        <div class="student-name">
          {{ studentName || '学生' }}
        </div>

        <div class="student-tip">综合测评基础最高上限为 40 分</div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="score-cards">
      <!-- 基础上限 -->
      <div class="score-card blue">
        <div class="card-title">基础最高上限</div>

        <div class="card-value">
          {{ formatScore(scoreData.baseLimit) }}
        </div>

        <div class="card-desc">综合测评基础上限</div>
      </div>

      <!-- 当前上限 -->
      <div class="score-card orange">
        <div class="card-title">当前最高上限</div>

        <div class="card-value">
          {{ formatScore(scoreData.actualLimit) }}
        </div>

        <div class="card-desc">40 分减去累计减分</div>
      </div>

      <!-- 累计加分 -->
      <div class="score-card green">
        <div class="card-title">累计加分</div>

        <div class="card-value">+{{ formatScore(scoreData.bonusScore) }}</div>

        <div class="card-desc">当前可见加分总和</div>
      </div>

      <!-- 累计减分 -->
      <div class="score-card red">
        <div class="card-title">累计减分</div>

        <div class="card-value">-{{ formatScore(scoreData.deductScore) }}</div>

        <div class="card-desc">当前可见减分总和</div>
      </div>
    </div>

    <!-- 最终成绩 -->
    <div class="final-score-card">
      <div class="final-left">
        <div class="final-label">最终综合测评成绩</div>

        <div class="final-score">
          {{ formatScore(scoreData.totalScore) }}
          <span>分</span>
        </div>

        <div class="final-desc">最终成绩不会超过当前最高上限</div>
      </div>

      <div class="progress-area">
        <div class="progress-top">
          <span>当前完成度</span>

          <span> {{ progressPercent }}% </span>
        </div>

        <div class="progress">
          <div class="progress-inner" :style="{ width: progressPercent + '%' }"></div>
        </div>

        <div class="progress-bottom">
          <span>
            当前成绩：
            {{ formatScore(scoreData.totalScore) }}
          </span>

          <span>
            上限：
            {{ formatScore(scoreData.actualLimit) }}
          </span>
        </div>
      </div>
    </div>

    <!-- 成绩明细 -->
    <div class="detail-card">
      <div class="detail-header">
        <div>
          <h3>成绩明细</h3>
          <p>仅显示当前可见的加分和减分记录</p>
        </div>

        <div class="detail-count">{{ detailList.length }} 条记录</div>
      </div>

      <!-- 空数据 -->
      <div v-if="detailList.length === 0" class="empty">
        <div class="empty-icon">📋</div>

        <div class="empty-title">暂无成绩记录</div>

        <div class="empty-desc">通过活动审核后，加分记录会显示在这里</div>
      </div>

      <!-- 成绩列表 -->
      <div v-else class="detail-list">
        <div v-for="(item, index) in detailList" :key="item.id || index" class="detail-item">
          <!-- 左侧图标 -->
          <div class="detail-icon" :class="item.score >= 0 ? 'plus-icon' : 'minus-icon'">
            {{ item.score >= 0 ? '+' : '−' }}
          </div>

          <!-- 内容 -->
          <div class="detail-content">
            <div class="detail-name">
              {{ item.ruleName || '综合测评项目' }}
            </div>

            <div class="detail-meta">
              <span>
                {{ getSourceName(item.sourceType) }}
              </span>

              <span class="dot"> · </span>

              <span>
                {{ formatDate(item.createTime) }}
              </span>
            </div>
          </div>

          <!-- 分值 -->
          <div class="detail-score" :class="item.score >= 0 ? 'plus-score' : 'minus-score'">
            {{ item.score >= 0 ? '+' : '' }}{{ item.score }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

import { getScore } from '@/api/score'

/*
 * 学生姓名
 */
const studentName = ref('')

/*
 * 后端统计数据
 */
const scoreData = ref({
  baseLimit: 40,

  bonusScore: 0,

  deductScore: 0,

  actualLimit: 40,

  totalScore: 0,

  avgScore: 0,

  maxScore: 0,

  minScore: 0,

  detail: [],
})

/*
 * 成绩明细
 */
const detailList = computed(() => {
  return scoreData.value.detail || []
})

/*
 * 当前成绩完成度
 *
 * 例如：
 *
 * 当前成绩 20
 * 当前上限 40
 *
 * = 50%
 */
const progressPercent = computed(() => {
  const total = Number(scoreData.value.totalScore || 0)

  const limit = Number(scoreData.value.actualLimit || 0)

  if (limit <= 0) {
    return 0
  }

  const percent = (total / limit) * 100

  return Math.min(Math.max(percent, 0), 100).toFixed(0)
})

/*
 * 格式化分数
 */
function formatScore(value) {
  if (value === null || value === undefined || value === '') {
    return '0'
  }

  const number = Number(value)

  if (Number.isNaN(number)) {
    return '0'
  }

  /*
   * 整数不显示 .00
   */
  if (Number.isInteger(number)) {
    return number
  }

  return number.toFixed(2)
}

/*
 * 来源名称
 */
function getSourceName(sourceType) {
  if (!sourceType) {
    return '综合测评'
  }

  const map = {
    apply: '自主申报',

    activity: '活动加分',

    manual: '人工调整',

    admin: '管理员调整',

    system: '系统记录',
  }

  return map[sourceType] || sourceType
}

/*
 * 日期格式
 */
function formatDate(value) {
  if (!value) {
    return ''
  }

  try {
    const date = new Date(value)

    if (Number.isNaN(date.getTime())) {
      return value
    }

    const year = date.getFullYear()

    const month = String(date.getMonth() + 1).padStart(2, '0')

    const day = String(date.getDate()).padStart(2, '0')

    return `${year}-${month}-${day}`
  } catch (error) {
    return value
  }
}

/*
 * 获取当前登录学生
 */
function getCurrentUser() {
  const userStr = localStorage.getItem('user')

  if (!userStr) {
    return null
  }

  try {
    return JSON.parse(userStr)
  } catch (error) {
    console.error('读取用户信息失败', error)

    return null
  }
}

/*
 * 查询成绩
 */
async function loadScore() {
  const user = getCurrentUser()

  if (!user) {
    console.warn('当前没有登录用户')

    return
  }

  /*
   * 兼容不同字段
   */
  const studentId = user.id || user.userId

  if (!studentId) {
    console.error('无法获取学生ID')

    return
  }

  try {
    const res = await getScore(studentId)

    console.log('成绩统计接口返回：', res)

    /*
     * 你的 Result 一般结构：
     *
     * {
     *   code: 200,
     *   data: {...}
     * }
     *
     * 如果 axios request.js 已经返回 data，
     * 那么这里也兼容。
     */
    let data = res?.data?.data || res?.data || res

    if (!data) {
      console.warn('成绩数据为空')

      return
    }

    /*
     * 保存学生姓名
     */
    studentName.value = data.studentName || user.realName || user.username || '学生'

    /*
     * 保存统计数据
     */
    scoreData.value = {
      baseLimit: data.baseLimit ?? 40,

      bonusScore: data.bonusScore ?? 0,

      deductScore: data.deductScore ?? 0,

      actualLimit: data.actualLimit ?? 40,

      totalScore: data.totalScore ?? 0,

      avgScore: data.avgScore ?? 0,

      maxScore: data.maxScore ?? 0,

      minScore: data.minScore ?? 0,

      detail: data.detail || [],
    }
  } catch (error) {
    console.error('查询成绩失败：', error)
  }
}

/*
 * 页面加载
 */
onMounted(() => {
  loadScore()
})
</script>

<style scoped>
.score-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 64px);
}

/* 页面标题 */

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
  font-size: 24px;
  color: #1f2937;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

/* 刷新按钮 */

.refresh-btn {
  border: none;
  background: #409eff;
  color: white;
  padding: 9px 18px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
}

.refresh-btn:hover {
  background: #337ecc;
}

/* 学生信息 */

.student-info {
  display: flex;
  align-items: center;
  background: white;
  padding: 20px;
  border-radius: 14px;
  margin-bottom: 20px;
}

.avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 22px;
  font-weight: bold;
  margin-right: 14px;
}

.student-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.student-tip {
  margin-top: 5px;
  color: #909399;
  font-size: 13px;
}

/* 统计卡片 */

.score-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);

  gap: 16px;

  margin-bottom: 20px;
}

.score-card {
  background: white;
  border-radius: 14px;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

.score-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 4px;
  height: 100%;
}

.score-card.blue::before {
  background: #409eff;
}

.score-card.orange::before {
  background: #e6a23c;
}

.score-card.green::before {
  background: #67c23a;
}

.score-card.red::before {
  background: #f56c6c;
}

.card-title {
  color: #606266;
  font-size: 14px;
}

.card-value {
  font-size: 30px;
  font-weight: 700;
  margin: 12px 0 6px;
  color: #303133;
}

.card-desc {
  font-size: 12px;
  color: #a8abb2;
}

/* 最终成绩 */

.final-score-card {
  background: white;
  border-radius: 16px;
  padding: 28px;
  margin-bottom: 20px;

  display: flex;
  align-items: center;
  gap: 50px;
}

.final-left {
  min-width: 240px;
}

.final-label {
  font-size: 15px;
  color: #606266;
}

.final-score {
  font-size: 52px;
  font-weight: 700;
  color: #409eff;
  margin: 10px 0;
}

.final-score span {
  font-size: 18px;
  font-weight: normal;
  margin-left: 5px;
}

.final-desc {
  color: #909399;
  font-size: 13px;
}

/* 进度 */

.progress-area {
  flex: 1;
}

.progress-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
  color: #606266;
  font-size: 14px;
}

.progress {
  height: 12px;
  background: #ebeef5;
  border-radius: 10px;
  overflow: hidden;
}

.progress-inner {
  height: 100%;
  background: #409eff;
  border-radius: 10px;
  transition: width 0.4s ease;
}

.progress-bottom {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

/* 明细 */

.detail-card {
  background: white;
  border-radius: 16px;
  overflow: hidden;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 22px 24px;
  border-bottom: 1px solid #ebeef5;
}

.detail-header h3 {
  margin: 0 0 5px;
  font-size: 18px;
  color: #303133;
}

.detail-header p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}

.detail-count {
  color: #909399;
  font-size: 13px;
}

/* 明细列表 */

.detail-list {
  padding: 0 24px;
}

.detail-item {
  display: flex;
  align-items: center;
  padding: 18px 0;
  border-bottom: 1px solid #f0f2f5;
}

.detail-item:last-child {
  border-bottom: none;
}

/* 图标 */

.detail-icon {
  width: 38px;
  height: 38px;
  border-radius: 50%;

  display: flex;
  align-items: center;
  justify-content: center;

  font-size: 20px;
  font-weight: bold;

  margin-right: 14px;
}

.plus-icon {
  background: #f0f9eb;
  color: #67c23a;
}

.minus-icon {
  background: #fef0f0;
  color: #f56c6c;
}

/* 内容 */

.detail-content {
  flex: 1;
}

.detail-name {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}

.detail-meta {
  margin-top: 6px;
  color: #909399;
  font-size: 12px;
}

.dot {
  margin: 0 5px;
}

/* 分数 */

.detail-score {
  font-size: 18px;
  font-weight: 600;
}

.plus-score {
  color: #67c23a;
}

.minus-score {
  color: #f56c6c;
}

/* 空状态 */

.empty {
  text-align: center;
  padding: 70px 20px;
}

.empty-icon {
  font-size: 42px;
  margin-bottom: 12px;
}

.empty-title {
  font-size: 16px;
  color: #606266;
}

.empty-desc {
  margin-top: 8px;
  color: #a8abb2;
  font-size: 13px;
}

/* 响应式 */

@media (max-width: 1100px) {
  .score-cards {
    grid-template-columns: repeat(2, 1fr);
  }

  .final-score-card {
    flex-direction: column;
    align-items: stretch;
    gap: 25px;
  }
}

@media (max-width: 650px) {
  .score-page {
    padding: 15px;
  }

  .score-cards {
    grid-template-columns: 1fr;
  }

  .page-header {
    align-items: flex-start;
  }

  .final-score {
    font-size: 42px;
  }
}
</style>
