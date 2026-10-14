<template>
  <div class="department-page">
    <div class="page-header">
      <div>
        <h2>部门管理</h2>
        <p>新增、编辑及启停部门，部门数据会同步到岗位体系</p>
      </div>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增部门
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="departments" stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="部门名称" min-width="240" />
        <el-table-column label="状态" width="120">
          <template #default="scope">
            <el-tag :type="Number(scope.row.status) === 1 ? 'success' : 'info'">
              {{ Number(scope.row.status) === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button
              v-if="Number(scope.row.status) === 1"
              link
              type="danger"
              @click="changeStatus(scope.row, false)"
            >停用</el-button>
            <el-button
              v-else
              link
              type="success"
              @click="changeStatus(scope.row, true)"
            >启用</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无部门，请先新增部门" />
        </template>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑部门' : '新增部门'"
      width="460px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="部门名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="请输入部门名称"
            maxlength="100"
            show-word-limit
            @keyup.enter="submitForm"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  addDepartment,
  disableDepartment,
  enableDepartment,
  getDepartmentManageList,
  updateDepartment
} from '@/api/department'

const departments = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const form = reactive({ name: '' })
const rules = {
  name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }]
}

function resultData(response, defaultValue) {
  if (response?.data?.data !== undefined) return response.data.data
  if (response?.data !== undefined) return response.data
  return defaultValue
}

async function loadDepartments() {
  loading.value = true
  try {
    const response = await getDepartmentManageList()
    const data = resultData(response, [])
    departments.value = Array.isArray(data) ? data : []
  } catch (error) {
    console.error('加载部门失败：', error)
    ElMessage.error('部门列表加载失败')
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  editId.value = null
  form.name = ''
  dialogVisible.value = true
}

function openEditDialog(row) {
  editId.value = row.id
  form.name = row.name || ''
  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const data = { name: form.name.trim() }
    if (editId.value) {
      await updateDepartment(editId.value, data)
      ElMessage.success('部门修改成功')
    } else {
      await addDepartment(data)
      ElMessage.success('部门新增成功')
    }
    dialogVisible.value = false
    await loadDepartments()
  } catch (error) {
    console.error('保存部门失败：', error)
    ElMessage.error(error?.response?.data?.message || '部门保存失败')
  } finally {
    saving.value = false
  }
}

async function changeStatus(row, enabled) {
  const action = enabled ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}部门“${row.name}”吗？`,
      `${action}部门`,
      { type: 'warning' }
    )
    if (enabled) await enableDepartment(row.id)
    else await disableDepartment(row.id)
    ElMessage.success(`部门已${action}`)
    await loadDepartments()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(`${action}部门失败：`, error)
      ElMessage.error(`${action}部门失败`)
    }
  }
}

onMounted(loadDepartments)
</script>

<style scoped>
.department-page { padding: 24px; }
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.page-header h2 { margin: 0 0 8px; color: #303133; }
.page-header p { margin: 0; color: #909399; }
@media (max-width: 768px) {
  .department-page { padding: 14px; }
  .page-header { align-items: flex-start; }
}
</style>
