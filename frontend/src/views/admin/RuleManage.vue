<template>
  <div class="rule-page">
    <div class="page-header">
      <div>
        <h2>规则管理</h2>
        <p>仅管理部门加减分的固定评分项目，部门临时/非固定模板不在这里维护</p>
      </div>

      <div class="header-actions">
        <el-button type="primary" @click="openAddDialog">
          <el-icon>
            <Plus />
          </el-icon>
          新建规则
        </el-button>

        <el-button type="success" plain @click="openImportDialog">
          <el-icon>
            <Upload />
          </el-icon>
          导入规则
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="rules" v-loading="loading" border stripe>
        <el-table-column type="index" label="#" width="60" align="center" />
        <el-table-column label="所属部门" min-width="140">
          <template #default="{ row }">
            {{ getDepartmentName(row.departmentId) }}
          </template>
        </el-table-column>
        <el-table-column prop="name" label="规则名称" min-width="180" />
        <el-table-column label="分类" width="120" align="center">
          <template #default="{ row }">
            {{ row.category || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="分值" width="100" align="center" />
        <el-table-column label="描述" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">
              {{ Number(row.status) === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" min-width="170" />
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEditDialog(row)">
              编辑
            </el-button>

            <el-button type="danger" link @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && rules.length === 0" description="暂无规则数据" />
    </el-card>

    <!-- =====================================================
         新增 / 编辑规则
    ====================================================== -->

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="90px"
      >
        <el-form-item label="所属部门" prop="departmentId">
          <el-select
            v-model="form.departmentId"
            placeholder="请选择所属部门"
            filterable
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

        <el-form-item label="规则名称" prop="name">
          <el-input
            v-model="form.name"
            placeholder="例如：优秀学生干部"
            maxlength="100"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="分类" prop="category">
          <el-input
            v-model="form.category"
            placeholder="例如：德育、学业、文体"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="分值" prop="score">
          <el-input-number
            v-model="form.score"
            :min="0.01"
            :max="9999"
            :step="0.5"
            :precision="2"
            controls-position="right"
            style="width: 200px"
          />

          <span class="form-tip">
            部门申报终审会按该分值生成正式成绩，必须大于 0
          </span>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="选填，说明该规则的适用范围"
          />
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">
              启用
            </el-radio>

            <el-radio :value="0">
              停用
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">
          取消
        </el-button>

        <el-button type="primary" :loading="saving" @click="submitForm">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- =====================================================
         导入规则
    ====================================================== -->

    <el-dialog
      v-model="importVisible"
      title="导入规则"
      width="620px"
      destroy-on-close
    >
      <div class="excel-info">
        <div class="info-title">
          Excel 格式
        </div>

        <div class="columns">
          <el-tag>部门</el-tag>
          <el-tag>规则名称</el-tag>
          <el-tag>分类</el-tag>
          <el-tag type="warning">
            分值
          </el-tag>
          <el-tag>描述</el-tag>
          <el-tag type="info">
            状态
          </el-tag>
        </div>

        <p>
          ① 部门必须是系统中已经存在的部门名称。
        </p>

        <p>
          ② 分值必须大于 0，最多保留两位小数。
        </p>

        <p>
          ③ 状态填写「启用」或「停用」，留空默认为启用。
        </p>

        <p>
          ④ 同一部门下「规则名称」已存在则更新，不存在则新增。
        </p>
      </div>

      <div class="upload-row">
        <el-button :loading="templateDownloading" @click="downloadTemplate">
          <el-icon>
            <Download />
          </el-icon>
          下载导入模板
        </el-button>
      </div>

      <el-upload
        ref="uploadRef"
        class="upload-area"
        action=""
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleFileChange"
        :on-remove="removeFile"
      >
        <el-button type="primary">
          选择规则 Excel
        </el-button>

        <template #tip>
          <div class="upload-tip">
            支持 .xlsx / .xls 文件
          </div>
        </template>
      </el-upload>

      <div v-if="importFile" class="selected-file">
        <el-icon>
          <Document />
        </el-icon>

        <span>
          {{ importFile.name }}
        </span>

        <el-button
          type="success"
          :loading="importing"
          @click="submitImport"
        >
          开始导入
        </el-button>
      </div>

      <!-- 导入结果 -->

      <div v-if="importResult" class="import-result">
        <div class="result-summary">
          <div class="result-item">
            <span>总数量</span>
            <b>{{ importResult.totalCount }}</b>
          </div>

          <div class="result-item success">
            <span>成功</span>
            <b>{{ importResult.successCount }}</b>
          </div>

          <div class="result-item error">
            <span>失败</span>
            <b>{{ importResult.failCount }}</b>
          </div>
        </div>

        <div
          v-if="importResult.errors && importResult.errors.length > 0"
          class="error-detail"
        >
          <div class="error-title">
            导入失败明细
          </div>

          <div
            v-for="(item, index) in importResult.errors"
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
      </div>

      <template #footer>
        <el-button @click="importVisible = false">
          关闭
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document,
  Download,
  Plus,
  Upload,
} from '@element-plus/icons-vue'
import request from '@/utils/request'

/*
 * =========================================================
 * 数据
 * =========================================================
 */

const rules = ref([])
const departments = ref([])
const loading = ref(false)
const saving = ref(false)

/*
 * =========================================================
 * 新增 / 编辑弹窗
 * =========================================================
 */

const dialogVisible = ref(false)
const formRef = ref(null)
const editId = ref(null)

const form = reactive({
  departmentId: null,
  name: '',
  category: '',
  score: null,
  description: '',
  status: 1,
})

const formRules = {
  departmentId: [
    {
      required: true,
      message: '请选择所属部门',
      trigger: 'change',
    },
  ],
  name: [
    {
      required: true,
      message: '请输入规则名称',
      trigger: 'blur',
    },
  ],
  score: [
    {
      required: true,
      message: '请输入分值',
      trigger: 'blur',
    },
  ],
}

const dialogTitle = computed(() => {
  return editId.value ? '编辑规则' : '新建规则'
})

/*
 * =========================================================
 * 导入
 * =========================================================
 */

const importVisible = ref(false)
const uploadRef = ref(null)
const importFile = ref(null)
const importing = ref(false)
const importResult = ref(null)
const templateDownloading = ref(false)

/*
 * =========================================================
 * 加载部门
 * =========================================================
 */

async function loadDepartments() {
  try {
    const res = await request.get('/department/list')
    const list = res?.data

    departments.value = Array.isArray(list) ? list : []
  } catch (error) {
    console.error('加载部门失败：', error)
    departments.value = []
  }
}

function getDepartmentName(departmentId) {
  const department = departments.value.find(
    (item) => Number(item.id) === Number(departmentId)
  )

  return department ? department.name : '-'
}

/*
 * =========================================================
 * 加载规则
 * =========================================================
 */

async function loadRules() {
  loading.value = true

  try {
    const res = await request.get('/scoreRule/list')

    if (res?.code !== 200) {
      rules.value = []
      ElMessage.error(res?.message || '规则管理加载失败')
      return
    }

    const list = res?.data

    rules.value = Array.isArray(list) ? list : []
  } catch (error) {
    console.error('加载规则失败：', error)
    rules.value = []
    ElMessage.error('规则管理加载失败')
  } finally {
    loading.value = false
  }
}

/*
 * =========================================================
 * 表单
 * =========================================================
 */

function resetForm() {
  editId.value = null

  form.departmentId = null
  form.name = ''
  form.category = ''
  form.score = null
  form.description = ''
  form.status = 1

  if (formRef.value) {
    formRef.value.clearValidate()
  }
}

function openAddDialog() {
  resetForm()

  dialogVisible.value = true
}

function openEditDialog(row) {
  resetForm()

  editId.value = row.id
  form.departmentId = row.departmentId ?? null
  form.name = row.name || ''
  form.category = row.category || ''
  form.score = row.score === null || row.score === undefined
    ? null
    : Number(row.score)
  form.description = row.description || ''
  form.status = Number(row.status) === 0 ? 0 : 1

  dialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) {
    return
  }

  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (form.score === null || Number(form.score) <= 0) {
    ElMessage.error('分值必须大于 0')
    return
  }

  saving.value = true

  try {
    const data = {
      departmentId: form.departmentId,
      name: form.name.trim(),
      category: form.category.trim(),
      score: form.score,
      description: form.description.trim(),
      status: form.status,
    }

    const res = editId.value
      ? await request.put(`/scoreRule/update/${editId.value}`, data)
      : await request.post('/scoreRule/add', data)

    if (res?.code !== 200) {
      ElMessage.error(res?.message || '规则保存失败')
      return
    }

    ElMessage.success(editId.value ? '规则修改成功' : '规则创建成功')

    dialogVisible.value = false

    await loadRules()
  } catch (error) {
    console.error('保存规则失败：', error)

    const message =
      error?.response?.data?.message
      || error?.message
      || '规则保存失败'

    ElMessage.error(message)
  } finally {
    saving.value = false
  }
}

/*
 * =========================================================
 * 删除
 * =========================================================
 */

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除规则「${row.name}」吗？删除后不可恢复。`,
      '删除规则',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
  } catch {
    return
  }

  try {
    const res = await request.delete(`/scoreRule/delete/${row.id}`)

    if (res?.code !== 200) {
      ElMessage.error(res?.message || '规则删除失败')
      return
    }

    ElMessage.success('规则删除成功')

    await loadRules()
  } catch (error) {
    console.error('删除规则失败：', error)

    const message =
      error?.response?.data?.message
      || error?.message
      || '规则删除失败'

    ElMessage.error(message)
  }
}

/*
 * =========================================================
 * 导入
 * =========================================================
 */

function openImportDialog() {
  importFile.value = null
  importResult.value = null

  uploadRef.value?.clearFiles()

  importVisible.value = true
}

function handleFileChange(file) {
  importFile.value = file.raw
}

function removeFile() {
  importFile.value = null
}

async function downloadTemplate() {
  templateDownloading.value = true

  try {
    const response = await request.get('/scoreRule/import/template', {
      responseType: 'blob',
    })

    const blob = new Blob(
      [response?.data || response],
      {
        type:
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      }
    )

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')

    link.href = url
    link.download = '评分项目导入模板.xlsx'

    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    window.URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载导入模板失败：', error)
    ElMessage.error('下载导入模板失败')
  } finally {
    templateDownloading.value = false
  }
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择规则 Excel')
    return
  }

  importing.value = true
  importResult.value = null

  try {
    const formData = new FormData()

    formData.append('file', importFile.value)

    const res = await request.post('/scoreRule/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })

    if (res?.code !== 200) {
      ElMessage.error(res?.message || '规则导入失败')
      return
    }

    const result = res.data || {}

    importResult.value = result

    const successCount = result.successCount || 0
    const failCount = result.failCount || 0

    if (failCount === 0) {
      ElMessage.success(`规则导入成功：共 ${successCount} 条`)
    } else {
      ElMessage.warning(
        `规则导入完成：成功 ${successCount} 条，失败 ${failCount} 条`
      )
    }

    importFile.value = null

    uploadRef.value?.clearFiles()

    await loadRules()
  } catch (error) {
    console.error('规则导入异常：', error)

    const message =
      error?.response?.data?.message
      || error?.message
      || '规则导入失败'

    ElMessage.error(message)
  } finally {
    importing.value = false
  }
}

/*
 * =========================================================
 * 初始化
 * =========================================================
 */

onMounted(async () => {
  await loadDepartments()
  await loadRules()
})
</script>

<style scoped>
.rule-page {
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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

.header-actions {
  display: flex;
  gap: 10px;
  flex: 0 0 auto;
}

.table-card {
  border-radius: 8px;
}

.form-tip {
  margin-left: 12px;
  color: #909399;
  font-size: 12px;
}

/* =========================================================
   导入
========================================================= */

.excel-info {
  padding: 16px;
  margin-bottom: 18px;
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

.upload-row {
  margin-bottom: 12px;
}

.upload-area {
  margin-bottom: 16px;
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
  word-break: break-all;
}

.import-result {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #ebeef5;
}

.result-summary {
  display: flex;
  gap: 50px;
  padding-bottom: 10px;
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

@media (max-width: 768px) {
  .rule-page {
    padding: 12px;
  }

  .page-header {
    flex-direction: column;
  }

  .header-actions {
    width: 100%;
  }

  .header-actions .el-button {
    flex: 1;
    margin-left: 0;
  }
}
</style>
