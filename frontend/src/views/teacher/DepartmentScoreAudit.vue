<template>
  <div class="page">
    <div class="page-header">
      <div>
        <h2>部门加减分审核</h2>
        <p>审核部门负责人提交的学生加减分申报</p>
      </div>

      <el-button type="primary" :loading="loading" @click="loadList"> 刷新 </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="list" border stripe style="width: 100%">
        <el-table-column type="index" label="#" width="60" align="center" />

        <el-table-column prop="studentName" label="学生" min-width="100" />

        <el-table-column prop="departmentName" label="部门" min-width="120" />

        <el-table-column prop="title" label="申报项目" min-width="180" />

        <el-table-column label="类型" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.scoreType === 1" type="success"> 加分 </el-tag>

            <el-tag v-else type="danger"> 减分 </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="分值" width="90" align="center">
          <template #default="{ row }">
            <span :class="row.scoreType === 1 ? 'bonus' : 'deduct'">
              {{ row.scoreType === 1 ? '+' : '-' }}{{ row.score }}
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />

        <el-table-column
          prop="reviewRemark"
          label="部门审核意见"
          min-width="180"
          show-overflow-tooltip
        />

        <el-table-column prop="createTime" label="申报时间" min-width="170" />

        <el-table-column label="凭证" width="90" align="center">
          <template #default="{ row }">
            <el-button
              v-if="row.evidenceUrl"
              type="primary"
              link
              @click="openEvidence(row.evidenceUrl)"
            >
              查看
            </el-button>

            <span v-else>无</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="170" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleAudit(row, 1)"> 通过 </el-button>

            <el-button type="danger" size="small" @click="handleAudit(row, 2)"> 驳回 </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无待审核申请" />
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="dialogVisible" title="最终审核" width="500px">
      <div v-if="currentRow" class="detail">
        <p>
          <strong>学生：</strong>
          {{ currentRow.studentName }}
        </p>

        <p>
          <strong>部门：</strong>
          {{ currentRow.departmentName }}
        </p>

        <p>
          <strong>项目：</strong>
          {{ currentRow.title }}
        </p>

        <p>
          <strong>分值：</strong>

          <span :class="currentRow.scoreType === 1 ? 'bonus' : 'deduct'">
            {{ currentRow.scoreType === 1 ? '+' : '-' }}{{ currentRow.score }}
          </span>
        </p>

        <p>
          <strong>申报说明：</strong>
          {{ currentRow.description || '无' }}
        </p>
      </div>

      <el-input v-model="reviewRemark" type="textarea" :rows="5" placeholder="请输入审核意见" />

      <template #footer>
        <el-button @click="dialogVisible = false"> 取消 </el-button>

        <el-button
          :type="auditStatus === 1 ? 'success' : 'danger'"
          :loading="auditLoading"
          @click="submitAudit"
        >
          {{ auditStatus === 1 ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const list = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const auditLoading = ref(false)

const currentRow = ref(null)
const auditStatus = ref(1)
const reviewRemark = ref('')

async function loadList() {
  loading.value = true

  try {
    const res = await request.get('/departmentScoreApply/final-audit/list')

    list.value = res.data?.data || []
  } catch (error) {
    console.error(error)

    ElMessage.error('获取待审核列表失败')
  } finally {
    loading.value = false
  }
}

function handleAudit(row, status) {
  currentRow.value = row
  auditStatus.value = status
  reviewRemark.value = ''

  dialogVisible.value = true
}

async function submitAudit() {
  if (!currentRow.value) {
    return
  }

  const text = auditStatus.value === 1 ? '确定通过这条申报吗？' : '确定驳回这条申报吗？'

  try {
    await ElMessageBox.confirm(text, '提示', {
      type: auditStatus.value === 1 ? 'success' : 'warning',
    })
  } catch {
    return
  }

  auditLoading.value = true

  try {
    await request.put(`/departmentScoreApply/final-audit/${currentRow.value.id}`, {
      status: auditStatus.value,
      reviewRemark: reviewRemark.value,
    })

    ElMessage.success(auditStatus.value === 1 ? '审核通过' : '已驳回')

    dialogVisible.value = false

    await loadList()
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '审核失败')
  } finally {
    auditLoading.value = false
  }
}

function openEvidence(url) {
  if (!url) {
    return
  }

  window.open(url, '_blank')
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.page {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.bonus {
  color: #67c23a;
  font-weight: 700;
}

.deduct {
  color: #f56c6c;
  font-weight: 700;
}

.detail {
  margin-bottom: 20px;
}

.detail p {
  margin: 10px 0;
}
</style>
