<template>
  <div class="adjust-page">

    <el-card shadow="never">

      <template #header>

        <div class="header">

          <div>
            <span class="title">
              管理员成绩调整
            </span>

            <span class="subtitle">
              手动为学生增加或扣除综合测评分
            </span>
          </div>

          <el-button
            type="primary"
            @click="openDialog"
          >
            新增调整
          </el-button>

        </div>

      </template>


      <!-- =====================================================
           调整记录
      ====================================================== -->

      <el-table
        :data="list"
        border
        stripe
        v-loading="loading"
      >

        <el-table-column
          prop="studentName"
          label="学生姓名"
          min-width="110"
        />

        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="150"
        />

        <el-table-column
          label="调整类型"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="Number(row.adjustType) === 1"
              type="success"
            >
              加分
            </el-tag>

            <el-tag
              v-else-if="Number(row.adjustType) === -1"
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
          label="调整分数"
          width="110"
          align="center"
        >

          <template #default="{ row }">

            <span
              :class="
                Number(row.adjustType) === 1
                  ? 'score-plus'
                  : 'score-minus'
              "
            >

              {{
                Number(row.adjustType) === 1
                  ? '+' + row.score
                  : '-' + row.score
              }}

            </span>

          </template>

        </el-table-column>


        <el-table-column
          prop="reason"
          label="调整原因"
          min-width="220"
          show-overflow-tooltip
        />


        <el-table-column
          prop="adminName"
          label="操作管理员"
          width="130"
        />


        <el-table-column
          prop="createTime"
          label="操作时间"
          width="180"
        />

      </el-table>


      <!-- 空数据 -->

      <el-empty
        v-if="!loading && list.length === 0"
        description="暂无成绩调整记录"
      />

    </el-card>


    <!-- =====================================================
         新增成绩调整
    ====================================================== -->

    <el-dialog
      v-model="dialogVisible"
      title="新增成绩调整"
      width="600px"
      destroy-on-close
    >

      <el-form
        ref="formRef"
        :model="form"
        label-width="100px"
      >

        <!-- 学生 -->

        <el-form-item
          label="学生"
          required
        >

          <el-select
            v-model="form.studentId"
            placeholder="请输入姓名或学号搜索"
            filterable
            clearable
            :filter-method="filterStudent"
            style="width: 100%"
            @clear="handleStudentClear"
          >

            <el-option
              v-for="student in filteredStudents"
              :key="student.id"
              :label="
                `${student.realName || ''}（${student.studentNo || ''}）`
              "
              :value="student.id"
            />

          </el-select>

        </el-form-item>


        <!-- 调整类型 -->

        <el-form-item
          label="调整类型"
          required
        >

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

        <el-form-item
          label="调整分数"
          required
        >

          <el-input-number
            v-model="form.score"
            :min="0.01"
            :step="0.5"
            :precision="2"
            controls-position="right"
            style="width: 200px"
          />

        </el-form-item>


        <!-- 原因 -->

        <el-form-item
          label="调整原因"
          required
        >

          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写本次成绩调整的具体原因"
          />

        </el-form-item>

      </el-form>


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
  computed,
  onMounted
} from 'vue'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import request from '@/utils/request'


/* =========================================================
 * 基础状态
 * ========================================================= */

const loading = ref(false)

const submitLoading = ref(false)

const dialogVisible = ref(false)


/* =========================================================
 * 调整记录
 * ========================================================= */

const list = ref([])


/* =========================================================
 * 学生列表
 *
 * 直接获取全部学生
 *
 * GET /user/student/all
 * ========================================================= */

const studentList = ref([])


/* =========================================================
 * 学生搜索关键字
 * ========================================================= */

const studentKeyword = ref('')


/* =========================================================
 * 过滤后的学生
 * ========================================================= */

const filteredStudents = computed(() => {

  const keyword =
    studentKeyword.value
      .trim()
      .toLowerCase()

  if (!keyword) {

    return studentList.value

  }

  return studentList.value.filter(
    student => {

      const name =
        String(
          student.realName || ''
        )
          .toLowerCase()

      const studentNo =
        String(
          student.studentNo || ''
        )
          .toLowerCase()

      const username =
        String(
          student.username || ''
        )
          .toLowerCase()

      return (
        name.includes(keyword)
        ||
        studentNo.includes(keyword)
        ||
        username.includes(keyword)
      )

    }
  )

})


/* =========================================================
 * 表单
 * ========================================================= */

const form = ref({
  studentId: null,
  adjustType: 1,
  score: 0,
  reason: ''
})


/* =========================================================
 * 重置表单
 * ========================================================= */

function resetForm() {

  form.value = {

    studentId: null,

    adjustType: 1,

    score: 0,

    reason: ''

  }

  studentKeyword.value = ''

}


/* =========================================================
 * 打开新增弹窗
 * ========================================================= */

async function openDialog() {

  resetForm()

  dialogVisible.value = true

  /*
   * 如果还没有学生数据，
   * 打开弹窗时再加载一次。
   */

  if (studentList.value.length === 0) {

    await loadStudents()

  }

}


/* =========================================================
 * 加载全部学生
 *
 * 注意：
 *
 * /utils/request 已经把 Axios response.data
 * 返回出来了。
 *
 * 后端：
 *
 * Result<List<StudentVO>>
 *
 * 所以：
 *
 * res.data
 *
 * 才是学生数组。
 * ========================================================= */

async function loadStudents() {

  try {

    const res =
      await request.get(
        '/user/student/all'
      )


    console.log(
      '管理员成绩调整：学生列表返回：',
      res
    )


    /*
     * 后端返回：
     *
     * {
     *   code: 200,
     *   message: "操作成功",
     *   data: [...]
     * }
     *
     * 因为 request.js 已经 return response.data，
     * 所以这里的 res 就是上面这个对象。
     *
     * 因此真正的学生数组：
     *
     * res.data
     */

    if (
      res
      &&
      res.code === 200
      &&
      Array.isArray(res.data)
    ) {

      studentList.value =
        res.data

    } else {

      studentList.value = []

      console.error(
        '学生列表数据格式异常：',
        res
      )

      ElMessage.error(
        '获取学生列表失败'
      )

    }

  } catch (error) {

    console.error(
      '获取学生列表失败：',
      error
    )

    studentList.value = []

    ElMessage.error(
      '获取学生列表失败'
    )

  }

}


/* =========================================================
 * 学生搜索
 * ========================================================= */

function filterStudent(
  keyword
) {

  studentKeyword.value =
    keyword || ''

}


/* =========================================================
 * 清除学生搜索
 * ========================================================= */

function handleStudentClear() {

  studentKeyword.value = ''

}


/* =========================================================
 * 加载调整记录
 * ========================================================= */

async function loadList() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/admin/scoreAdjustment/list',
        {
          params: {
            pageNum: 1,
            pageSize: 100
          }
        }
      )


    console.log(
      '管理员成绩调整记录：',
      res
    )


    /*
     * 后端：
     *
     * Result<Page<ScoreAdminAdjustmentVO>>
     *
     *
     * request 已经返回 Result，
     *
     * 所以：
     *
     * res.data
     *
     * 是 Page
     *
     * res.data.records
     *
     * 才是真正的记录数组。
     */

    if (
      res
      &&
      res.code === 200
      &&
      res.data
    ) {

      list.value =
        Array.isArray(
          res.data.records
        )
          ? res.data.records
          : []

    } else {

      list.value = []

    }

  } catch (error) {

    console.error(
      '获取调整记录失败：',
      error
    )

    ElMessage.error(
      '获取调整记录失败'
    )

  } finally {

    loading.value = false

  }

}


/* =========================================================
 * 提交成绩调整
 * ========================================================= */

async function submit() {

  /*
   * 1. 学生
   */

  if (!form.value.studentId) {

    ElMessage.warning(
      '请选择学生'
    )

    return

  }


  /*
   * 2. 分数
   */

  if (
    !form.value.score
    ||
    Number(form.value.score) <= 0
  ) {

    ElMessage.warning(
      '请输入大于 0 的调整分数'
    )

    return

  }


  /*
   * 3. 原因
   */

  if (
    !form.value.reason
    ||
    !form.value.reason.trim()
  ) {

    ElMessage.warning(
      '请输入调整原因'
    )

    return

  }


  /*
   * 找到学生
   */

  const student =
    studentList.value.find(
      item =>
        item.id ===
        form.value.studentId
    )


  const studentName =
    student?.realName
    || '该学生'


  /*
   * 二次确认
   */

  try {

    await ElMessageBox.confirm(
      `确定要为「${studentName}」${Number(form.value.adjustType) === 1 ? '加分' : '减分'} ${form.value.score} 分吗？`,
      '确认成绩调整',
      {
        confirmButtonText: '确定调整',
        cancelButtonText: '取消',
        type:
          Number(form.value.adjustType) === 1
            ? 'success'
            : 'warning'
      }
    )

  } catch {

    return

  }


  submitLoading.value = true


  try {

    const res =
      await request.post(
        '/admin/scoreAdjustment/add',
        {
          studentId:
          form.value.studentId,

          adjustType:
            Number(form.value.adjustType),

          score:
          form.value.score,

          reason:
            form.value.reason.trim()
        }
      )


    console.log(
      '成绩调整返回：',
      res
    )


    if (
      res
      &&
      res.code === 200
    ) {

      ElMessage.success(
        '成绩调整成功'
      )

      dialogVisible.value = false

      resetForm()

      await loadList()

    } else {

      ElMessage.error(
        res?.message
        || '成绩调整失败'
      )

    }

  } catch (error) {

    console.error(
      '成绩调整失败：',
      error
    )

    ElMessage.error(
      error.response?.data?.message
      || error.response?.data?.msg
      || '成绩调整失败'
    )

  } finally {

    submitLoading.value = false

  }

}


/* =========================================================
 * 页面初始化
 * ========================================================= */

onMounted(async () => {

  await Promise.all([
    loadStudents(),
    loadList()
  ])

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

.title {
  font-size: 18px;
  font-weight: 600;
}

.subtitle {
  margin-left: 15px;
  color: #909399;
  font-size: 13px;
}

.score-plus {
  color: #67c23a;
  font-weight: 600;
}

.score-minus {
  color: #f56c6c;
  font-weight: 600;
}

</style>
