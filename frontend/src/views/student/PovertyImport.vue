<template>

  <div class="poverty-import-page">

    <el-card
      shadow="never"
      class="page-card"
    >

      <template #header>

        <div class="card-header">

          <div class="card-title">

            <el-icon><Upload /></el-icon>

            <span>贫困生名单导入</span>

          </div>

          <div class="card-subtitle">
            下载模板后填写学号、姓名，再上传即可把名单中的学生标记为贫困生
          </div>

        </div>

      </template>


      <!-- =================================================
           操作区
      ================================================== -->

      <div
        v-if="permissionLoaded && !canImport"
        class="no-permission"
      >

        <el-alert
          type="warning"
          :closable="false"
          show-icon
          title="没有导入权限"
          description="只有勤工助学部的部长 / 副部长可以导入贫困生名单。"
        />

      </div>


      <template v-else>

        <div class="toolbar">

          <el-button
            type="primary"
            plain
            :loading="templateDownloading"
            @click="downloadTemplate"
          >

            <el-icon><Download /></el-icon>

            下载导入模板

          </el-button>

        </div>


        <!-- ===============================================
             导入模式
        ================================================ -->

        <el-radio-group
          v-model="importMode"
          class="mode-group"
          :disabled="importing"
        >

          <el-radio value="append">
            仅新增 / 修正（不影响名单外学生）
          </el-radio>

          <el-radio value="replace">
            按名单覆盖（清除名单外的贫困生标记）
          </el-radio>

        </el-radio-group>


        <div
          v-if="importMode === 'replace'"
          class="mode-tip"
        >
          覆盖模式下，本次名单即最终结果：名单外的学生会被取消贫困生标记。
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
            选择名单 Excel
          </el-button>

          <template #tip>

            <div class="upload-tip">
              支持 .xlsx / .xls 文件，模板固定两列：学号 | 姓名，学号与姓名必须与系统一致
            </div>

          </template>

        </el-upload>


        <div
          v-if="selectedFile"
          class="selected-file"
        >

          <span class="selected-file-name">
            {{ selectedFile.name }}
          </span>

          <el-button
            type="success"
            :loading="importing"
            @click="submitImport"
          >
            开始导入
          </el-button>

        </div>


        <!-- ===============================================
             导入结果
        ================================================ -->

        <div
          v-if="importResult"
          class="import-result"
        >

          <el-alert
            :type="(importResult.failCount || 0) > 0 ? 'warning' : 'success'"
            :closable="false"
            show-icon
          >

            <template #title>

              共处理 {{ importResult.totalCount || 0 }} 条，
              成功 {{ importResult.successCount || 0 }} 条，
              失败 {{ importResult.failCount || 0 }} 条
              <template v-if="importResult.mode === 'replace'">
                ，取消标记 {{ importResult.clearedCount || 0 }} 人
              </template>

            </template>

          </el-alert>


          <el-table
            v-if="importErrors.length > 0"
            class="error-table"
            :data="importErrors"
            size="small"
            border
          >

            <el-table-column
              prop="row"
              label="行号"
              width="90"
            />

            <el-table-column
              prop="studentNo"
              label="学号"
              min-width="140"
            />

            <el-table-column
              prop="reason"
              label="失败原因"
              min-width="200"
            />

          </el-table>

        </div>

      </template>

    </el-card>

  </div>

</template>


<script setup>

import {
  computed,
  onMounted,
  ref
} from 'vue'


import {
  ElMessage,
  ElMessageBox
} from 'element-plus'


import {
  Download,
  Upload
} from '@element-plus/icons-vue'


import {
  downloadPovertyTemplate,
  getPovertyPermission,
  importPovertyStudents
} from '@/api/poverty'


// =========================================================
// 状态
// =========================================================

const permissionLoaded =
  ref(false)

const canImport =
  ref(false)

const uploadRef =
  ref(null)

const selectedFile =
  ref(null)

const importing =
  ref(false)

const templateDownloading =
  ref(false)

const importResult =
  ref(null)


// 导入模式：append（仅新增/修正） | replace（按名单覆盖）
const importMode =
  ref('append')


const importErrors =
  computed(() =>
    Array.isArray(importResult.value?.errors)
      ? importResult.value.errors
      : []
  )


// =========================================================
// 权限（以后端为准，前端只控制按钮显示）
// =========================================================

async function loadPermission() {

  try {

    const res =
      await getPovertyPermission()

    canImport.value =
      res?.data?.canImport === true ||
      Number(res?.data?.canImport) === 1

  } catch (error) {

    console.error(
      '获取贫困生导入权限失败：',
      error
    )

    canImport.value = false

  } finally {

    permissionLoaded.value = true

  }

}


// =========================================================
// 下载模板
// =========================================================

async function downloadTemplate() {

  templateDownloading.value = true

  try {

    const response =
      await downloadPovertyTemplate()

    /*
     * 下载失败时后端返回的是 JSON，
     * 这里要提示真实原因，而不是当成 xlsx 保存。
     */
    if (
      response instanceof Blob
      && (response.type || '').includes('json')
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
      }
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

    ElMessage.error(
      error?.message || '下载导入模板失败'
    )

  } finally {

    templateDownloading.value = false

  }

}


// =========================================================
// 选择文件
// =========================================================

function handleFileChange(file) {

  selectedFile.value = file.raw

  importResult.value = null

}


function removeFile() {

  selectedFile.value = null

}


// =========================================================
// 提交导入
// =========================================================

async function submitImport() {

  if (!selectedFile.value) {

    ElMessage.warning('请先选择要导入的 Excel 文件')

    return

  }


  /*
   * 覆盖模式会清除名单外学生的贫困生标记，
   * 属于批量数据变更，提交前必须二次确认。
   */
  if (importMode.value === 'replace') {

    try {

      await ElMessageBox.confirm(
        '覆盖模式会把本次名单作为最终结果，名单外的学生将被取消贫困生标记。确定继续吗？',
        '确认覆盖导入',
        {
          type: 'warning',
          confirmButtonText: '确定覆盖',
          cancelButtonText: '取消',
        }
      )

    } catch (e) {

      return

    }

  }


  importing.value = true

  try {

    const formData = new FormData()

    formData.append('file', selectedFile.value)

    formData.append('mode', importMode.value)

    const res =
      await importPovertyStudents(formData)


    if (
      !res ||
      (Number(res.code) !== 200 &&
        Number(res.code) !== 0)
    ) {

      ElMessage.error(
        res?.message || '导入失败'
      )

      return

    }


    importResult.value = res.data || {}

    const failCount =
      Number(importResult.value.failCount) || 0

    if (failCount > 0) {

      ElMessage.warning(
        `导入完成，${failCount} 条失败，请查看失败原因`
      )

    } else {

      ElMessage.success('导入成功')

    }


    selectedFile.value = null

    uploadRef.value?.clearFiles()


    /*
     * 覆盖导入完成后自动切回默认模式，
     * 避免重复点击再次清空名单外的标记。
     */
    if (importResult.value.mode === 'replace') {

      importMode.value = 'append'

    }

  } catch (error) {

    ElMessage.error(
      error?.message || '导入失败'
    )

  } finally {

    importing.value = false

  }

}


onMounted(loadPermission)

</script>


<style scoped>

.poverty-import-page {

  padding: 4px;

}

.page-card {

  border-radius: 8px;

}

.card-header {

  display: flex;

  flex-direction: column;

  gap: 6px;

}

.card-title {

  display: flex;

  align-items: center;

  gap: 8px;

  font-size: 16px;

  font-weight: 600;

}

.card-subtitle {

  font-size: 13px;

  color: #909399;

}

.toolbar {

  margin-bottom: 16px;

}

.mode-group {

  display: flex;

  flex-wrap: wrap;

  gap: 8px 20px;

  margin-bottom: 8px;

}

.mode-tip {

  margin-bottom: 12px;

  font-size: 12px;

  color: #e6a23c;

}

.upload-area {

  margin-bottom: 12px;

}

.upload-tip {

  margin-top: 6px;

  font-size: 12px;

  color: #909399;

}

.selected-file {

  display: flex;

  align-items: center;

  gap: 12px;

  margin: 12px 0 20px;

}

.selected-file-name {

  font-size: 13px;

  color: #606266;

}

.import-result {

  margin-top: 12px;

}

.error-table {

  margin-top: 12px;

}

.no-permission {

  padding: 8px 0;

}

</style>
