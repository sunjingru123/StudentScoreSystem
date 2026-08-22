<template>
  <div class="excel-import-page">
    <div class="page-header">
      <div>
        <h2>Excel 数据导入</h2>
        <p>用于批量导入学生名单和部门成员名单</p>
      </div>
    </div>

    <!-- 学生名单 -->
    <el-card class="import-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>学生名单导入</h3>
          <p>导入系统中的学生基本信息。学生名单不需要邮箱、性别、手机号。</p>
        </div>

        <el-tag type="primary">学生基础数据</el-tag>
      </div>

      <div class="import-content">
        <div class="excel-info">
          <div class="info-title">Excel 格式</div>

          <div class="columns">
            <el-tag>姓名</el-tag>
            <el-tag>班级</el-tag>
            <el-tag>学号</el-tag>
          </div>

          <p>导入后，如果学号已经存在，则更新学生姓名和班级； 如果不存在，则新增学生。</p>
        </div>

        <el-upload
          ref="studentUploadRef"
          class="upload-area"
          action=""
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleStudentFileChange"
          :on-remove="removeStudentFile"
        >
          <el-button type="primary"> 选择学生名单 Excel </el-button>

          <template #tip>
            <div class="upload-tip">支持 .xlsx / .xls 文件</div>
          </template>
        </el-upload>

        <div v-if="studentFile" class="selected-file">
          <el-icon>
            <Document />
          </el-icon>

          <span>{{ studentFile.name }}</span>

          <el-button type="success" :loading="studentImporting" @click="importStudents">
            导入学生名单
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 部门成员名单 -->
    <el-card class="import-card" shadow="never">
      <div class="card-header">
        <div>
          <h3>部门成员名单导入</h3>
          <p>批量设置学生所属部门以及干事、副部长、部长身份。</p>
        </div>

        <el-tag type="success">部门干部数据</el-tag>
      </div>

      <div class="import-content">
        <div class="excel-info">
          <div class="info-title">Excel 格式</div>

          <div class="columns">
            <el-tag>部门</el-tag>
            <el-tag>姓名</el-tag>
            <el-tag>学号</el-tag>
            <el-tag type="warning">职位</el-tag>
          </div>

          <p>
            职位只能填写：
            <b>干事</b>、<b>副部长</b>、<b>部长</b>。
          </p>

          <p>普通学生不需要出现在这个 Excel 中。 没有部门成员记录的学生，就是普通学生。</p>
        </div>

        <el-upload
          ref="departmentUploadRef"
          class="upload-area"
          action=""
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls"
          :on-change="handleDepartmentFileChange"
          :on-remove="removeDepartmentFile"
        >
          <el-button type="success"> 选择部门成员 Excel </el-button>

          <template #tip>
            <div class="upload-tip">支持 .xlsx / .xls 文件</div>
          </template>
        </el-upload>

        <div v-if="departmentFile" class="selected-file">
          <el-icon>
            <Document />
          </el-icon>

          <span>{{ departmentFile.name }}</span>

          <el-button type="success" :loading="departmentImporting" @click="importDepartmentMembers">
            导入部门成员
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 导入规则 -->
    <el-card class="rule-card" shadow="never">
      <template #header>
        <span>导入规则说明</span>
      </template>

      <el-alert title="学生和部门干部是两套独立数据" type="info" :closable="false" show-icon />

      <div class="rule-list">
        <p>① 所有学生首先通过“学生名单 Excel”进入 sys_user。</p>
        <p>② 干事、副部长、部长也是学生，不会改变学生角色。</p>
        <p>③ 部门成员通过“部门成员 Excel”进入 sys_user_department。</p>
        <p>④ 普通学生没有部门成员记录。</p>
        <p>⑤ 一个学生可以属于多个部门。</p>
        <p>⑥ 部长、副部长可以进行部门申报审核。</p>
        <p>⑦ 干事、副部长、部长都可以进行个人证书申报。</p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import request from '@/utils/request'

const studentUploadRef = ref(null)
const departmentUploadRef = ref(null)

const studentFile = ref(null)
const departmentFile = ref(null)

const studentImporting = ref(false)
const departmentImporting = ref(false)

/**
 * =========================
 * 学生 Excel
 * =========================
 */

function handleStudentFileChange(file) {
  studentFile.value = file.raw
}

function removeStudentFile() {
  studentFile.value = null
}

async function importStudents() {
  if (!studentFile.value) {
    ElMessage.warning('请先选择学生名单 Excel')
    return
  }

  studentImporting.value = true

  try {
    const formData = new FormData()

    formData.append('file', studentFile.value)

    const res = await request.post('/excel/import/students', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    if (res.data?.code === 200) {
      ElMessage.success(res.data?.message || '学生名单导入成功')

      studentFile.value = null
      studentUploadRef.value?.clearFiles()
    } else {
      ElMessage.error(res.data?.message || '学生名单导入失败')
    }
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '学生名单导入失败')
  } finally {
    studentImporting.value = false
  }
}

/**
 * =========================
 * 部门成员 Excel
 * =========================
 */

function handleDepartmentFileChange(file) {
  departmentFile.value = file.raw
}

function removeDepartmentFile() {
  departmentFile.value = null
}

async function importDepartmentMembers() {
  if (!departmentFile.value) {
    ElMessage.warning('请先选择部门成员 Excel')
    return
  }

  departmentImporting.value = true

  try {
    const formData = new FormData()

    formData.append('file', departmentFile.value)

    const res = await request.post('/excel/import/department-members', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    if (res.data?.code === 200) {
      ElMessage.success(res.data?.message || '部门成员导入成功')

      departmentFile.value = null
      departmentUploadRef.value?.clearFiles()
    } else {
      ElMessage.error(res.data?.message || '部门成员导入失败')
    }
  } catch (error) {
    console.error(error)

    ElMessage.error(error?.response?.data?.message || '部门成员导入失败')
  } finally {
    departmentImporting.value = false
  }
}
</script>

<style scoped>
.excel-import-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: #f5f7fa;
}

.page-header {
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
}

.import-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 25px;
}

.card-header h3 {
  margin: 0 0 8px;
  font-size: 19px;
  color: #303133;
}

.card-header p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.import-content {
  max-width: 900px;
}

.excel-info {
  padding: 16px;
  margin-bottom: 20px;
  background: #f8f9fb;
  border-radius: 8px;
}

.info-title {
  margin-bottom: 12px;
  font-weight: bold;
  color: #303133;
}

.columns {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.excel-info p {
  margin: 7px 0;
  color: #606266;
  font-size: 14px;
}

.upload-area {
  margin-bottom: 20px;
}

.upload-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.selected-file {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.selected-file span {
  flex: 1;
  color: #606266;
}

.rule-card {
  margin-bottom: 20px;
}

.rule-list {
  margin-top: 18px;
  color: #606266;
  line-height: 1.9;
}
</style>
