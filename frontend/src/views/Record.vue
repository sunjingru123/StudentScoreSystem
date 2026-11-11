<template>
  <div class="record">
    <el-card>
      <template #header> 我的申请记录 </template>

      <el-table
        v-loading="loading"
        :data="list"
        :empty-text="loading ? '加载中…' : '暂无申请记录'"
        style="width: 100%"
      >
        <!--
          个人证书申请不绑定规则（ruleId 为空），
          后端把「项目名称」放在 title 里（= 获奖名称），
          原来的 ruleName 永远取不到值。
        -->
        <el-table-column label="项目" min-width="220">
          <template #default="scope">
            {{ getTitle(scope.row) }}
          </template>
        </el-table-column>

        <!--
          分值由档案部初审时认定：
          待审核、初审驳回的申请没有分值，这里显式提示「未认定」，
          不再是一个空白单元格。
        -->
        <el-table-column label="申请分数" width="120" align="center">
          <template #default="scope">
            <span v-if="hasScore(scope.row)" class="score">
              {{ formatScore(scope.row) }}
            </span>

            <span v-else class="empty-score"> 未认定 </span>
          </template>
        </el-table-column>

        <el-table-column label="状态">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning"> 待审核 </el-tag>

            <el-tag v-if="scope.row.status === 1" type="success"> 已通过 </el-tag>

            <el-tag v-if="scope.row.status === 2" type="danger"> 已拒绝 </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="申请时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

import { ElMessage } from 'element-plus'

import request from '@/api/request'

const list = ref([])

/*
 * 初始就是加载中，避免先闪一下「暂无申请记录」。
 */
const loading = ref(true)

/*
 * =========================================================
 * 项目名称
 *
 * 个人证书申请：title = 获奖名称
 * 其它申请：回退到规则名称
 * =========================================================
 */

function getTitle(row) {
  return row.title || row.ruleName || '—'
}

/*
 * =========================================================
 * 申请分数
 *
 * 学生提交时没有分值，初审通过后才有档案部认定分值
 * =========================================================
 */

function getScore(row) {
  return row.applyScore ?? row.score
}

function hasScore(row) {
  const score = getScore(row)

  return score !== null && score !== undefined && score !== ''
}

function formatScore(row) {
  const score = Number(getScore(row))

  if (Number.isNaN(score)) {
    return '—'
  }

  return score > 0 ? `+${score}` : `${score}`
}

async function loadRecords() {
  loading.value = true

  try {
    const res = await request.get('/scoreApply/my')

    list.value = res?.data ?? []
  } catch (error) {
    console.error('获取申请记录失败：', error)

    ElMessage.error(
      error?.response?.data?.message || '获取申请记录失败'
    )
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.record {
  padding: 30px;
}

.score {
  color: #67c23a;
  font-weight: 600;
}

.empty-score {
  color: #909399;
}
</style>
