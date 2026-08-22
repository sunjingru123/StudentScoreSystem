<template>
  <div class="department-audit-page">
    <el-card shadow="never">
      <template #header>
        <div class="page-header">
          <div>
            <h2>部门加减分审核</h2>
            <p>审核本部门成员提交的部门加减分申报</p>
          </div>

          <el-button :loading="loading" @click="loadList"> 刷新 </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="studentName" label="申报人" min-width="100" />

        <el-table-column prop="departmentName" label="部门" min-width="120" />

        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.scoreType === 1 ? 'success' : 'danger'">
              {{ row.scoreType === 1 ? '加分' : '减分' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="score" label="分值" width="90" />

        <el-table-column prop="title" label="项目" min-width="180" />

        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />

        <el-table-column prop="createTime" label="申报时间" min-width="170" />

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="success" size="small" @click="handleAudit(row, 1)"> 通过 </el-button>

            <el-button type="danger" size="small" @click="handleAudit(row, 2)"> 驳回 </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无待审核的部门申报" />
    </el-card>

    <!-- 审核意见 -->
    <el-dialog
      v-model="dialogVisible"
      :title="auditStatus === 1 ? '通过申请' : '驳回申请'"
      width="500px"
    >
      <el-form>
        <el-form-item label="审核意见">
          <el-input
            v-model="reviewRemark"
            type="textarea"
            :rows="5"
            maxlength="300"
            show-word-limit
            :placeholder="auditStatus === 1 ? '请输入审核意见（可不填）' : '请输入驳回原因'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false"> 取消 </el-button>

        <el-button
          :type="auditStatus === 1 ? 'success' : 'danger'"
          :loading="auditing"
          @click="submitAudit"
        >
          确认
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const list = ref([])

const loading = ref(false)

const dialogVisible = ref(false)

const auditing = ref(false)

const currentApply = ref(null)

const auditStatus = ref(null)

const reviewRemark = ref('')

async function loadList() {
  loading.value = true

  try {
    const res = await request.get('/departmentScoreApply/audit/list')

    if (res.data?.code === 200) {
      list.value = res.data.data || []
    } else {
      ElMessage.error(res.data?.message || '获取审核列表失败')
    }
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '获取审核列表失败')
  } finally {
    loading.value = false
  }
}

function handleAudit(row, status) {
  currentApply.value = row

  auditStatus.value = status

  reviewRemark.value = ''

  dialogVisible.value = true
}

async function submitAudit() {
  if (!currentApply.value) {
    return
  }

  if (auditStatus.value === 2 && !reviewRemark.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }

  try {
    await ElMessageBox.confirm(
      auditStatus.value === 1 ? '确定通过这条部门加减分申报吗？' : '确定驳回这条部门加减分申报吗？',
      '确认审核',
      {
        type: auditStatus.value === 1 ? 'success' : 'warning',
      },
    )
  } catch {
    return
  }

  auditing.value = true

  try {
    const res = await request.put(`/departmentScoreApply/audit/${currentApply.value.id}`, {
      status: auditStatus.value,
      reviewRemark: reviewRemark.value.trim(),
    })

    if (res.data?.code === 200) {
      ElMessage.success(auditStatus.value === 1 ? '审核通过' : '已驳回')

      dialogVisible.value = false

      currentApply.value = null

      await loadList()
    } else {
      ElMessage.error(res.data?.message || '审核失败')
    }
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '审核失败')
  } finally {
    auditing.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.department-audit-page {
  padding: 30px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0 0 8px;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}
</style>
