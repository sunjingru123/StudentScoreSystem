<template>
  <div class="teacher-manage">
    <!-- 页面头部 -->
    <div class="page-header">
      <div>
        <h2>教师管理</h2>
        <p>管理系统辅导员账号及负责部门</p>
      </div>

      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        新增教师
      </el-button>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :inline="true">
        <el-form-item label="教师姓名">
          <el-input
            v-model="search.realName"
            placeholder="请输入教师姓名"
            clearable
            @keyup.enter="loadTeacherList"
          />
        </el-form-item>

        <el-form-item label="管理部门">
          <el-select
            v-model="search.departmentId"
            placeholder="全部部门"
            clearable
            style="width: 180px"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="状态">
          <el-select
            v-model="search.status"
            placeholder="全部状态"
            clearable
            style="width: 140px"
          >
            <el-option label="正常" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="loadTeacherList">
            查询
          </el-button>

          <el-button @click="resetSearch">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 教师列表 -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="filteredList"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column
          type="index"
          label="序号"
          width="70"
          align="center"
        />

        <el-table-column
          prop="realName"
          label="教师姓名"
          min-width="120"
        />

        <el-table-column
          prop="departmentName"
          label="管理部门"
          min-width="160"
        >
          <template #default="{ row }">
            <el-tag v-if="row.departmentName" type="primary">
              {{ row.departmentName }}
            </el-tag>

            <span v-else class="empty-text">
              未绑定部门
            </span>
          </template>
        </el-table-column>

        <el-table-column
          prop="username"
          label="账号"
          min-width="160"
        />

        <el-table-column
          label="密码"
          min-width="120"
        >
          <template #default>
            <span class="password-text">******</span>
          </template>
        </el-table-column>

        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="{ row }">
            <el-tag
              v-if="Number(row.status) === 1"
              type="success"
            >
              正常
            </el-tag>

            <el-tag
              v-else
              type="info"
            >
              停用
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          label="操作"
          min-width="260"
          fixed="right"
          align="center"
        >
          <template #default="{ row }">

            <el-button
              size="small"
              type="primary"
              plain
              @click="openEditDialog(row)"
            >
              编辑
            </el-button>

            <el-button
              size="small"
              type="warning"
              plain
              @click="resetPassword(row)"
            >
              重置密码
            </el-button>

            <el-button
              v-if="Number(row.status) === 1"
              size="small"
              type="danger"
              plain
              @click="disableTeacher(row)"
            >
              停用
            </el-button>

            <el-button
              v-else
              size="small"
              type="success"
              plain
              @click="enableTeacher(row)"
            >
              启用
            </el-button>

          </template>
        </el-table-column>
      </el-table>

      <div
        v-if="!loading && filteredList.length === 0"
        class="empty-box"
      >
        暂无教师数据
      </div>
    </el-card>

    <!-- 新增 / 编辑教师 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑教师' : '新增教师'"
      width="500px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="90px"
      >

        <el-form-item label="教师姓名" prop="realName">
          <el-input
            v-model="form.realName"
            placeholder="请输入教师姓名"
          />
        </el-form-item>

        <el-form-item label="管理部门" prop="departmentId">
          <el-select
            v-model="form.departmentId"
            placeholder="请选择管理部门"
            style="width: 100%"
          >
            <el-option
              v-for="item in departments"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入登录账号"
          />
          <div class="form-tip">
            建议使用教师姓名的完整拼音，例如：孙靖茹 → sunjingru
          </div>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
          />
          <div class="form-tip">
            默认密码：123456
          </div>
        </el-form-item>

      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>

        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submitTeacher"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

import {
  getTeacherList,
  getTeacherDepartments,
  addTeacher,
  updateTeacher,
  disableTeacher as apiDisableTeacher,
  enableTeacher as apiEnableTeacher,
  resetTeacherPassword
} from '@/api/teacher'

// =========================================================
// 数据
// =========================================================

const loading = ref(false)
const submitLoading = ref(false)

const list = ref([])
const departments = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)

const formRef = ref(null)

const search = ref({
  realName: '',
  departmentId: null,
  status: null
})

const form = ref({
  id: null,
  realName: '',
  departmentId: null,
  username: '',
  password: '123456'
})

// =========================================================
// 表单验证
// =========================================================

const rules = {
  realName: [
    {
      required: true,
      message: '请输入教师姓名',
      trigger: 'blur'
    }
  ],

  departmentId: [
    {
      required: true,
      message: '请选择管理部门',
      trigger: 'change'
    }
  ],

  username: [
    {
      required: true,
      message: '请输入账号',
      trigger: 'blur'
    }
  ],

  password: [
    {
      required: true,
      message: '请输入密码',
      trigger: 'blur'
    },
    {
      min: 6,
      message: '密码至少6位',
      trigger: 'blur'
    }
  ]
}

// =========================================================
// 前端筛选
// =========================================================

const filteredList = computed(() => {
  return list.value.filter(item => {

    const matchName =
      !search.value.realName ||
      String(item.realName || '').includes(search.value.realName)

    const matchDepartment =
      search.value.departmentId === null ||
      Number(item.departmentId) === Number(search.value.departmentId)

    const matchStatus =
      search.value.status === null ||
      Number(item.status) === Number(search.value.status)

    return matchName && matchDepartment && matchStatus
  })
})

// =========================================================
// 加载教师
// =========================================================

async function loadTeacherList() {
  loading.value = true

  try {
    const res = await getTeacherList()

    if (res.data?.code === 200) {
      list.value = res.data.data || []
    } else {
      ElMessage.error(res.data?.message || '获取教师列表失败')
    }

  } catch (error) {
    console.error(error)
    ElMessage.error('获取教师列表失败')
  } finally {
    loading.value = false
  }
}

// =========================================================
// 加载部门
// =========================================================

async function loadDepartments() {
  try {
    const res = await getTeacherDepartments()

    if (res.data?.code === 200) {
      departments.value = res.data.data || []
    } else {
      ElMessage.error(res.data?.message || '获取部门失败')
    }

  } catch (error) {
    console.error(error)
    ElMessage.error('获取部门失败')
  }
}

// =========================================================
// 重置搜索
// =========================================================

function resetSearch() {
  search.value = {
    realName: '',
    departmentId: null,
    status: null
  }

  loadTeacherList()
}

// =========================================================
// 新增
// =========================================================

function openAddDialog() {
  isEdit.value = false

  form.value = {
    id: null,
    realName: '',
    departmentId: null,
    username: '',
    password: '123456'
  }

  dialogVisible.value = true
}

// =========================================================
// 编辑
// =========================================================

function openEditDialog(row) {
  isEdit.value = true

  form.value = {
    id: row.id,
    realName: row.realName || '',
    departmentId: row.departmentId || null,
    username: row.username || '',
    password: ''
  }

  dialogVisible.value = true
}

// =========================================================
// 提交
// =========================================================

async function submitTeacher() {
  if (!formRef.value) {
    return
  }

  await formRef.value.validate(async valid => {
    if (!valid) {
      return
    }

    submitLoading.value = true

    try {
      let res

      if (isEdit.value) {
        res = await updateTeacher(form.value.id, {
          realName: form.value.realName,
          departmentId: form.value.departmentId,
          username: form.value.username,
          password: form.value.password
        })
      } else {
        res = await addTeacher({
          realName: form.value.realName,
          departmentId: form.value.departmentId,
          username: form.value.username,
          password: form.value.password || '123456'
        })
      }

      if (res.data?.code === 200) {
        ElMessage.success(
          isEdit.value ? '教师修改成功' : '教师新增成功'
        )

        dialogVisible.value = false
        await loadTeacherList()
      } else {
        ElMessage.error(res.data?.message || '操作失败')
      }

    } catch (error) {
      console.error(error)
      ElMessage.error(
        error.response?.data?.message || '操作失败'
      )
    } finally {
      submitLoading.value = false
    }
  })
}

// =========================================================
// 停用
// =========================================================

async function disableTeacher(row) {
  try {
    await ElMessageBox.confirm(
      `确定要停用教师「${row.realName}」吗？停用后该账号将无法登录。`,
      '停用教师',
      {
        type: 'warning'
      }
    )

    const res = await apiDisableTeacher(row.id)

    if (res.data?.code === 200) {
      ElMessage.success('教师已停用')
      await loadTeacherList()
    } else {
      ElMessage.error(res.data?.message || '停用失败')
    }

  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('停用失败')
    }
  }
}

// =========================================================
// 启用
// =========================================================

async function enableTeacher(row) {
  try {
    const res = await apiEnableTeacher(row.id)

    if (res.data?.code === 200) {
      ElMessage.success('教师已启用')
      await loadTeacherList()
    } else {
      ElMessage.error(res.data?.message || '启用失败')
    }

  } catch (error) {
    console.error(error)
    ElMessage.error('启用失败')
  }
}

// =========================================================
// 重置密码
// =========================================================

async function resetPassword(row) {
  try {
    await ElMessageBox.confirm(
      `确定将「${row.realName}」的密码重置为 123456 吗？`,
      '重置密码',
      {
        type: 'warning'
      }
    )

    const res = await resetTeacherPassword(row.id)

    if (res.data?.code === 200) {
      ElMessage.success('密码已重置为 123456')
    } else {
      ElMessage.error(res.data?.message || '重置密码失败')
    }

  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error('重置密码失败')
    }
  }
}

// =========================================================
// 初始化
// =========================================================

onMounted(() => {
  loadDepartments()
  loadTeacherList()
})
</script>

<style scoped>
.teacher-manage {
  padding: 24px;
  min-height: 100%;
  background: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 8px;
  font-size: 24px;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  min-height: 300px;
}

.password-text {
  color: #909399;
  letter-spacing: 2px;
}

.empty-text {
  color: #c0c4cc;
}

.empty-box {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.form-tip {
  margin-top: 5px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}

@media (max-width: 768px) {
  .teacher-manage {
    padding: 12px;
  }

  .page-header {
    align-items: flex-start;
    gap: 12px;
  }

  .page-header h2 {
    font-size: 20px;
  }

  .search-card :deep(.el-form-item) {
    width: 100%;
    margin-right: 0;
  }

  .search-card :deep(.el-input),
  .search-card :deep(.el-select) {
    width: 100% !important;
  }
}
</style>
