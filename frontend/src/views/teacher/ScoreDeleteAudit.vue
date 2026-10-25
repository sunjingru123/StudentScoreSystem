<template>
  <div class="delete-audit-page">

    <div class="page-header">

      <div>
        <h2>成绩删除申请审核</h2>

        <p>
          审核档案部成员提交的学生加减分删除申请，审核通过后立即删除对应成绩
        </p>
      </div>

    </div>


    <el-card class="audit-card">

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">

        <!-- 待审核 -->

        <el-tab-pane label="待审核" name="pending">

          <el-button
            class="batch-approve"
            type="success"
            :loading="batchLoading"
            :disabled="loading || selectedRows.length === 0"
            @click="handleBatchApprove"
          >
            一键审批通过（已选 {{ selectedRows.length }} 条）
          </el-button>

          <el-table
            v-loading="loading || batchLoading"
            :data="pendingList"
            row-key="id"
            @selection-change="selectedRows = $event"
            border
            stripe
            empty-text="暂无待审核的删除申请"
          >

            <el-table-column type="selection" width="48" />

            <el-table-column prop="studentName" label="学生" width="110" />

            <el-table-column prop="studentNo" label="学号" width="140" />

            <el-table-column prop="className" label="班级" width="130" />

            <el-table-column prop="projectName" label="加减分项目" min-width="160" show-overflow-tooltip />

            <el-table-column prop="score" label="分值" width="90" />

            <el-table-column prop="applicantName" label="申请人" width="110" />

            <el-table-column prop="reason" label="删除原因" min-width="180" show-overflow-tooltip />

            <el-table-column prop="createTime" label="申请时间" width="170" />

            <el-table-column label="操作" width="170" fixed="right">

              <template #default="{ row }">

                <el-button
                  type="success"
                  size="small"
                  @click="handleApprove(row)"
                >
                  通过
                </el-button>

                <el-button
                  type="danger"
                  size="small"
                  @click="handleReject(row)"
                >
                  驳回
                </el-button>

              </template>

            </el-table-column>

          </el-table>

        </el-tab-pane>


        <!-- 已处理 -->

        <el-tab-pane label="已处理" name="processed">

          <el-table
            v-loading="loading"
            :data="processedList"
            border
            stripe
            empty-text="暂无已处理的删除申请"
          >

            <el-table-column prop="studentName" label="学生" width="110" />

            <el-table-column prop="studentNo" label="学号" width="140" />

            <el-table-column prop="projectName" label="加减分项目" min-width="160" show-overflow-tooltip />

            <el-table-column prop="score" label="分值" width="90" />

            <el-table-column prop="applicantName" label="申请人" width="110" />

            <el-table-column prop="statusText" label="状态" width="100" />

            <el-table-column prop="reviewerName" label="审核人" width="110" />

            <el-table-column prop="reviewRemark" label="审核意见" min-width="150" show-overflow-tooltip />

            <el-table-column prop="reviewTime" label="审核时间" width="170" />

          </el-table>

        </el-tab-pane>

      </el-tabs>

    </el-card>

  </div>
</template>


<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  getPendingDeleteApplies,
  getProcessedDeleteApplies,
  approveDeleteApply,
  rejectDeleteApply
} from '@/api/scoreDeleteApply'


const activeTab = ref('pending')

const loading = ref(false)

const pendingList = ref([])

const processedList = ref([])
const selectedRows = ref([])
const batchLoading = ref(false)

async function handleBatchApprove() {
  if (batchLoading.value || selectedRows.value.length === 0) return
  const rows = [...selectedRows.value]
  batchLoading.value = true
  try {
    await ElMessageBox.confirm(
      `确认通过选中的 ${rows.length} 条删除申请？通过后将立即作废并隐藏对应的加减分成绩。`,
      '一键审批通过',
      { type: 'warning', confirmButtonText: '确认通过并删除', cancelButtonText: '取消' }
    )

    let successCount = 0
    const failures = []
    for (const row of rows) {
      try {
        const result = unwrap(await approveDeleteApply(row.id, '一键审批通过'))
        if (result?.code === 200 || result?.code === 0) {
          successCount++
        } else {
          failures.push(`申请 ${row.id}：${result?.message || '审核失败'}`)
        }
      } catch (error) {
        failures.push(`申请 ${row.id}：${error?.response?.data?.message || error?.message || '请求失败，请刷新确认处理状态'}`)
      }
    }

    selectedRows.value = []
    await loadPending()
    if (failures.length) {
      await ElMessageBox.alert(
        `成功 ${successCount} 条，失败 ${failures.length} 条。\n${failures.join('\n')}`,
        '批量审批结果',
        { type: 'warning' }
      )
    } else {
      ElMessage.success(`已通过 ${successCount} 条申请并删除对应成绩`)
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('批量审批失败：', error)
      ElMessage.error('批量审批失败，请刷新确认处理状态')
    }
  } finally {
    batchLoading.value = false
  }
}


function unwrap(res) {

  return res?.data?.code !== undefined
    ? res.data
    : res

}


async function loadPending() {

  loading.value = true
  selectedRows.value = []

  try {

    const result = unwrap(await getPendingDeleteApplies())

    if (result?.code === 200 || result?.code === 0) {

      pendingList.value = result.data || []

    } else {

      ElMessage.error(result?.message || '获取待审核列表失败')

    }

  } catch (error) {

    console.error('获取待审核列表失败：', error)

    ElMessage.error('获取待审核列表失败')

  } finally {

    loading.value = false

  }

}


async function loadProcessed() {

  loading.value = true

  try {

    const result = unwrap(await getProcessedDeleteApplies())

    if (result?.code === 200 || result?.code === 0) {

      processedList.value = result.data || []

    } else {

      ElMessage.error(result?.message || '获取已处理列表失败')

    }

  } catch (error) {

    console.error('获取已处理列表失败：', error)

    ElMessage.error('获取已处理列表失败')

  } finally {

    loading.value = false

  }

}


function handleTabChange(name) {

  if (name === 'processed') {

    loadProcessed()

  } else {

    loadPending()

  }

}


async function handleApprove(row) {

  try {

    const { value } = await ElMessageBox.prompt(

      `确认删除【${row.studentName}】的加减分【${row.projectName || ''}】？`,

      '审核通过',

      {

        confirmButtonText: '确认通过',

        cancelButtonText: '取消',

        inputPlaceholder: '审核意见（可选）',

        inputValue: ''

      }

    )

    const result = unwrap(await approveDeleteApply(row.id, value || ''))

    if (result?.code === 200 || result?.code === 0) {

      ElMessage.success('已通过并删除对应成绩')

      loadPending()

    } else {

      ElMessage.error(result?.message || '审核失败')

    }

  } catch (error) {

    if (error === 'cancel' || error === 'close') {

      return

    }

    console.error('审核通过失败：', error)

    ElMessage.error('审核失败')

  }

}


async function handleReject(row) {

  try {

    const { value } = await ElMessageBox.prompt(

      '请输入驳回原因',

      '驳回删除申请',

      {

        confirmButtonText: '确认驳回',

        cancelButtonText: '取消',

        inputPlaceholder: '驳回原因',

        inputValue: ''

      }

    )

    const result = unwrap(await rejectDeleteApply(row.id, value || ''))

    if (result?.code === 200 || result?.code === 0) {

      ElMessage.success('已驳回')

      loadPending()

    } else {

      ElMessage.error(result?.message || '驳回失败')

    }

  } catch (error) {

    if (error === 'cancel' || error === 'close') {

      return

    }

    console.error('驳回失败：', error)

    ElMessage.error('驳回失败')

  }

}


onMounted(() => {

  loadPending()

})
</script>


<style scoped>

.delete-audit-page {

  padding: 24px;

  background: #f5f7fa;

  min-height: calc(100vh - 120px);

}


.page-header {

  margin-bottom: 20px;

}


.page-header h2 {

  margin: 0 0 8px;

  font-size: 26px;

  color: #303133;

}


.page-header p {

  margin: 0;

  color: #909399;

  font-size: 14px;

}


.batch-approve {
  margin-bottom: 16px;
}

.audit-card {

  border-radius: 12px;

}

</style>
