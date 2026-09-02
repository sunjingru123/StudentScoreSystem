<template>

  <div class="certificate-page">

    <el-card shadow="never">

      <template #header>

        <div class="header">

          <div>

            <h2>
              个人证书审核
            </h2>

            <p>
              档案部负责学生个人证书、奖状等加分材料的初审与终审
            </p>

          </div>


          <el-button
            type="primary"
            :loading="loading"
            @click="load"
          >
            刷新
          </el-button>

        </div>

      </template>


      <!-- ==================== 审核阶段 ==================== -->

      <el-tabs
        v-model="activeTab"
        @tab-change="handleTabChange"
      >

        <!-- 初审 -->

        <el-tab-pane
          name="preliminary"
          label="初审"
        >

          <div class="toolbar">

            <div class="toolbar-left">

              <el-button
                type="success"
                :disabled="selectedRows.length === 0"
                :loading="batchLoading"
                @click="batchPreliminaryApprove"
              >
                批量通过
              </el-button>


              <span class="selected-text">
                已选择 {{ selectedRows.length }} 条
              </span>

            </div>

          </div>


          <el-table
            ref="preliminaryTableRef"
            :data="list"
            v-loading="loading"
            border
            stripe
            @selection-change="handleSelectionChange"
          >

            <el-table-column
              type="selection"
              width="55"
              align="center"
            />


            <el-table-column
              type="index"
              label="#"
              width="60"
              align="center"
            />


            <el-table-column
              prop="studentName"
              label="学生"
              min-width="110"
            />


            <el-table-column
              prop="studentNo"
              label="学号"
              min-width="130"
            />


            <el-table-column
              prop="title"
              label="证书/获奖项目"
              min-width="180"
            />


            <el-table-column
              prop="description"
              label="申请说明"
              min-width="220"
              show-overflow-tooltip
            />


            <el-table-column
              prop="createTime"
              label="提交时间"
              min-width="170"
            />


            <el-table-column
              label="状态"
              width="100"
              align="center"
            >

              <template #default>

                <el-tag type="warning">
                  待初审
                </el-tag>

              </template>

            </el-table-column>


            <!-- 材料 -->

            <el-table-column
              label="材料"
              width="100"
              align="center"
            >

              <template #default="{ row }">

                <el-button
                  v-if="row.materialFile"
                  type="primary"
                  link
                  @click="openFile(row.materialFile)"
                >
                  查看材料
                </el-button>


                <span
                  v-else
                  class="no-file"
                >
                  无材料
                </span>

              </template>

            </el-table-column>


            <!-- 操作 -->

            <el-table-column
              label="操作"
              width="220"
              fixed="right"
              align="center"
            >

              <template #default="{ row }">

                <el-button
                  type="success"
                  link
                  @click="preliminaryApprove(row)"
                >
                  通过并填写分值
                </el-button>


                <el-button
                  type="danger"
                  link
                  @click="preliminaryReject(row)"
                >
                  驳回
                </el-button>

              </template>

            </el-table-column>

          </el-table>


          <el-empty
            v-if="
              !loading &&
              list.length === 0
            "
            description="暂无待初审的个人证书"
          />

        </el-tab-pane>


        <!-- ==================== 终审 ==================== -->

        <el-tab-pane
          v-if="canFinalAudit"
          name="final"
          label="终审"
        >

          <div class="toolbar">

            <div class="toolbar-left">

              <el-button
                type="success"
                :disabled="selectedRows.length === 0"
                :loading="batchLoading"
                @click="batchFinalApprove"
              >
                批量通过
              </el-button>


              <span class="selected-text">
                已选择 {{ selectedRows.length }} 条
              </span>

            </div>

          </div>


          <el-table
            ref="finalTableRef"
            :data="list"
            v-loading="loading"
            border
            stripe
            @selection-change="handleSelectionChange"
          >

            <el-table-column
              type="selection"
              width="55"
              align="center"
            />


            <el-table-column
              type="index"
              label="#"
              width="60"
              align="center"
            />


            <el-table-column
              prop="studentName"
              label="学生"
              min-width="110"
            />


            <el-table-column
              prop="studentNo"
              label="学号"
              min-width="130"
            />


            <el-table-column
              prop="title"
              label="证书/获奖项目"
              min-width="180"
            />


            <!-- 初审确定的分值 -->

            <el-table-column
              label="初审加分"
              width="100"
              align="center"
            >

              <template #default="{ row }">

                <span class="score-text">
                  {{ formatScore(row.applyScore) }}
                </span>

              </template>

            </el-table-column>


            <el-table-column
              prop="description"
              label="申请说明"
              min-width="220"
              show-overflow-tooltip
            />


            <el-table-column
              prop="createTime"
              label="提交时间"
              min-width="170"
            />


            <el-table-column
              label="状态"
              width="100"
              align="center"
            >

              <template #default>

                <el-tag type="warning">
                  待终审
                </el-tag>

              </template>

            </el-table-column>


            <!-- 材料 -->

            <el-table-column
              label="材料"
              width="100"
              align="center"
            >

              <template #default="{ row }">

                <el-button
                  v-if="row.materialFile"
                  type="primary"
                  link
                  @click="openFile(row.materialFile)"
                >
                  查看材料
                </el-button>


                <span
                  v-else
                  class="no-file"
                >
                  无材料
                </span>

              </template>

            </el-table-column>


            <!-- 终审操作 -->

            <el-table-column
              label="操作"
              width="180"
              fixed="right"
              align="center"
            >

              <template #default="{ row }">

                <el-button
                  type="success"
                  link
                  @click="finalApprove(row)"
                >
                  一键通过
                </el-button>


                <el-button
                  type="danger"
                  link
                  @click="finalReject(row)"
                >
                  驳回
                </el-button>

              </template>

            </el-table-column>

          </el-table>


          <el-empty
            v-if="
              !loading &&
              list.length === 0
            "
            description="暂无待终审的个人证书"
          />

        </el-tab-pane>

      </el-tabs>

    </el-card>


    <!-- ==================== 初审填写分值 ==================== -->

    <el-dialog
      v-model="scoreDialogVisible"
      title="填写证书加分"
      width="420px"
      destroy-on-close
    >

      <div
        v-if="currentRow"
        class="score-dialog"
      >

        <div class="student-info">

          <div>
            <span class="label">
              学生：
            </span>

            <span>
              {{ currentRow.studentName || '-' }}
            </span>
          </div>


          <div>
            <span class="label">
              学号：
            </span>

            <span>
              {{ currentRow.studentNo || '-' }}
            </span>
          </div>


          <div>
            <span class="label">
              证书：
            </span>

            <span>
              {{ currentRow.title || '-' }}
            </span>
          </div>

        </div>


        <el-form>

          <el-form-item label="审核加分">

            <el-input-number
              v-model="scoreValue"
              :min="0"
              :max="40"
              :precision="2"
              :step="0.5"
              controls-position="right"
              style="width: 100%"
            />

          </el-form-item>

        </el-form>


        <div class="score-tip">
          请根据学校/学院加分标准填写最终认定分值。
        </div>

      </div>


      <template #footer>

        <el-button
          @click="scoreDialogVisible = false"
        >
          取消
        </el-button>


        <el-button
          type="success"
          :loading="auditLoading"
          @click="confirmPreliminaryApprove"
        >
          确定通过
        </el-button>

      </template>

    </el-dialog>


    <!-- ==================== 材料预览 ==================== -->

    <el-dialog
      v-model="materialDialogVisible"
      title="查看证书材料"
      width="80%"
      top="5vh"
      destroy-on-close
    >

      <div class="material-preview">

        <!-- 图片 -->

        <el-image
          v-if="previewType === 'image'"
          :src="previewUrl"
          fit="contain"
          class="preview-image"
          :preview-src-list="[previewUrl]"
          :initial-index="0"
        />


        <!-- PDF -->

        <iframe
          v-else-if="previewType === 'pdf'"
          :src="previewUrl"
          class="preview-pdf"
          frameborder="0"
        />


        <!-- 其他文件 -->

        <div
          v-else
          class="other-file"
        >

          <el-empty
            description="该文件类型暂不支持在线预览"
          >

            <el-button
              type="primary"
              @click="openNewWindow"
            >
              在新窗口打开
            </el-button>

          </el-empty>

        </div>

      </div>

    </el-dialog>

  </div>

</template>


<script setup>

import {
  ref,
  computed,
  onMounted
} from 'vue'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import request from '@/utils/request'


// =========================================================
// 基础数据
// =========================================================

const list =
  ref([])


const loading =
  ref(false)


const auditLoading =
  ref(false)


const batchLoading =
  ref(false)


const selectedRows =
  ref([])


const activeTab =
  ref('preliminary')



// =========================================================
// 当前用户角色
// =========================================================

const user =
  ref(null)


function loadCurrentUser() {

  try {

    const userString =
      localStorage.getItem('user')


    if (!userString) {

      user.value = {}

      return

    }


    user.value =
      JSON.parse(userString)

  } catch (error) {

    console.error(
      '读取当前用户信息失败',
      error
    )

    user.value = {}

  }

}


/*
 * 干事：
 *     只能初审
 *
 * 副部长：
 *     初审 + 终审
 *
 * 部长：
 *     初审 + 终审
 */

const canFinalAudit =
  computed(() => {

    const departments =
      user.value?.departments


    if (
      !Array.isArray(departments)
    ) {

      return false

    }


    return departments.some(
      department => {

        if (
          department?.departmentName !==
          '档案部'
        ) {

          return false

        }


        return (
          department.position === '副部长' ||
          department.position === '部长'
        )

      }
    )

  })



// =========================================================
// 统一处理后端 Result
// =========================================================

function getResultData(res) {

  /*
   * 你的 request.js 已经：
   *
   * return response.data
   *
   * 所以正常情况下：
   *
   * res =
   * {
   *   code: 200,
   *   message: "...",
   *   data: ...
   * }
   *
   * 这里保留兼容处理，防止以后接口套了一层。
   */

  if (
    res?.data?.code !== undefined
  ) {

    return res.data

  }


  return res

}



// =========================================================
// 获取当前阶段列表
// =========================================================

async function load() {

  loading.value = true

  selectedRows.value = []


  try {

    let res


    if (
      activeTab.value ===
      'final'
    ) {

      res =
        await request.get(
          '/scoreApply/final-pending'
        )

    } else {

      res =
        await request.get(
          '/scoreApply/pending'
        )

    }


    const result =
      getResultData(res)


    if (
      result?.code !== 200 &&
      result?.code !== 0
    ) {

      ElMessage.error(
        result?.message ||
        '获取证书审核数据失败'
      )

      list.value = []

      return

    }


    const data =
      result?.data


    if (
      Array.isArray(data)
    ) {

      list.value = data

    } else {

      list.value =
        data?.records || []

    }

  } catch (error) {

    console.error(
      '获取证书审核数据失败',
      error
    )


    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取证书审核数据失败'
    )


    list.value = []

  } finally {

    loading.value = false

  }

}



// =========================================================
// 切换初审 / 终审
// =========================================================

function handleTabChange() {

  selectedRows.value = []

  load()

}



// =========================================================
// 表格选择
// =========================================================

function handleSelectionChange(
  rows
) {

  selectedRows.value =
    rows || []

}



// =========================================================
// 分值弹窗
// =========================================================

const scoreDialogVisible =
  ref(false)


const currentRow =
  ref(null)


const scoreValue =
  ref(null)



function openScoreDialog(row) {

  currentRow.value =
    row


  scoreValue.value =
    null


  scoreDialogVisible.value =
    true

}



// =========================================================
// 初审通过
// =========================================================

function preliminaryApprove(row) {

  openScoreDialog(row)

}



// =========================================================
// 确认初审通过
// =========================================================

async function confirmPreliminaryApprove() {

  if (!currentRow.value) {

    return

  }


  if (
    scoreValue.value === null ||
    scoreValue.value === undefined ||
    Number(scoreValue.value) <= 0
  ) {

    ElMessage.warning(
      '请先填写审核加分'
    )

    return

  }


  try {

    await ElMessageBox.confirm(

      `确定给「${
        currentRow.value.studentName || ''
      }」认定 ${
        scoreValue.value
      } 分，并通过初审吗？`,

      '初审确认',

      {

        confirmButtonText:
          '确定通过',

        cancelButtonText:
          '取消',

        type:
          'success'

      }

    )

  } catch {

    return

  }


  auditLoading.value =
    true


  try {

    const res =
      await request.post(
        '/scoreApply/preliminary-audit',
        {

          id:
          currentRow.value.id,

          status:
            1,

          finalScore:
            Number(scoreValue.value)

        }
      )


    const result =
      getResultData(res)


    if (
      result?.code !== 200 &&
      result?.code !== 0
    ) {

      ElMessage.error(
        result?.message ||
        '初审失败'
      )

      return

    }


    ElMessage.success(
      '初审通过，已进入终审'
    )


    scoreDialogVisible.value =
      false


    currentRow.value =
      null


    await load()

  } catch (error) {

    console.error(
      '个人证书初审失败',
      error
    )


    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '个人证书初审失败'
    )

  } finally {

    auditLoading.value =
      false

  }

}



// =========================================================
// 初审驳回
// =========================================================

async function preliminaryReject(row) {

  try {

    await ElMessageBox.confirm(

      `确定驳回「${
        row.studentName || ''
      }」的个人证书加分申请吗？`,

      '初审驳回',

      {

        confirmButtonText:
          '确定驳回',

        cancelButtonText:
          '取消',

        type:
          'warning'

      }

    )

  } catch {

    return

  }


  auditLoading.value =
    true


  try {

    const res =
      await request.post(
        '/scoreApply/preliminary-audit',
        {

          id:
          row.id,

          status:
            2

        }
      )


    const result =
      getResultData(res)


    if (
      result?.code !== 200 &&
      result?.code !== 0
    ) {

      ElMessage.error(
        result?.message ||
        '初审驳回失败'
      )

      return

    }


    ElMessage.success(
      '证书申请已驳回'
    )


    await load()

  } catch (error) {

    console.error(
      '初审驳回失败',
      error
    )


    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '初审驳回失败'
    )

  } finally {

    auditLoading.value =
      false

  }

}



// =========================================================
// 批量初审通过
// =========================================================

async function batchPreliminaryApprove() {

  if (
    selectedRows.value.length === 0
  ) {

    return

  }


  /*
   * 批量通过时，每个证书的分值可能不一样，
   * 所以这里不直接提交。
   *
   * 先逐条打开填写流程。
   *
   * 为了避免一个一个点确认，
   * 这里使用循环方式依次填写。
   */

  ElMessage.info(
    '批量通过时需要分别填写每份证书的认定分值，请逐条处理。'
  )


  if (
    selectedRows.value.length > 0
  ) {

    openScoreDialog(
      selectedRows.value[0]
    )

  }

}



// =========================================================
// 终审通过
// =========================================================

async function finalApprove(row) {

  try {

    await ElMessageBox.confirm(

      `确定一键通过「${
        row.studentName || ''
      }」的个人证书加分申请吗？`,

      '终审确认',

      {

        confirmButtonText:
          '一键通过',

        cancelButtonText:
          '取消',

        type:
          'success'

      }

    )

  } catch {

    return

  }


  auditLoading.value =
    true


  try {

    const res =
      await request.post(
        '/scoreApply/final-audit',
        {

          id:
          row.id,

          status:
            1

        }
      )


    const result =
      getResultData(res)


    if (
      result?.code !== 200 &&
      result?.code !== 0
    ) {

      ElMessage.error(
        result?.message ||
        '终审失败'
      )

      return

    }


    ElMessage.success(
      '终审通过，已生成正式成绩记录'
    )


    await load()

  } catch (error) {

    console.error(
      '个人证书终审失败',
      error
    )


    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '个人证书终审失败'
    )

  } finally {

    auditLoading.value =
      false

  }

}



// =========================================================
// 终审驳回
// =========================================================

async function finalReject(row) {

  try {

    await ElMessageBox.confirm(

      `确定驳回「${
        row.studentName || ''
      }」的个人证书加分申请吗？`,

      '终审驳回',

      {

        confirmButtonText:
          '确定驳回',

        cancelButtonText:
          '取消',

        type:
          'warning'

      }

    )

  } catch {

    return

  }


  auditLoading.value =
    true


  try {

    const res =
      await request.post(
        '/scoreApply/final-audit',
        {

          id:
          row.id,

          status:
            2

        }
      )


    const result =
      getResultData(res)


    if (
      result?.code !== 200 &&
      result?.code !== 0
    ) {

      ElMessage.error(
        result?.message ||
        '终审驳回失败'
      )

      return

    }


    ElMessage.success(
      '证书申请已驳回'
    )


    await load()

  } catch (error) {

    console.error(
      '终审驳回失败',
      error
    )

  } finally {

    auditLoading.value =
      false

  }

}



// =========================================================
// 批量终审通过
// =========================================================

async function batchFinalApprove() {

  if (
    selectedRows.value.length === 0
  ) {

    return

  }


  try {

    await ElMessageBox.confirm(

      `确定一键通过选中的 ${
        selectedRows.value.length
      } 份个人证书申请吗？`,

      '批量终审',

      {

        confirmButtonText:
          '一键通过',

        cancelButtonText:
          '取消',

        type:
          'success'

      }

    )

  } catch {

    return

  }


  batchLoading.value =
    true


  try {

    const ids =
      selectedRows.value.map(
        row => row.id
      )


    const res =
      await request.post(
        '/scoreApply/final-audit/batch',
        {
          ids
        }
      )


    const result =
      getResultData(res)


    if (
      result?.code !== 200 &&
      result?.code !== 0
    ) {

      ElMessage.error(
        result?.message ||
        '批量终审失败'
      )

      return

    }


    ElMessage.success(
      '批量终审通过'
    )


    await load()

  } catch (error) {

    console.error(
      '批量终审失败',
      error
    )


    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '批量终审失败'
    )

  } finally {

    batchLoading.value =
      false

  }

}



// =========================================================
// 材料预览
// =========================================================

const materialDialogVisible =
  ref(false)


const previewUrl =
  ref('')


const previewType =
  ref('other')



function getFileExtension(url) {

  if (!url) {

    return ''

  }


  const cleanUrl =
    url
      .split('?')[0]
      .split('#')[0]


  const index =
    cleanUrl.lastIndexOf('.')


  if (index === -1) {

    return ''

  }


  return cleanUrl
    .substring(index + 1)
    .toLowerCase()

}



function openFile(url) {

  if (!url) {

    ElMessage.warning(
      '暂无材料'
    )

    return

  }


  previewUrl.value =
    url


  const extension =
    getFileExtension(url)


  if (
    [
      'jpg',
      'jpeg',
      'png',
      'gif',
      'webp',
      'bmp'
    ].includes(extension)
  ) {

    previewType.value =
      'image'

  } else if (
    extension === 'pdf'
  ) {

    previewType.value =
      'pdf'

  } else {

    previewType.value =
      'other'

  }


  materialDialogVisible.value =
    true

}



function openNewWindow() {

  if (!previewUrl.value) {

    return

  }


  window.open(
    previewUrl.value,
    '_blank'
  )

}



// =========================================================
// 分值格式化
// =========================================================

function formatScore(score) {

  if (
    score === null ||
    score === undefined ||
    score === ''
  ) {

    return '-'

  }


  return Number(score).toFixed(2)

}



// =========================================================
// 初始化
// =========================================================

onMounted(() => {

  loadCurrentUser()

  load()

})

</script>


<style scoped>

.certificate-page {

  padding:
    30px;

}


.header {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    center;

}


.header h2 {

  margin:
    0 0 8px;

}


.header p {

  margin:
    0;

  color:
    #909399;

}


.toolbar {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    center;

  margin:
    0 0 15px;

}


.toolbar-left {

  display:
    flex;

  align-items:
    center;

  gap:
    15px;

}


.selected-text {

  color:
    #909399;

  font-size:
    14px;

}


.score-text {

  font-weight:
    600;

}


.finished {

  color:
    #999;

}


.no-file {

  color:
    #c0c4cc;

}


.score-dialog {

  padding:
    5px 10px;

}


.student-info {

  padding:
    12px;

  margin-bottom:
    20px;

  background:
    #f5f7fa;

  border-radius:
    6px;

  line-height:
    30px;

}


.student-info .label {

  display:
    inline-block;

  width:
    60px;

  color:
    #909399;

}


.score-tip {

  color:
    #909399;

  font-size:
    13px;

  line-height:
    22px;

}


.material-preview {

  width:
    100%;

  min-height:
    500px;

  display:
    flex;

  justify-content:
    center;

  align-items:
    center;

  background:
    #f5f7fa;

}


.preview-image {

  max-width:
    100%;

  max-height:
    70vh;

}


.preview-pdf {

  width:
    100%;

  height:
    70vh;

  background:
    white;

}


.other-file {

  width:
    100%;

  min-height:
    400px;

  display:
    flex;

  justify-content:
    center;

  align-items:
    center;

}

</style>
