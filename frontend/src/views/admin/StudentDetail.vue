<template>
  <div class="adjust-page">
    <el-card>
      <template #header>
        <div class="header">
          <span>管理员成绩调整</span>

          <el-button
            type="primary"
            @click="openDialog"
          >
            新增调整
          </el-button>
        </div>
      </template>

      <!-- =========================
           调整记录
      ========================== -->
      <el-table
        :data="list"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
      >

        <el-table-column
          type="index"
          label="#"
          width="70"
          :index="indexMethod"
        />

        <el-table-column
          prop="studentName"
          label="学生姓名"
          min-width="120"
        />

        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="140"
        />

        <el-table-column
          label="调整类型"
          width="100"
        >
          <template #default="scope">

            <el-tag
              v-if="scope.row.adjustType === 1"
              type="success"
            >
              加分
            </el-tag>

            <el-tag
              v-else-if="scope.row.adjustType === -1"
              type="danger"
            >
              减分
            </el-tag>

            <el-tag
              v-else
              type="info"
            >
              未知
            </el-tag>

          </template>
        </el-table-column>

        <el-table-column
          prop="score"
          label="调整分数"
          width="110"
        />

        <el-table-column
          prop="reason"
          label="调整原因"
          min-width="220"
          show-overflow-tooltip
        />

        <el-table-column
          prop="adminName"
          label="操作管理员"
          min-width="120"
        />

        <el-table-column
          prop="createTime"
          label="操作时间"
          min-width="180"
        />

      </el-table>


      <!-- =========================
           分页
      ========================== -->
      <div class="pagination-wrapper">

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />

      </div>

    </el-card>


    <!-- =========================
         新增弹窗
    ========================== -->
    <el-dialog
      v-model="dialogVisible"
      title="成绩调整"
      width="600px"
      destroy-on-close
    >

      <el-form
        :model="form"
        label-width="100px"
      >

        <!-- 学生 -->
        <el-form-item label="学生">

          <el-select
            v-model="form.studentId"
            placeholder="请选择学生"
            filterable
            clearable
            remote
            :remote-method="searchStudents"
            :loading="studentLoading"
            style="width: 100%"
          >

            <el-option
              v-for="s in studentList"
              :key="s.id"
              :label="`${s.realName}(${s.studentNo})`"
              :value="s.id"
            />

          </el-select>

        </el-form-item>


        <!-- 调整类型 -->
        <el-form-item label="调整类型">

          <el-radio-group
            v-model="form.adjustType"
          >

            <el-radio :label="1">
              加分
            </el-radio>

            <el-radio :label="-1">
              减分
            </el-radio>

          </el-radio-group>

        </el-form-item>


        <!-- 分数 -->
        <el-form-item label="分数">

          <el-input-number
            v-model="form.score"
            :min="0.01"
            :step="0.5"
            :precision="2"
          />

        </el-form-item>


        <!-- 原因 -->
        <el-form-item label="调整原因">

          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="填写调整原因"
          />

        </el-form-item>

      </el-form>


      <!-- =========================
           弹窗底部
      ========================== -->
      <template #footer>

        <el-button
          @click="dialogVisible = false"
        >
          取消
        </el-button>

        <el-button
          type="primary"
          :loading="submitLoading"
          @click="submit"
        >
          确认调整
        </el-button>

      </template>

    </el-dialog>

  </div>
</template>


<script setup>

import {
  ref,
  onMounted
} from 'vue'

import {
  ElMessage
} from 'element-plus'

import request from '@/utils/request'


/*
 * =========================================================
 * 调整记录
 * =========================================================
 */

const loading = ref(false)

const list = ref([])

const page = ref(1)

const pageSize = ref(10)

const total = ref(0)


/*
 * =========================================================
 * 新增弹窗
 * =========================================================
 */

const dialogVisible = ref(false)

const submitLoading = ref(false)


const form = ref({
  studentId: null,
  adjustType: 1,
  score: 0,
  reason: ''
})


/*
 * =========================================================
 * 学生选择
 * =========================================================
 */

const studentList = ref([])

const studentLoading = ref(false)


/*
 * =========================================================
 * 计算表格序号
 * =========================================================
 */

function indexMethod(index) {

  return (
    (page.value - 1) *
    pageSize.value +
    index +
    1
  )

}


/*
 * =========================================================
 * 打开新增弹窗
 * =========================================================
 */

function openDialog() {

  form.value = {
    studentId: null,
    adjustType: 1,
    score: 0,
    reason: ''
  }

  studentList.value = []

  dialogVisible.value = true

  /*
   * 打开弹窗时先加载第一页学生
   */
  loadStudents()

}


/*
 * =========================================================
 * 加载学生
 *
 * 注意：
 *
 * 这里也使用分页。
 *
 * 不再一次性加载所有学生。
 * =========================================================
 */

async function loadStudents(
  keyword = ''
) {

  studentLoading.value = true

  try {

    const res =
      await request.get(
        '/user/student/list',
        {
          params: {
            page: 1,
            pageSize: 20,
            studentNo: keyword,
            realName: keyword
          }
        }
      )


    const data =
      res.data.data


    if (
      data &&
      Array.isArray(data.records)
    ) {

      studentList.value =
        data.records

    } else {

      studentList.value = []

    }

  } catch (e) {

    console.error(
      '获取学生失败',
      e
    )

    ElMessage.error(
      '获取学生列表失败'
    )

  } finally {

    studentLoading.value = false

  }

}


/*
 * =========================================================
 * 学生远程搜索
 * =========================================================
 */

function searchStudents(keyword) {

  if (
    keyword === undefined
    || keyword === null
  ) {

    keyword = ''

  }


  loadStudents(
    keyword.trim()
  )

}


/*
 * =========================================================
 * 加载成绩调整记录
 * =========================================================
 */

async function loadList() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/admin/scoreAdjustment/list',
        {
          params: {
            page: page.value,
            pageSize: pageSize.value
          }
        }
      )


    console.log(
      '成绩调整分页数据：',
      res
    )


    const data =
      res.data.data


    /*
     * 后端分页返回：
     *
     * {
     *   records: [],
     *   total: 100,
     *   current: 1,
     *   size: 10,
     *   pages: 10
     * }
     */

    if (
      data &&
      Array.isArray(data.records)
    ) {

      list.value =
        data.records

      total.value =
        Number(data.total || 0)

    } else {

      /*
       * 防止后端暂时还是返回数组
       */
      if (
        Array.isArray(data)
      ) {

        list.value = data

        total.value =
          data.length

      } else {

        list.value = []

        total.value = 0

      }

    }

  } catch (e) {

    console.error(
      '获取调整记录失败',
      e
    )

    ElMessage.error(
      '获取调整记录失败'
    )

  } finally {

    loading.value = false

  }

}


/*
 * =========================================================
 * 切换每页数量
 * =========================================================
 */

function handleSizeChange(size) {

  pageSize.value = size

  page.value = 1

  loadList()

}


/*
 * =========================================================
 * 切换页码
 * =========================================================
 */

function handleCurrentChange(current) {

  page.value = current

  loadList()

}


/*
 * =========================================================
 * 提交成绩调整
 * =========================================================
 */

async function submit() {

  if (
    !form.value.studentId
  ) {

    ElMessage.warning(
      '请选择学生'
    )

    return

  }


  if (
    !form.value.score
    || form.value.score <= 0
  ) {

    ElMessage.warning(
      '请填写正确的分数'
    )

    return

  }


  if (
    !form.value.reason
    || !form.value.reason.trim()
  ) {

    ElMessage.warning(
      '请填写调整原因'
    )

    return

  }


  submitLoading.value = true

  try {

    await request.post(
      '/admin/scoreAdjustment/add',
      form.value
    )


    ElMessage.success(
      '调整成功'
    )


    dialogVisible.value =
      false


    /*
     * 回到第一页
     */
    page.value = 1


    /*
     * 重新加载
     */
    await loadList()

  } catch (err) {

    console.error(
      '成绩调整失败',
      err
    )


    ElMessage.error(
      err.response?.data?.msg
      ||
      err.response?.data?.message
      ||
      '操作失败'
    )

  } finally {

    submitLoading.value = false

  }

}


/*
 * =========================================================
 * 初始化
 * =========================================================
 */

onMounted(() => {

  loadList()

})

</script>


<style scoped>

.adjust-page {
  padding: 30px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-bottom: 10px;
}

</style>
