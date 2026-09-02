<template>
  <div class="rule-page">
    <div class="page-header">
      <div>
        <h2>规则管理</h2>
        <p>仅管理部门加减分的固定评分项目，部门临时/非固定模板不在这里维护</p>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="rules" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column prop="ruleName" label="规则名称" min-width="180" />
        <el-table-column prop="category" label="分类" width="120" align="center" />
        <el-table-column prop="score" label="分值" width="100" align="center" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
              {{ Number(row.status) === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="120" align="center">
          <template #default>
            <el-button type="primary" link>查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rules.length === 0" description="暂无规则数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const rules = ref([])
const loading = ref(false)

async function loadRules() {
  loading.value = true

  try {
    const res = await request.get('/scoreRule/list')
    const list = res.data?.data ?? res.data ?? []

    rules.value = Array.isArray(list)
      ? list.map((item) => ({
        id: item.id ?? item.ruleId ?? item.rule_id,
        ruleName: item.ruleName ?? item.name ?? item.rule_name ?? '未命名规则',
        category: item.category ?? item.type ?? item.ruleType ?? item.classify ?? '其他',
        score: item.score ?? item.ruleScore ?? item.value ?? 0,
        description: item.description ?? item.remark ?? item.memo ?? '-',
        status: item.status ?? item.enabled ?? item.state ?? 1,
        createTime: item.createTime ?? item.createdTime ?? item.create_time ?? '-',
      }))
      : []
  } catch (error) {
    console.error('加载规则失败：', error)
    rules.value = []
    ElMessage.error('规则管理加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped>
.rule-page {
  padding: 24px;
}

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

.table-card {
  border-radius: 8px;
}
</style>
