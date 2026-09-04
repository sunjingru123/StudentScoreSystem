<template>
  <div class="excel-import-page">

    <div class="page-header">
      <div>
        <h2>Excel 数据导入</h2>
        <p>用于批量导入学生名单和部门成员名单</p>
      </div>
    </div>

    <!-- ========================= -->
    <!-- 学生名单 -->
    <!-- ========================= -->
    <el-card class="import-card" shadow="never">

      <div class="card-header">
        <div>
          <h3>学生名单导入</h3>
          <p>
            导入系统中的学生基本信息。
            学生名单不需要邮箱、性别、手机号。
          </p>
        </div>

        <el-tag type="primary">
          学生基础数据
        </el-tag>
      </div>

      <div class="import-content">

        <div class="excel-info">

          <div class="info-title">
            Excel 格式
          </div>

          <div class="columns">
            <el-tag>姓名</el-tag>
            <el-tag>班级</el-tag>
            <el-tag>学号</el-tag>
          </div>

          <p>
            学号存在则更新学生姓名和班级，
            不存在则新增学生。
          </p>

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

          <el-button type="primary">
            选择学生名单 Excel
          </el-button>

          <template #tip>
            <div class="upload-tip">
              支持 .xlsx / .xls 文件
            </div>
          </template>

        </el-upload>

        <div
          v-if="studentFile"
          class="selected-file"
        >

          <el-icon>
            <Document />
          </el-icon>

          <span>
            {{ studentFile.name }}
          </span>

          <el-button
            type="success"
            :loading="studentImporting"
            @click="importStudents"
          >
            导入学生名单
          </el-button>

        </div>

      </div>

    </el-card>

    <!-- ========================= -->
    <!-- 部门成员 -->
    <!-- ========================= -->
    <el-card class="import-card" shadow="never">

      <div class="card-header">

        <div>
          <h3>部门成员名单导入</h3>

          <p>
            批量设置学生所属部门以及
            干事、副部长、部长身份。
          </p>
        </div>

        <el-tag type="success">
          部门干部数据
        </el-tag>

      </div>

      <div class="import-content">

        <div class="excel-info">

          <div class="info-title">
            Excel 格式
          </div>

          <div class="columns">

            <el-tag>部门</el-tag>
            <el-tag>姓名</el-tag>
            <el-tag>学号</el-tag>
            <el-tag type="warning">
              职位
            </el-tag>

          </div>

          <p>
            职位只能填写：
            <b>干事</b>、
            <b>副部长</b>、
            <b>部长</b>。
          </p>

          <p>
            部门可以只在第一行填写，
            后续空白部门会自动继承上一行部门。
          </p>

          <p>
            普通学生不需要出现在这个 Excel 中。
          </p>

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

          <el-button type="success">
            选择部门成员 Excel
          </el-button>

          <template #tip>
            <div class="upload-tip">
              支持 .xlsx / .xls 文件
            </div>
          </template>

        </el-upload>

        <div
          v-if="departmentFile"
          class="selected-file"
        >

          <el-icon>
            <Document />
          </el-icon>

          <span>
            {{ departmentFile.name }}
          </span>

          <el-button
            type="success"
            :loading="departmentImporting"
            @click="importDepartmentMembers"
          >
            导入部门成员
          </el-button>

        </div>

      </div>

    </el-card>

    <!-- ========================= -->
    <!-- 导入结果 -->
    <!-- ========================= -->
    <el-card
      v-if="lastImportResult"
      class="result-card"
      shadow="never"
    >

      <template #header>
        <div class="result-header">
          <span>最近一次导入结果</span>

          <el-tag
            :type="
              lastImportResult.failCount > 0
                ? 'warning'
                : 'success'
            "
          >
            {{
              lastImportResult.failCount > 0
                ? '部分失败'
                : '全部成功'
            }}
          </el-tag>
        </div>
      </template>

      <div class="result-summary">

        <div class="result-item">
          <span>总数量</span>
          <b>
            {{ lastImportResult.totalCount }}
          </b>
        </div>

        <div class="result-item success">
          <span>成功</span>
          <b>
            {{ lastImportResult.successCount }}
          </b>
        </div>

        <div class="result-item error">
          <span>失败</span>
          <b>
            {{ lastImportResult.failCount }}
          </b>
        </div>

      </div>

      <!-- 失败明细 -->
      <div
        v-if="
          lastImportResult.errors &&
          lastImportResult.errors.length > 0
        "
        class="error-detail"
      >

        <div class="error-title">
          导入失败明细
        </div>

        <div
          v-for="(item, index) in lastImportResult.errors"
          :key="index"
          class="error-row"
        >

          <el-tag type="danger">
            第 {{ item.row }} 行
          </el-tag>

          <span>
            {{ item.message }}
          </span>

        </div>

      </div>

    </el-card>

    <!-- ========================= -->
    <!-- 导入规则 -->
    <!-- ========================= -->
    <el-card
      class="rule-card"
      shadow="never"
    >

      <template #header>
        <span>导入规则说明</span>
      </template>

      <el-alert
        title="学生和部门干部是两套独立数据"
        type="info"
        :closable="false"
        show-icon
      />

      <div class="rule-list">

        <p>
          ① 所有学生首先通过“学生名单 Excel”进入 sys_user。
        </p>

        <p>
          ② 干事、副部长、部长也是学生，不会改变学生角色。
        </p>

        <p>
          ③ 部门成员通过“部门成员 Excel”
          进入 sys_user_department。
        </p>

        <p>
          ④ Excel 中不存在的部门会自动创建。
        </p>

        <p>
          ⑤ 空白部门自动继承上一行部门。
        </p>

        <p>
          ⑥ 一个学生可以属于多个部门。
        </p>

        <p>
          ⑦ 部长、副部长可以进行部门申报审核。
        </p>

        <p>
          ⑧ 干事、副部长、部长都可以进行个人证书申报。
        </p>

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

const lastImportResult = ref(null)

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

    ElMessage.warning(
      '请先选择学生名单 Excel'
    )

    return
  }

  studentImporting.value = true

  lastImportResult.value = null

  try {

    const formData = new FormData()

    formData.append(
      'file',
      studentFile.value
    )

    /*
     * 注意：
     *
     * request.js 已经：
     *
     * response => response.data
     *
     * 所以这里的 res 本身就是 Result。
     */
    const res =
      await request.post(
        '/excel/import/students',
        formData,
        {
          headers: {
            'Content-Type':
              'multipart/form-data'
          }
        }
      )

    console.log(
      '学生 Excel 导入结果：',
      res
    )

    /*
     * 正确：
     *
     * res.code
     *
     * 错误：
     *
     * res.data.code
     */
    if (res?.code === 200) {

      const result =
        res.data || {}

      lastImportResult.value =
        result

      if (
        result.failCount &&
        result.failCount > 0
      ) {

        ElMessage.warning(
          `学生名单导入完成：成功 ${result.successCount} 条，失败 ${result.failCount} 条`
        )

      } else {

        ElMessage.success(
          `学生名单导入成功：共 ${result.successCount} 条`
        )
      }

      studentFile.value = null

      studentUploadRef
        .value
        ?.clearFiles()

    } else {

      ElMessage.error(
        res?.message ||
        '学生名单导入失败'
      )
    }

  } catch (error) {

    console.error(
      '学生名单导入异常：',
      error
    )

    /*
     * axios 网络异常
     */
    const message =
      error?.response?.data?.message ||
      error?.message ||
      '学生名单导入失败'

    ElMessage.error(
      message
    )

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

  departmentFile.value =
    file.raw
}

function removeDepartmentFile() {

  departmentFile.value = null
}

async function importDepartmentMembers() {

  if (!departmentFile.value) {

    ElMessage.warning(
      '请先选择部门成员 Excel'
    )

    return
  }

  departmentImporting.value = true

  lastImportResult.value = null

  try {

    const formData = new FormData()

    formData.append(
      'file',
      departmentFile.value
    )

    const res =
      await request.post(
        '/excel/import/department-members',
        formData,
        {
          headers: {
            'Content-Type':
              'multipart/form-data'
          }
        }
      )

    console.log(
      '部门成员 Excel 导入结果：',
      res
    )

    /*
     * 这里必须使用 res.code，
     * 不能使用 res.data.code。
     */
    if (res?.code === 200) {

      const result =
        res.data || {}

      lastImportResult.value =
        result

      const successCount =
        result.successCount || 0

      const failCount =
        result.failCount || 0

      /*
       * 全部成功
       */
      if (failCount === 0) {

        ElMessage.success(
          `部门成员导入成功：共 ${successCount} 条`
        )

      } else {

        /*
         * 部分成功
         */
        ElMessage.warning(
          `部门成员导入完成：成功 ${successCount} 条，失败 ${failCount} 条`
        )
      }

      departmentFile.value =
        null

      departmentUploadRef
        .value
        ?.clearFiles()

    } else {

      /*
       * 后端业务失败
       *
       * 例如：
       *
       * code = 400
       * message =
       * 系统中不存在有效的“部长”岗位
       */
      ElMessage.error(
        res?.message ||
        '部门成员导入失败'
      )
    }

  } catch (error) {

    console.error(
      '部门成员导入异常：',
      error
    )

    const message =
      error?.response?.data?.message ||
      error?.message ||
      '部门成员导入失败'

    ElMessage.error(
      message
    )

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
  flex-wrap: wrap;
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

/* ========================= */
/* 导入结果 */
/* ========================= */

.result-card {
  margin-bottom: 20px;
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.result-summary {
  display: flex;
  gap: 50px;
  padding: 10px 0 20px;
}

.result-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.result-item span {
  color: #909399;
  font-size: 14px;
}

.result-item b {
  font-size: 24px;
  color: #303133;
}

.result-item.success b {
  color: #67c23a;
}

.result-item.error b {
  color: #f56c6c;
}

.error-detail {
  margin-top: 10px;
  padding-top: 18px;
  border-top: 1px solid #ebeef5;
}

.error-title {
  margin-bottom: 12px;
  font-weight: bold;
  color: #f56c6c;
}

.error-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #fff5f5;
  border-radius: 6px;
  color: #606266;
}

.rule-card {
  margin-bottom: 20px;
}

.rule-list {
  margin-top: 18px;
}

.rule-list p {
  margin: 9px 0;
  color: #606266;
  line-height: 1.6;
}

</style>
