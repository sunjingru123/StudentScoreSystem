<template>
  <div class="score-page">

    <!-- =====================================================
         页面标题
    ====================================================== -->
    <div class="page-header">

      <div>
        <h2>成绩管理</h2>

        <p>
          查看和管理学生综合测评成绩
        </p>
      </div>

    </div>


    <!-- =====================================================
         搜索
    ====================================================== -->
    <el-card
      shadow="never"
      class="search-card"
    >

      <el-form :inline="true">

        <el-form-item label="搜索">

          <el-input
            v-model="keyword"
            placeholder="学号 / 姓名 / 班级"
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
    ====================================================== -->
    <el-card
      shadow="never"
      class="table-card"
    >

      <el-table
        :data="students"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
      >

        <!-- 序号 -->
        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
          :index="studentIndexMethod"
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
          min-width="180"
        />


        <!-- 综合评分 -->
        <el-table-column
          label="综合评分"
          width="130"
          align="center"
        >

          <template #default="{ row }">

            <span class="score">

              {{ formatScore(row.totalScore) }}

            </span>

          </template>

        </el-table-column>


        <!-- 操作 -->
        <el-table-column
          label="操作"
          width="150"
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

          </template>

        </el-table-column>

      </el-table>


      <!-- =====================================================
           学生列表分页
      ====================================================== -->
      <div
        v-if="!loading && studentTotal > 0"
        class="pagination-wrapper"
      >

        <el-pagination
          v-model:current-page="studentPage"
          v-model:page-size="studentPageSize"

          :page-sizes="[10, 20, 30, 50]"

          :total="studentTotal"

          layout="total, sizes, prev, pager, next, jumper"

          background

          @size-change="handleStudentSizeChange"

          @current-change="handleStudentCurrentChange"
        />

      </div>


      <!-- =====================================================
           空数据
      ====================================================== -->
      <el-empty
        v-if="!loading && students.length === 0"
        description="暂无学生数据"
      />

    </el-card>


    <!-- =====================================================
         成绩详情
    ====================================================== -->
    <el-drawer
      v-model="drawerVisible"
      title="学生成绩明细"
      size="720px"
      destroy-on-close
    >

      <!-- ===================================================
           学生信息
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
            {{ currentStudent.realName }}
          </strong>

        </div>


        <div>

          <span>
            学号：
          </span>

          <strong>
            {{ currentStudent.studentNo }}
          </strong>

        </div>


        <div>

          <span>
            班级：
          </span>

          <strong>
            {{ currentStudent.className }}
          </strong>

        </div>


        <div class="total-score">

          综合评分：

          <strong>
            {{ formatScore(currentTotal) }}
          </strong>

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
        style="width: 100%"
      >

        <!-- 序号 -->
        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
          :index="scoreIndexMethod"
        />


        <!-- 评分项目 -->
        <el-table-column
          prop="ruleName"
          label="评分项目"
          min-width="150"
          show-overflow-tooltip
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
                Number(row.score) > 0
                  ? '+' + formatScore(row.score)
                  : formatScore(row.score)
              }}

            </span>

          </template>

        </el-table-column>


        <!-- 来源 -->
        <el-table-column
          label="来源"
          width="100"
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

            <el-button
              v-if="Number(row.adminHidden) === 0"
              type="danger"
              link
              @click="handleHide(row)"
            >
              隐藏
            </el-button>


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
           成绩明细分页
      =================================================== -->
      <div
        v-if="!detailLoading && scoreTotal > 0"
        class="detail-pagination-wrapper"
      >

        <el-pagination
          v-model:current-page="scorePage"
          v-model:page-size="scorePageSize"

          :page-sizes="[5, 10, 20, 30]"

          :total="scoreTotal"

          layout="total, sizes, prev, pager, next, jumper"

          background

          @size-change="handleScoreSizeChange"

          @current-change="handleScoreCurrentChange"
        />

      </div>

    </el-drawer>

  </div>
</template>


<script setup>

import {
  ref,
  onMounted
} from 'vue'

import {
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  getAdminStudentScores,
  getAdminStudentTotal,
  hideScore,
  showScore
} from '@/api/adminScore'

import request from '@/utils/request'


/* =========================================================
 * 基础状态
 * ========================================================= */

const loading = ref(false)

const detailLoading = ref(false)


/* =========================================================
 * 搜索
 * ========================================================= */

const keyword = ref('')


/* =========================================================
 * 学生列表
 * ========================================================= */

const students = ref([])


/* =========================================================
 * 学生分页
 * ========================================================= */

const studentPage = ref(1)

const studentPageSize = ref(10)

const studentTotal = ref(0)


/* =========================================================
 * 成绩抽屉
 * ========================================================= */

const drawerVisible = ref(false)

const currentStudent = ref(null)


/* =========================================================
 * 成绩明细
 * ========================================================= */

const scoreDetails = ref([])

const currentTotal = ref(0)


/* =========================================================
 * 成绩明细分页
 * ========================================================= */

const scorePage = ref(1)

const scorePageSize = ref(10)

const scoreTotal = ref(0)


/* =========================================================
 * 学生列表序号
 * ========================================================= */

function studentIndexMethod(index) {

  return (
    (studentPage.value - 1)
    *
    studentPageSize.value
    +
    index
    +
    1
  )

}


/* =========================================================
 * 成绩明细序号
 * ========================================================= */

function scoreIndexMethod(index) {

  return (
    (scorePage.value - 1)
    *
    scorePageSize.value
    +
    index
    +
    1
  )

}


/* =========================================================
 * 加载学生列表
 *
 * 真正的后端分页
 * ========================================================= */

async function loadStudents() {

  loading.value = true

  try {

    /*
     * =====================================================
     * 重点：
     *
     * pageNum
     * pageSize
     *
     * 交给后端分页
     * =====================================================
     */

    const res =
      await request.get(
        '/user/student/list',
        {
          params: {

            pageNum:
            studentPage.value,

            pageSize:
            studentPageSize.value,

            keyword:
              keyword.value.trim() || undefined

          }

        }
      )


    console.log(
      '学生列表原始返回：',
      res
    )


    const data =
      res.data?.data


    let records = []

    let total = 0


    /*
     * =====================================================
     * MyBatis-Plus 分页格式
     *
     * {
     *   records: [],
     *   total: 100,
     *   pages: 10,
     *   current: 1,
     *   size: 10
     * }
     * =====================================================
     */

    if (
      data
      &&
      Array.isArray(data.records)
    ) {

      records =
        data.records

      total =
        Number(
          data.total || 0
        )

    }


    /*
     * =====================================================
     * 兼容旧接口直接返回数组
     * =====================================================
     */

    else if (
      Array.isArray(data)
    ) {

      records =
        data

      /*
       * 旧接口没有 total，
       * 只能按照当前数组数量处理。
       */

      total =
        data.length

    }


    /*
     * =====================================================
     * 兼容 res.data 直接数组
     * =====================================================
     */

    else if (
      Array.isArray(res.data)
    ) {

      records =
        res.data

      total =
        res.data.length

    }


    /*
     * =====================================================
     * 数据异常
     * =====================================================
     */

    if (!Array.isArray(records)) {

      throw new Error(
        '学生列表数据格式异常'
      )

    }


    /*
     * =====================================================
     * 先显示学生基本信息
     * =====================================================
     */

    students.value =
      records.map(
        student => ({

          ...student,

          totalScore:
            0

        })
      )


    studentTotal.value =
      total


    /*
     * =====================================================
     * 并行加载当前页学生综合成绩
     * =====================================================
     */

    await loadCurrentPageScores()

  } catch (error) {

    console.error(
      '学生列表加载失败：',
      error
    )

    students.value =
      []

    studentTotal.value =
      0

    ElMessage.error(
      error.response?.data?.msg
      ||
      error.response?.data?.message
      ||
      error.message
      ||
      '学生列表加载失败'
    )

  } finally {

    loading.value = false

  }

}


/* =========================================================
 * 加载当前页学生综合成绩
 * ========================================================= */

async function loadCurrentPageScores() {

  const promises =
    students.value.map(
      async student => {

        try {

          const res =
            await getAdminStudentTotal(
              student.id
            )


          const data =
            res.data?.data


          /*
           * 兼容：
           *
           * { totalScore: 85 }
           *
           * 或
           *
           * 85
           */

          if (
            data
            &&
            typeof data === 'object'
          ) {

            student.totalScore =
              data.totalScore
              ??
              data.score
              ??
              0

          } else {

            student.totalScore =
              data
              ??
              res.data
              ??
              0

          }

        } catch (error) {

          console.error(
            `获取学生 ${student.id} 成绩失败：`,
            error
          )

          student.totalScore =
            0

        }

      }
    )


  await Promise.all(
    promises
  )

}


/* =========================================================
 * 学生分页：修改每页数量
 * ========================================================= */

async function handleStudentSizeChange(size) {

  studentPageSize.value =
    size

  studentPage.value =
    1

  await loadStudents()

}


/* =========================================================
 * 学生分页：切换页码
 * ========================================================= */

async function handleStudentCurrentChange(page) {

  studentPage.value =
    page

  await loadStudents()

}


/* =========================================================
 * 搜索
 * ========================================================= */

async function handleSearch() {

  studentPage.value =
    1

  await loadStudents()

}


/* =========================================================
 * 重置
 * ========================================================= */

async function handleReset() {

  keyword.value =
    ''

  studentPage.value =
    1

  await loadStudents()

}


/* =========================================================
 * 打开成绩详情
 * ========================================================= */

async function openDetail(student) {

  currentStudent.value =
    student

  drawerVisible.value =
    true


  /*
   * 每次打开学生成绩，
   * 默认第一页
   */

  scorePage.value =
    1

  scorePageSize.value =
    10

  scoreTotal.value =
    0

  scoreDetails.value =
    []

  currentTotal.value =
    student.totalScore
    ??
    0


  await loadScoreDetail()

}


/* =========================================================
 * 加载成绩明细
 * ========================================================= */

async function loadScoreDetail() {

  if (
    !currentStudent.value
    ||
    !currentStudent.value.id
  ) {

    return

  }


  detailLoading.value =
    true


  try {

    /*
     * =====================================================
     * 这里优先使用你前面已经使用过的分页接口。
     *
     * 如果 getAdminStudentScores 内部目前还没有分页参数，
     * 后面只需要改 api/adminScore.js。
     * =====================================================
     */

    const res =
      await getAdminStudentScores(
        currentStudent.value.id,
        {
          pageNum:
          scorePage.value,

          pageSize:
          scorePageSize.value
        }
      )


    console.log(
      '成绩明细原始返回：',
      res
    )


    const data =
      res.data?.data


    /*
     * =====================================================
     * MyBatis-Plus 分页
     * =====================================================
     */

    if (
      data
      &&
      Array.isArray(data.records)
    ) {

      scoreDetails.value =
        data.records

      scoreTotal.value =
        Number(
          data.total || 0
        )

    }


    /*
     * =====================================================
     * 旧接口直接数组
     * =====================================================
     */

    else if (
      Array.isArray(data)
    ) {

      scoreDetails.value =
        data

      scoreTotal.value =
        data.length

    }


    else {

      scoreDetails.value =
        []

      scoreTotal.value =
        0

    }


    /*
     * =====================================================
     * 同时刷新当前学生总成绩
     * =====================================================
     */

    await refreshCurrentTotal()

  } catch (error) {

    console.error(
      '成绩加载失败：',
      error
    )

    scoreDetails.value =
      []

    scoreTotal.value =
      0

    ElMessage.error(
      error.response?.data?.msg
      ||
      error.response?.data?.message
      ||
      '成绩加载失败'
    )

  } finally {

    detailLoading.value =
      false

  }

}


/* =========================================================
 * 刷新当前学生总成绩
 * ========================================================= */

async function refreshCurrentTotal() {

  if (
    !currentStudent.value
  ) {

    return

  }


  try {

    const res =
      await getAdminStudentTotal(
        currentStudent.value.id
      )


    const data =
      res.data?.data


    let total =
      0


    if (
      data
      &&
      typeof data === 'object'
    ) {

      total =
        data.totalScore
        ??
        data.score
        ??
        0

    } else {

      total =
        data
        ??
        res.data
        ??
        0

    }


    currentTotal.value =
      total


    currentStudent.value.totalScore =
      total


    /*
     * 同步学生列表中的总成绩
     */

    const student =
      students.value.find(
        item =>
          Number(item.id)
          ===
          Number(
            currentStudent.value.id
          )
      )


    if (student) {

      student.totalScore =
        total

    }

  } catch (error) {

    console.error(
      '刷新综合成绩失败：',
      error
    )

  }

}


/* =========================================================
 * 成绩分页：修改每页数量
 * ========================================================= */

async function handleScoreSizeChange(size) {

  scorePageSize.value =
    size

  scorePage.value =
    1

  await loadScoreDetail()

}


/* =========================================================
 * 成绩分页：切换页码
 * ========================================================= */

async function handleScoreCurrentChange(page) {

  scorePage.value =
    page

  await loadScoreDetail()

}


/* =========================================================
 * 隐藏成绩
 * ========================================================= */

async function handleHide(row) {

  try {

    await ElMessageBox.confirm(

      '隐藏后，学生和辅导员将无法看到这条成绩明细，确定继续吗？',

      '隐藏成绩',

      {
        type: 'warning'
      }

    )


    await hideScore(
      row.id
    )


    ElMessage.success(
      '成绩已隐藏'
    )


    /*
     * 如果当前页最后一条被隐藏，
     * 可能导致当前页没有数据。
     */

    await loadScoreDetail()


    /*
     * 刷新学生列表中的总成绩
     */

    await refreshCurrentTotal()


    /*
     * 如果当前页超过实际页数，
     * 自动回到上一页
     */

    if (
      scoreDetails.value.length === 0
      &&
      scorePage.value > 1
    ) {

      scorePage.value =
        scorePage.value - 1

      await loadScoreDetail()

    }

  } catch (error) {

    if (
      error === 'cancel'
      ||
      error === 'close'
    ) {

      return

    }


    console.error(
      '隐藏成绩失败：',
      error
    )

    ElMessage.error(
      '隐藏失败'
    )

  }

}


/* =========================================================
 * 恢复成绩
 * ========================================================= */

async function handleShow(row) {

  try {

    await ElMessageBox.confirm(

      '恢复后，学生和辅导员将重新看到这条成绩明细，确定继续吗？',

      '恢复成绩',

      {
        type: 'info'
      }

    )


    await showScore(
      row.id
    )


    ElMessage.success(
      '成绩已恢复'
    )


    await loadScoreDetail()


    await refreshCurrentTotal()

  } catch (error) {

    if (
      error === 'cancel'
      ||
      error === 'close'
    ) {

      return

    }


    console.error(
      '恢复成绩失败：',
      error
    )

    ElMessage.error(
      '恢复失败'
    )

  }

}


/* =========================================================
 * 来源名称
 * ========================================================= */

function formatSource(
  sourceType
) {

  if (
    sourceType === 'apply'
  ) {

    return '自主申请'

  }


  if (
    sourceType === 'system'
  ) {

    return '系统'

  }


  if (
    sourceType === 'admin'
  ) {

    return '管理员'

  }


  return (
    sourceType
    ||
    '-'
  )

}


/* =========================================================
 * 分数格式化
 * ========================================================= */

function formatScore(score) {

  if (
    score === null
    ||
    score === undefined
    ||
    score === ''
  ) {

    return '0'

  }


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

  min-height:
    calc(100vh - 60px);

}


/* =========================================================
 * 页面标题
 * ========================================================= */

.page-header {

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

  border-radius: 8px;

}


/* =========================================================
 * 学生综合评分
 * ========================================================= */

.score {

  font-size: 18px;

  font-weight: 600;

  color: #409eff;

}


/* =========================================================
 * 学生分页
 * ========================================================= */

.pagination-wrapper {

  display: flex;

  justify-content: flex-end;

  align-items: center;

  margin-top: 20px;

  padding: 5px 0;

}


/* =========================================================
 * 学生信息
 * ========================================================= */

.student-info {

  display: grid;

  grid-template-columns:
    1fr 1fr;

  gap: 15px;

  padding: 5px 0;

}


.student-info span {

  color: #909399;

}


.total-score {

  grid-column:
    1 / 3;

  padding: 14px;

  background:
    #f0f9ff;

  border-radius:
    8px;

  color:
    #606266;

}


.total-score strong {

  color:
    #409eff;

  font-size:
    25px;

  margin-left:
    8px;

}


/* =========================================================
 * 成绩分页
 * ========================================================= */

.detail-pagination-wrapper {

  display: flex;

  justify-content:
    flex-end;

  align-items:
    center;

  margin-top:
    20px;

  padding:
    5px 0;

}


/* =========================================================
 * 加分
 * ========================================================= */

.score-plus {

  color:
    #67c23a;

  font-weight:
    600;

}


/* =========================================================
 * 减分
 * ========================================================= */

.score-minus {

  color:
    #f56c6c;

  font-weight:
    600;

}

</style>
