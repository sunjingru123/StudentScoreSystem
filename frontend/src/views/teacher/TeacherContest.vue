<template>
  <div class="contest-page">

    <!-- =========================
         页面标题
    ========================== -->
    <el-card class="title-card">
      <div class="title-box">
        <div>
          <h2>比赛筛选</h2>
          <p>按获奖级别、获奖类别、贫困生分类筛选学生参加的比赛</p>
        </div>

        <div class="title-actions">
          <el-button
            v-if="canImportPoverty"
            type="primary"
            plain
            :icon="Upload"
            @click="openImportDialog"
          >
            导入贫困生名单
          </el-button>

          <el-button
            :icon="Refresh"
            :loading="loading"
            @click="loadList"
          >
            刷新
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- =========================
         筛选条件
    ========================== -->
    <el-card class="filter-card">
      <div class="filter-box">

        <el-input
          v-model="filters.keyword"
          placeholder="学号 / 姓名 / 班级"
          clearable
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon>
              <Search />
            </el-icon>
          </template>
        </el-input>

        <el-select
          v-model="filters.awardLevel"
          placeholder="获奖级别"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option
            v-for="item in awardLevelOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-select
          v-model="filters.awardCategory"
          placeholder="获奖类别"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option
            v-for="item in awardCategoryOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>

        <el-select
          v-model="filters.povertyStatus"
          placeholder="学生分类"
          clearable
          style="width: 150px"
          @change="handleSearch"
        >
          <el-option
            label="贫困生"
            value="1"
          />

          <el-option
            label="非贫困生"
            value="0"
          />
        </el-select>

        <el-button
          type="primary"
          :icon="Search"
          :loading="loading"
          @click="handleSearch"
        >
          搜索
        </el-button>

        <el-button
          :icon="RefreshLeft"
          @click="handleReset"
        >
          重置
        </el-button>

      </div>
    </el-card>

    <!-- =========================
         比赛列表
    ========================== -->
    <el-card class="table-card">

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        style="width: 100%"
      >

        <el-table-column
          label="学生"
          min-width="140"
        >
          <template #default="scope">
            <div class="student-cell">
              <strong>
                {{ scope.row.studentName || '未填写姓名' }}
              </strong>
            </div>
          </template>
        </el-table-column>

        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="130"
          align="center"
        />

        <el-table-column
          prop="className"
          label="班级"
          min-width="140"
          align="center"
        />

        <el-table-column
          label="学生分类"
          width="110"
          align="center"
        >
          <template #default="scope">
            <el-tag
              v-if="Number(scope.row.povertyStatus) === 1"
              type="danger"
            >
              贫困生
            </el-tag>

            <el-tag
              v-else
              type="info"
            >
              非贫困生
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column
          prop="awardName"
          label="比赛 / 奖项名称"
          min-width="200"
        >
          <template #default="scope">
            {{ scope.row.awardName || scope.row.title || '-' }}
          </template>
        </el-table-column>

        <el-table-column
          label="获奖级别"
          width="110"
          align="center"
        >
          <template #default="scope">
            <el-tag
              v-if="scope.row.awardLevel"
              type="success"
            >
              {{ scope.row.awardLevel }}
            </el-tag>

            <span v-else>-</span>
          </template>
        </el-table-column>

        <el-table-column
          label="获奖类别"
          width="100"
          align="center"
        >
          <template #default="scope">
            {{ formatAwardCategory(scope.row.awardCategory) }}
          </template>
        </el-table-column>

        <el-table-column
          label="获奖等级"
          width="120"
          align="center"
        >
          <template #default="scope">
            {{ formatAwardGrade(scope.row) }}
          </template>
        </el-table-column>

        <el-table-column
          prop="awardTime"
          label="获奖时间"
          width="120"
          align="center"
        >
          <template #default="scope">
            {{ scope.row.awardTime || '-' }}
          </template>
        </el-table-column>

        <el-table-column
          label="状态"
          width="100"
          align="center"
        >
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)">
              {{ formatStatus(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>

      </el-table>

      <!-- =========================
           分页
      ========================== -->
      <div class="pagination-box">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadList"
          @size-change="handleSizeChange"
        />
      </div>

    </el-card>

    <!-- =========================
         贫困生名单导入
    ========================== -->
    <el-dialog
      v-model="importVisible"
      title="导入贫困生名单"
      width="620px"
      destroy-on-close
    >
      <div class="import-tip">
        <p>
          模板固定两列：
          <b>学号</b>、
          <b>姓名</b>。
          系统按学号匹配学生，只把名单中的学生标记为贫困生，
          未出现在名单中的学生保持原状态。
        </p>

        <el-button
          type="primary"
          link
          :loading="templateDownloading"
          @click="downloadTemplate"
        >
          下载导入模板
        </el-button>
      </div>

      <el-upload
        ref="importUploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="handleImportFileChange"
        :on-remove="removeImportFile"
        drag
        style="width: 100%"
      >
        <el-icon class="el-icon--upload">
          <UploadFilled />
        </el-icon>

        <div class="el-upload__text">
          将 Excel 拖到此处，或
          <em>点击选择文件</em>
        </div>
      </el-upload>

      <!-- 导入结果 -->
      <div
        v-if="importResult"
        class="import-result"
      >
        <p>
          共处理
          <b>{{ importResult.totalCount || 0 }}</b>
          条，成功
          <b class="success-text">{{ importResult.successCount || 0 }}</b>
          条，失败
          <b class="fail-text">{{ importResult.failCount || 0 }}</b>
          条
        </p>

        <el-table
          v-if="importResult.errors && importResult.errors.length > 0"
          :data="importResult.errors"
          border
          size="small"
          max-height="220"
          style="width: 100%"
        >
          <el-table-column
            prop="row"
            label="Excel 行号"
            width="110"
            align="center"
          />

          <el-table-column
            prop="studentNo"
            label="学号"
            width="150"
            align="center"
          />

          <el-table-column
            prop="reason"
            label="失败原因"
          />
        </el-table>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">
          关闭
        </el-button>

        <el-button
          type="primary"
          :disabled="!importFile"
          :loading="importing"
          @click="submitImport"
        >
          开始导入
        </el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>

import {
  onMounted,
  reactive,
  ref,
} from 'vue'

import { ElMessage } from 'element-plus'

import {
  Refresh,
  RefreshLeft,
  Search,
  Upload,
  UploadFilled,
} from '@element-plus/icons-vue'

import { getContestList } from '@/api/contest'

import {
  downloadPovertyTemplate,
  getPovertyPermission,
  importPovertyStudents,
} from '@/api/poverty'

/* ====================================================== */
/* 筛选项                                                */
/* ====================================================== */

const awardLevelOptions = [
  { label: '国奖（国家级）', value: '国家级' },
  { label: '省奖（省级）', value: '省级' },
  { label: '校级', value: '校级' },
  { label: '院级', value: '院级' },
]

const awardCategoryOptions = [
  { label: 'A类', value: 'A' },
  { label: 'B类', value: 'B' },
  { label: 'C类', value: 'C' },
  { label: '其他类', value: 'OTHER' },
]

const filters = reactive({
  keyword: '',
  awardLevel: '',
  awardCategory: '',
  povertyStatus: '',
})

/* ====================================================== */
/* 列表                                                  */
/* ====================================================== */

const loading = ref(false)

const list = ref([])

const total = ref(0)

const pageNum = ref(1)

const pageSize = ref(10)

/* ====================================================== */
/* 贫困生导入                                            */
/* ====================================================== */

const canImportPoverty = ref(false)

const importVisible = ref(false)

const importFile = ref(null)

const importing = ref(false)

const templateDownloading = ref(false)

const importResult = ref(null)

const importUploadRef = ref(null)

/* ====================================================== */
/* 列表加载                                              */
/* ====================================================== */

async function loadList() {
  loading.value = true

  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    }

    if (filters.keyword) {
      params.keyword = filters.keyword.trim()
    }

    if (filters.awardLevel) {
      params.awardLevel = filters.awardLevel
    }

    if (filters.awardCategory) {
      params.awardCategory = filters.awardCategory
    }

    if (filters.povertyStatus !== '' && filters.povertyStatus !== null) {
      params.povertyStatus = filters.povertyStatus
    }

    const res = await getContestList(params)

    if (res?.code !== 200) {
      ElMessage.error(res?.message || '加载比赛列表失败')

      list.value = []

      total.value = 0

      return
    }

    const data = res?.data || {}

    list.value = Array.isArray(data.records) ? data.records : []

    total.value = Number(data.total) || 0
  } catch (error) {
    ElMessage.error(error?.message || '加载比赛列表失败')

    list.value = []

    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1

  loadList()
}

function handleReset() {
  filters.keyword = ''

  filters.awardLevel = ''

  filters.awardCategory = ''

  filters.povertyStatus = ''

  handleSearch()
}

function handleSizeChange() {
  pageNum.value = 1

  loadList()
}

/* ====================================================== */
/* 展示格式化                                            */
/* ====================================================== */

function formatAwardCategory(value) {
  if (!value) {
    return '-'
  }

  const map = {
    A: 'A类',
    B: 'B类',
    C: 'C类',
    OTHER: '其他类',
  }

  return map[value] || value
}

function formatAwardGrade(row) {
  if (!row) {
    return '-'
  }

  if (row.awardGrade === 'OTHER') {
    return row.awardGradeOther || '其他'
  }

  return row.awardGrade || '-'
}

function formatStatus(status) {
  const map = {
    0: '待审核',
    1: '已通过',
    2: '已驳回',
  }

  return map[Number(status)] || '未知'
}

function statusTagType(status) {
  const value = Number(status)

  if (value === 1) {
    return 'success'
  }

  if (value === 2) {
    return 'danger'
  }

  return 'warning'
}

/* ====================================================== */
/* 权限                                                  */
/* ====================================================== */

async function loadPermission() {
  try {
    const res = await getPovertyPermission()

    const data = res?.data || {}

    canImportPoverty.value =
      data.canImport === true || Number(data.canImport) === 1
  } catch (error) {
    canImportPoverty.value = false
  }
}

/* ====================================================== */
/* 导入贫困生名单                                        */
/* ====================================================== */

function openImportDialog() {
  importFile.value = null

  importResult.value = null

  importUploadRef.value?.clearFiles()

  importVisible.value = true
}

function handleImportFileChange(file) {
  importFile.value = file.raw
}

function removeImportFile() {
  importFile.value = null
}

async function downloadTemplate() {
  templateDownloading.value = true

  try {
    const response = await downloadPovertyTemplate()

    /*
     * 下载失败时后端返回的是 JSON，
     * 这里要提示真实原因，而不是当成 xlsx 保存。
     */
    if (
      response instanceof Blob &&
      (response.type || '').includes('json')
    ) {
      let message = '下载导入模板失败'

      try {
        const data = JSON.parse(await response.text())

        message = data?.message || message
      } catch (e) {
        // 保持默认提示
      }

      ElMessage.error(message)

      return
    }

    const blob = new Blob(
      [response?.data || response],
      {
        type:
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      },
    )

    const url = window.URL.createObjectURL(blob)

    const link = document.createElement('a')

    link.href = url

    link.download = '贫困生名单导入模板.xlsx'

    document.body.appendChild(link)

    link.click()

    document.body.removeChild(link)

    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error?.message || '下载导入模板失败')
  } finally {
    templateDownloading.value = false
  }
}

async function submitImport() {
  if (!importFile.value) {
    ElMessage.warning('请先选择贫困生名单 Excel')

    return
  }

  importing.value = true

  importResult.value = null

  try {
    const formData = new FormData()

    formData.append('file', importFile.value)

    const res = await importPovertyStudents(formData)

    if (res?.code !== 200) {
      ElMessage.error(res?.message || '导入失败')

      return
    }

    const result = res?.data || {}

    importResult.value = result

    ElMessage.success(result.message || '导入完成')

    loadList()
  } catch (error) {
    ElMessage.error(error?.message || '导入失败')
  } finally {
    importing.value = false
  }
}

/* ====================================================== */
/* 初始化                                                */
/* ====================================================== */

onMounted(() => {
  loadPermission()

  loadList()
})

</script>

<style scoped>

.contest-page {
  display: flex;

  flex-direction: column;

  gap: 16px;
}

.title-card .title-box {
  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 12px;

  flex-wrap: wrap;
}

.title-box h2 {
  margin: 0;

  font-size: 20px;

  color: #303133;
}

.title-box p {
  margin: 6px 0 0;

  font-size: 13px;

  color: #909399;
}

.title-actions {
  display: flex;

  gap: 10px;
}

.filter-box {
  display: flex;

  align-items: center;

  gap: 12px;

  flex-wrap: wrap;
}

.table-card .student-cell strong {
  color: #303133;
}

.pagination-box {
  display: flex;

  justify-content: flex-end;

  margin-top: 16px;
}

.import-tip {
  margin-bottom: 14px;

  font-size: 13px;

  color: #606266;

  line-height: 1.7;
}

.import-tip p {
  margin: 0 0 6px;
}

.import-result {
  margin-top: 16px;

  font-size: 13px;

  color: #606266;
}

.import-result .success-text {
  color: #67c23a;
}

.import-result .fail-text {
  color: #f56c6c;
}

</style>