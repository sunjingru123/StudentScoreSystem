<template>
  <div class="score-page">

    <!-- =====================================================
         页面标题
         ===================================================== -->

    <div class="page-header">

      <div>

        <h2>
          学生管理
        </h2>

        <p>
          查看学生信息及综合测评成绩
        </p>

      </div>

    </div>


    <!-- =====================================================
         搜索
         ===================================================== -->

    <el-card
      shadow="never"
      class="search-card"
    >

      <el-form :inline="true">

        <el-form-item label="搜索">

          <el-input
            v-model="keyword"
            placeholder="学号 / 姓名 / 用户名 / 班级"
            clearable
            style="width: 280px"
            @keyup.enter="handleSearch"
          />

        </el-form-item>


        <el-form-item>

          <el-button
            type="primary"
            @click="handleSearch"
          >
            搜索
          </el-button>


          <el-button
            @click="handleReset"
          >
            重置
          </el-button>

        </el-form-item>

      </el-form>

    </el-card>


    <!-- =====================================================
         学生列表
         ===================================================== -->

    <el-card
      shadow="never"
      class="table-card"
    >

      <el-table
        :data="students"
        border
        stripe
        v-loading="loading"
        row-key="id"
      >

        <!-- 序号 -->

        <el-table-column
          type="index"
          label="#"
          width="60"
          :index="indexMethod"
        />


        <!-- 学号 -->

        <el-table-column
          prop="studentNo"
          label="学号"
          width="150"
        />


        <!-- 姓名 -->

        <el-table-column
          prop="realName"
          label="姓名"
          width="120"
        />


        <!-- 班级 -->

        <el-table-column
          prop="className"
          label="班级"
          min-width="160"
        />


        <!-- 状态 -->

        <el-table-column
          label="状态"
          width="90"
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
              type="danger"
            >
              禁用
            </el-tag>

          </template>

        </el-table-column>


        <!-- 加分 -->

        <el-table-column
          label="加分"
          width="90"
          align="center"
        >
          <template #default="{ row }">
    <span class="score">
      {{ formatScore(row.scoreStatistics?.bonusScore ?? 0) }}
    </span>
          </template>
        </el-table-column>

        <!-- 减分 -->

        <el-table-column
          label="减分"
          width="90"
          align="center"
        >
          <template #default="{ row }">
    <span class="score">
      {{ formatScore(row.scoreStatistics?.deductScore ?? 0) }}
    </span>
          </template>
        </el-table-column>


        <!-- 操作 -->

        <el-table-column
          label="操作"
          width="250"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              type="primary"
              link
              @click="openDetail(row)"
            >
              查看成绩
            </el-button>

            <el-button
              type="warning"
              link
              @click="handleResetPassword(row)"
            >
              重置密码
            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- ===================================================
           无数据
           =================================================== -->

      <el-empty
        v-if="
          !loading &&
          students.length === 0
        "
        description="暂无学生数据"
      />


      <!-- ===================================================
           学生列表分页
           =================================================== -->

      <div
        v-if="total > 0"
        class="pagination-wrapper"
      >

        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          background
          :hide-on-single-page="false"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />

      </div>

    </el-card>


    <!-- =====================================================
         学生成绩明细抽屉
         ===================================================== -->

    <el-drawer
      v-model="drawerVisible"
      title="学生成绩明细"
      size="720px"
    >

      <!-- ===================================================
           学生基本信息
           =================================================== -->

      <div
        v-if="currentStudent"
        class="student-info"
      >

        <div>

          <span>
            姓名：
          </span>

          <strong>
            {{ currentStudent.realName || '-' }}
          </strong>

        </div>


        <div>

          <span>
            学号：
          </span>

          <strong>
            {{ currentStudent.studentNo || '-' }}
          </strong>

        </div>


        <div>

          <span>
            班级：
          </span>

          <strong>
            {{ currentStudent.className || '-' }}
          </strong>

        </div>


        <div>

          <span>
            状态：
          </span>

          <el-tag
            v-if="Number(currentStudent.status) === 1"
            type="success"
          >
            正常
          </el-tag>

          <el-tag
            v-else
            type="danger"
          >
            禁用
          </el-tag>

        </div>


        <!-- 加减分 -->

        <div class="total-score">

  <span>
    加分：
    <strong>
      {{ formatScore(currentStudent?.scoreStatistics?.bonusScore ?? 0) }}
    </strong>
  </span>

          <span style="margin-left: 30px;">
    减分：
    <strong>
      {{ formatScore(currentStudent?.scoreStatistics?.deductScore ?? 0) }}
    </strong>
  </span>

        </div>

      </div>


      <el-divider />


      <!-- ===================================================
           成绩明细
           =================================================== -->

      <el-table
        :data="scoreDetails"
        border
        stripe
        v-loading="detailLoading"
      >

        <!-- 评分项目 -->

        <el-table-column
          prop="ruleName"
          label="评分项目"
          min-width="150"
        />


        <!-- 分数 -->

        <el-table-column
          label="分数"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <span
              :class="
                Number(row.score) >= 0
                  ? 'score-plus'
                  : 'score-minus'
              "
            >

              {{
                Number(row.score) >= 0
                  ? '+'
                  : ''
              }}

              {{ formatScore(row.score) }}

            </span>

          </template>

        </el-table-column>


        <!-- 来源 -->

        <el-table-column
          label="来源"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            {{ formatSource(row.sourceType) }}

          </template>

        </el-table-column>


        <!-- 状态 -->

        <el-table-column
          label="状态"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="Number(row.adminHidden) === 1"
              type="danger"
            >
              已隐藏
            </el-tag>


            <el-tag
              v-else
              type="success"
            >
              正常
            </el-tag>

          </template>

        </el-table-column>


        <!-- 操作 -->

        <el-table-column
          label="操作"
          width="110"
          align="center"
        >

          <template #default="{ row }">

            <!-- 正常成绩 -->

            <el-button
              v-if="Number(row.adminHidden) === 0"
              type="danger"
              link
              @click="handleHide(row)"
            >
              隐藏
            </el-button>


            <!-- 已隐藏成绩 -->

            <el-button
              v-else
              type="success"
              link
              @click="handleShow(row)"
            >
              恢复
            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- ===================================================
           成绩为空
           =================================================== -->

      <el-empty
        v-if="
          !detailLoading &&
          scoreDetails.length === 0
        "
        description="暂无成绩明细"
      />

    </el-drawer>

  </div>
</template>


<script setup>

import {
  ref,
  onMounted,
} from 'vue'


import {
  ElMessage,
  ElMessageBox,
} from 'element-plus'


import {
  getAdminStudentScores,
  getAdminStudentTotal,
  hideScore,
  showScore,
} from '@/api/adminScore'


import request from '@/api/request'
import {
  resetStudentPassword,
} from '@/api/student'


/* =========================================================
 * 学生列表
 * ========================================================= */

const loading =
  ref(false)


const students =
  ref([])


const total =
  ref(0)


const pageNum =
  ref(1)


const pageSize =
  ref(10)


const keyword =
  ref('')


/* =========================================================
 * 成绩抽屉
 * ========================================================= */

const detailLoading =
  ref(false)


const drawerVisible =
  ref(false)


const currentStudent =
  ref(null)


const scoreDetails =
  ref([])


const currentTotal =
  ref(0)


/* =========================================================
 * 统一解析 Result
 * ========================================================= */

function unwrapResult(res) {

  if (!res) {

    return null

  }


  /*
   * AxiosResponse
   *
   * res.data = {
   *   code: 200,
   *   message: '操作成功',
   *   data: ...
   * }
   */

  if (
    res.data &&
    typeof res.data === 'object' &&
    (
      res.data.code !== undefined ||
      res.data.data !== undefined
    )
  ) {

    return res.data.data

  }


  /*
   * 已经是 Result
   */

  if (
    res.code !== undefined ||
    res.data !== undefined
  ) {

    return res.data

  }


  return res

}


/* =========================================================
 * 加载学生列表
 * ========================================================= */

async function loadStudents() {

  loading.value =
    true

  try {

    const res =
      await request.get(
        '/user/student/list',
        {
          params: {

            pageNum:
            pageNum.value,

            pageSize:
            pageSize.value,

            keyword:
              keyword.value.trim()
              || undefined,

          },
        },
      )


    console.log(
      '学生列表原始返回：',
      res,
    )


    const data =
      unwrapResult(res)


    console.log(
      '解析后的学生列表：',
      data,
    )


    /*
     * MyBatis-Plus Page
     */

    if (
      data &&
      Array.isArray(data.records)
    ) {

      students.value =
        data.records

      total.value =
        Number(
          data.total || 0,
        )

    }


    /*
     * 兼容直接数组
     */

    else if (
      Array.isArray(data)
    ) {

      students.value =
        data

      total.value =
        data.length

    }


    /*
     * 数据异常
     */

    else {

      students.value =
        []

      total.value =
        0

      console.error(
        '学生列表数据格式异常：',
        data,
      )

    }


    /*
     * 查询当前页学生综合评分
     */

    await loadCurrentPageTotals()

  }
  catch (error) {

    console.error(
      '学生列表加载失败：',
      error,
    )

    students.value =
      []

    total.value =
      0

    ElMessage.error(
      '学生列表加载失败',
    )

  }
  finally {

    loading.value =
      false

  }

}


/* =========================================================
 * 加载当前页学生综合评分
 * ========================================================= */

async function loadCurrentPageTotals() {

  if (
    !students.value ||
    students.value.length === 0
  ) {

    return

  }


  await Promise.all(

    students.value.map(
      async student => {

        try {

          const res =
            await getAdminStudentTotal(
              student.id,
            )


          const statistics =
            unwrapResult(res)
            || {}


          student.totalScore =
            Number(
              statistics.totalScore ?? 0,
            )


          student.scoreStatistics =
            statistics

        }
        catch (error) {

          console.error(
            '获取学生综合评分失败：',
            student.id,
            error,
          )

          student.totalScore =
            0

          student.scoreStatistics =
            {}

        }

      },
    ),

  )

}


/* =========================================================
 * 搜索
 * ========================================================= */

function handleSearch() {

  pageNum.value =
    1

  loadStudents()

}


/* =========================================================
 * 重置
 * ========================================================= */

function handleReset() {

  keyword.value =
    ''

  pageNum.value =
    1

  pageSize.value =
    10

  loadStudents()

}


/* =========================================================
 * 学生列表分页
 * ========================================================= */

function handlePageChange(page) {

  pageNum.value =
    page

  loadStudents()

}


function handleSizeChange(size) {

  pageSize.value =
    size

  pageNum.value =
    1

  loadStudents()

}


/* =========================================================
 * 表格序号
 * ========================================================= */

function indexMethod(index) {

  return (
    (pageNum.value - 1)
    *
    pageSize.value
    +
    index
    +
    1
  )

}


/* =========================================================
 * 格式化分数
 * ========================================================= */

function formatScore(score) {

  const value =
    Number(score)


  if (
    Number.isNaN(value)
  ) {

    return '0'

  }


  if (
    Number.isInteger(value)
  ) {

    return String(value)

  }


  return value.toFixed(2)

}


/* =========================================================
 * 查看学生成绩
 * ========================================================= */

async function openDetail(student) {

  /*
   * 保存当前学生
   */

  currentStudent.value =
    student


  /*
   * 打开抽屉
   */

  drawerVisible.value =
    true


  /*
   * 清空上一个学生的成绩
   */

  scoreDetails.value =
    []


  /*
   * 先显示学生列表里的总成绩
   */

  currentTotal.value =
    Number(
      student.totalScore ?? 0,
    )


  /*
   * 加载全部成绩
   */

  await loadScoreDetail()


  /*
   * 刷新一次综合评分
   */

  await refreshCurrentTotal()

}


/* =========================================================
 * 加载学生全部成绩明细
 *
 * 后端接口：
 *
 * GET /admin/score/student/{studentId}
 *
 * 返回：
 *
 * Result<List<AdminScoreDetailVO>>
 *
 * 所以：
 *
 * data 就是数组
 * ========================================================= */

async function loadScoreDetail() {

  if (
    !currentStudent.value?.id
  ) {

    return

  }


  detailLoading.value =
    true

  try {

    const res =
      await getAdminStudentScores(
        currentStudent.value.id,
      )


    console.log(
      '学生成绩明细原始返回：',
      res,
    )


    const data =
      unwrapResult(res)


    console.log(
      '解析后的学生成绩明细：',
      data,
    )


    /*
     * 后端直接返回 List
     */

    if (
      Array.isArray(data)
    ) {

      scoreDetails.value =
        data

    }
    else {

      scoreDetails.value =
        []

      console.error(
        '学生成绩明细数据格式异常：',
        data,
      )

    }

  }
  catch (error) {

    console.error(
      '学生成绩明细加载失败：',
      error,
    )

    scoreDetails.value =
      []

    ElMessage.error(
      '学生成绩明细加载失败',
    )

  }
  finally {

    detailLoading.value =
      false

  }

}


/* =========================================================
 * 刷新当前学生综合评分
 * ========================================================= */

async function refreshCurrentTotal() {

  if (
    !currentStudent.value?.id
  ) {

    return

  }


  try {

    const res =
      await getAdminStudentTotal(
        currentStudent.value.id,
      )


    const statistics =
      unwrapResult(res)


    /*
     * ScoreStatisticsVO
     */

    if (
      statistics &&
      typeof statistics === 'object'
    ) {

      currentTotal.value =
        Number(
          statistics.totalScore ?? 0,
        )

    }
    else {

      currentTotal.value =
        Number(
          statistics ?? 0,
        )

    }

  }
  catch (error) {

    console.error(
      '刷新综合评分失败：',
      error,
    )

  }

}


/* =========================================================
 * 来源
 * ========================================================= */

function formatSource(sourceType) {

  if (
    sourceType === 'DEPARTMENT'
  ) {

    return '部门申报'

  }


  if (
    sourceType === 'PERSONAL'
  ) {

    return '个人申请'

  }


  if (
    sourceType === 'ADMIN'
  ) {

    return '管理员调整'

  }


  return sourceType || '-'

}


/* =========================================================
 * 隐藏成绩
 * ========================================================= */

async function handleHide(row) {

  try {

    await ElMessageBox.confirm(
      '确定隐藏这条成绩吗？',
      '提示',
      {
        type: 'warning',
      },
    )


    /*
     * 调用管理员隐藏接口
     */

    await hideScore(
      row.id,
    )


    ElMessage.success(
      '隐藏成功',
    )


    /*
     * 重新加载成绩
     *
     * 后端仍然会返回这条记录，
     * 只是 adminHidden 变成 1。
     */

    await loadScoreDetail()


    /*
     * 刷新总成绩
     */

    await refreshCurrentTotal()


    /*
     * 刷新学生列表综合评分
     */

    await loadStudents()

  }
  catch (error) {

    /*
     * 用户点击取消
     */

    if (
      error !== 'cancel' &&
      error !== 'close'
    ) {

      console.error(
        '隐藏成绩失败：',
        error,
      )

      ElMessage.error(
        '隐藏成绩失败',
      )

    }

  }

}


/* =========================================================
 * 恢复成绩
 * ========================================================= */

async function handleShow(row) {

  try {

    await ElMessageBox.confirm(
      '确定恢复这条成绩吗？',
      '提示',
      {
        type: 'warning',
      },
    )


    /*
     * 调用管理员恢复接口
     */

    await showScore(
      row.id,
    )


    ElMessage.success(
      '恢复成功',
    )


    /*
     * 重新加载成绩
     */

    await loadScoreDetail()


    /*
     * 刷新总成绩
     */

    await refreshCurrentTotal()


    /*
     * 刷新学生列表综合评分
     */

    await loadStudents()

  }
  catch (error) {

    /*
     * 用户点击取消
     */

    if (
      error !== 'cancel' &&
      error !== 'close'
    ) {

      console.error(
        '恢复成绩失败：',
        error,
      )

      ElMessage.error(
        '恢复成绩失败',
      )

    }

  }

}


/* =========================================================
 * 管理员重置学生密码
 * ========================================================= */

async function handleResetPassword(row) {

  try {

    await ElMessageBox.confirm(
      `确定将「${row.realName || row.studentNo || '该学生'}」的密码重置为 123456 吗？\n\n重置后，该学生下次登录时必须重新修改密码。`,
      '重置学生密码',
      {
        type: 'warning',
        confirmButtonText: '确定重置',
        cancelButtonText: '取消',
      },
    )

    const res = await resetStudentPassword(row.id)

    const result = unwrapResult(res)

    if (res?.code === 200 || res?.data?.code === 200) {
      ElMessage.success('密码已重置为 123456，学生下次登录需重新修改密码')
    } else {
      ElMessage.error(
        res?.message
        || res?.data?.message
        || result?.message
        || '重置密码失败',
      )
    }

  } catch (error) {

    if (error !== 'cancel' && error !== 'close') {
      console.error('重置学生密码失败：', error)
      ElMessage.error(
        error?.response?.data?.message
        || error?.message
        || '重置密码失败',
      )
    }

  }

}


/* =========================================================
 * 初始化
 * ========================================================= */

onMounted(() => {

  loadStudents()

})

</script>


<style scoped>

.score-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}


/* =========================================================
 * 页面标题
 * ========================================================= */

.page-header {
  margin-bottom: 18px;
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


/* =========================================================
 * 搜索
 * ========================================================= */

.search-card {
  margin-bottom: 18px;
}


/* =========================================================
 * 表格
 * ========================================================= */

.table-card {
  margin-bottom: 20px;
}


/* =========================================================
 * 综合评分
 * ========================================================= */

.score {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
}


/* =========================================================
 * 学生信息
 * ========================================================= */

.student-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  font-size: 15px;
}

.student-info span {
  color: #909399;
}


/* =========================================================
 * 综合评分
 * ========================================================= */

.total-score {
  grid-column: 1 / -1;
  font-size: 18px;
}

.total-score strong {
  color: #409eff;
  font-size: 24px;
}


/* =========================================================
 * 加分
 * ========================================================= */

.score-plus {
  color: #67c23a;
  font-weight: bold;
}


/* =========================================================
 * 扣分
 * ========================================================= */

.score-minus {
  color: #f56c6c;
  font-weight: bold;
}


/* =========================================================
 * 分页
 * ========================================================= */

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

</style>
