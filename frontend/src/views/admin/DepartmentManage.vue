<template>
  <div class="department-page">
    <div class="page-header">
      <div>
        <h2>部门管理</h2>
        <p>新增、编辑及启停部门；点击“成员”可查看并管理该部门的学生</p>
      </div>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增部门
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table v-loading="loading" :data="departments" stripe>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="name" label="部门名称" min-width="220" />
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="Number(scope.row.status) === 1 ? 'success' : 'info'">
              {{ Number(scope.row.status) === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openMemberDialog(scope.row)">成员</el-button>
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

    <!-- ========================= -->
    <!-- 部门成员管理 -->
    <!-- ========================= -->
    <el-dialog
      v-model="memberDialogVisible"
      :title="`部门成员 - ${currentDepartment?.name || ''}`"
      width="780px"
      destroy-on-close
    >
      <div class="member-add">
        <el-select
          v-model="addForm.userId"
          class="member-select"
          filterable
          clearable
          :loading="studentLoading"
          placeholder="搜索并选择学生（姓名 / 学号）"
        >
          <el-option
            v-for="student in studentOptions"
            :key="student.id"
            :label="`${student.realName}（${student.studentNo}）`"
            :value="student.id"
          />
        </el-select>

        <el-select
          v-model="addForm.position"
          class="position-select"
          placeholder="职位"
        >
          <el-option
            v-for="item in positionOptions"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>

        <el-button type="primary" :loading="adding" @click="submitAddMember">
          添加成员
        </el-button>
      </div>

      <el-table
        v-loading="membersLoading"
        :data="members"
        stripe
        max-height="420"
      >
        <el-table-column prop="realName" label="姓名" min-width="120" />
        <el-table-column prop="studentNo" label="学号" min-width="140" />
        <el-table-column prop="className" label="班级" min-width="140" />
        <el-table-column prop="position" label="职位" width="100" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button link type="danger" @click="removeMember(scope.row)">移出</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="该部门暂无成员" />
        </template>
      </el-table>

      <template #footer>
        <el-button @click="memberDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

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
  addDepartmentMember,
  disableDepartment,
  enableDepartment,
  getDepartmentManageList,
  getDepartmentMembers,
  removeDepartmentMember,
  updateDepartment
} from '@/api/department'
import { getAllStudents } from '@/api/student'

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

/* 部门成员 */
const memberDialogVisible = ref(false)
const currentDepartment = ref(null)
const members = ref([])
const membersLoading = ref(false)
const studentOptions = ref([])
const studentLoading = ref(false)
const studentsLoaded = ref(false)
const positionOptions = ['干事', '副部长', '部长']
const adding = ref(false)
const addForm = reactive({ userId: null, position: '' })

async function loadDepartments() {
  loading.value = true
  try {
    const res = await getDepartmentManageList()
    if (res?.code === 200) {
      departments.value = Array.isArray(res.data) ? res.data : []
    } else {
      ElMessage.error(res?.message || '部门列表加载失败')
    }
  } catch (error) {
    console.error('加载部门失败：', error)
    ElMessage.error(error?.response?.data?.message || '部门列表加载失败')
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
    const res = editId.value
      ? await updateDepartment(editId.value, data)
      : await addDepartment(data)

    if (res?.code !== 200) {
      ElMessage.error(res?.message || '部门保存失败')
      return
    }

    ElMessage.success(editId.value ? '部门修改成功' : '部门新增成功')
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
    const res = enabled
      ? await enableDepartment(row.id)
      : await disableDepartment(row.id)

    if (res?.code !== 200) {
      ElMessage.error(res?.message || `${action}部门失败`)
      return
    }

    ElMessage.success(`部门已${action}`)
    await loadDepartments()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error(`${action}部门失败：`, error)
      ElMessage.error(`${action}部门失败`)
    }
  }
}

/* ========================= */
/* 部门成员 */
/* ========================= */

function openMemberDialog(row) {
  currentDepartment.value = row
  addForm.userId = null
  addForm.position = ''
  members.value = []
  memberDialogVisible.value = true
  loadStudents()
  loadMembers()
}

async function loadMembers() {
  if (!currentDepartment.value) return
  membersLoading.value = true
  try {
    const res = await getDepartmentMembers(currentDepartment.value.id)
    if (res?.code === 200) {
      members.value = Array.isArray(res.data) ? res.data : []
    } else {
      ElMessage.error(res?.message || '成员列表加载失败')
    }
  } catch (error) {
    console.error('加载部门成员失败：', error)
    ElMessage.error(error?.response?.data?.message || '成员列表加载失败')
  } finally {
    membersLoading.value = false
  }
}

async function loadStudents() {
  if (studentsLoaded.value) return
  studentLoading.value = true
  try {
    const res = await getAllStudents()
    if (res?.code === 200) {
      studentOptions.value = Array.isArray(res.data) ? res.data : []
      studentsLoaded.value = true
    } else {
      ElMessage.error(res?.message || '学生列表加载失败')
    }
  } catch (error) {
    console.error('加载学生列表失败：', error)
    ElMessage.error(error?.response?.data?.message || '学生列表加载失败')
  } finally {
    studentLoading.value = false
  }
}

async function submitAddMember() {
  if (!currentDepartment.value) return
  if (!addForm.userId) {
    ElMessage.warning('请选择学生')
    return
  }
  if (!addForm.position) {
    ElMessage.warning('请选择职位')
    return
  }

  adding.value = true
  try {
    const res = await addDepartmentMember(currentDepartment.value.id, {
      userId: addForm.userId,
      position: addForm.position
    })

    if (res?.code === 200) {
      ElMessage.success('成员添加成功')
      addForm.userId = null
      await loadMembers()
    } else {
      ElMessage.error(res?.message || '成员添加失败')
    }
  } catch (error) {
    console.error('添加成员失败：', error)
    ElMessage.error(error?.response?.data?.message || '成员添加失败')
  } finally {
    adding.value = false
  }
}

async function removeMember(row) {
  if (!currentDepartment.value || !row?.id) return
  try {
    await ElMessageBox.confirm(
      `确定要把“${row.realName || row.studentNo || '该学生'}”移出部门“${currentDepartment.value.name}”吗？`,
      '移出成员',
      { type: 'warning' }
    )

    const res = await removeDepartmentMember(row.id)
    if (res?.code === 200) {
      ElMessage.success('成员已移出')
      await loadMembers()
    } else {
      ElMessage.error(res?.message || '移出成员失败')
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('移出成员失败：', error)
      ElMessage.error(error?.response?.data?.message || '移出成员失败')
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

.member-add {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.member-select { flex: 1; }
.position-select { width: 130px; }

@media (max-width: 768px) {
  .department-page { padding: 14px; }
  .page-header { align-items: flex-start; }
  .member-add { flex-wrap: wrap; }
}
</style>
