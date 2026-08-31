<template>
  <div class="score-page">

    <!-- ====================================================== -->
    <!-- 页面标题 -->
    <!-- ====================================================== -->

    <div class="page-header">

      <div>

        <h2>
          成绩管理
        </h2>

        <p>
          查看和管理学生综合测评成绩
        </p>

      </div>

    </div>



    <!-- ====================================================== -->
    <!-- 搜索 -->
    <!-- ====================================================== -->

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



    <!-- ====================================================== -->
    <!-- 学生列表 -->
    <!-- ====================================================== -->

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

        <!-- ================================================== -->
        <!-- 序号 -->
        <!-- ================================================== -->

        <el-table-column
          type="index"
          label="#"
          width="60"
          :index="indexMethod"
        />


        <!-- ================================================== -->
        <!-- 学号 -->
        <!-- ================================================== -->

        <el-table-column
          prop="studentNo"
          label="学号"
          width="150"
        />


        <!-- ================================================== -->
        <!-- 姓名 -->
        <!-- ================================================== -->

        <el-table-column
          prop="realName"
          label="姓名"
          width="120"
        />


        <!-- ================================================== -->
        <!-- 班级 -->
        <!-- ================================================== -->

        <el-table-column
          prop="className"
          label="班级"
          min-width="160"
        />


        <!-- ================================================== -->
        <!-- 状态 -->
        <!-- ================================================== -->

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



        <!-- ================================================== -->
        <!-- 综合评分 -->
        <!-- ================================================== -->

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



        <!-- ================================================== -->
        <!-- 操作 -->
        <!-- ================================================== -->

        <el-table-column
          label="操作"
          width="150"
          fixed="right"
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



      <!-- ================================================== -->
      <!-- 空数据 -->
      <!-- ================================================== -->

      <el-empty
        v-if="
          !loading &&
          students.length === 0
        "
        description="暂无学生数据"
      />



      <!-- ================================================== -->
      <!-- 分页 -->
      <!-- ================================================== -->

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



    <!-- ====================================================== -->
    <!-- 成绩详情 -->
    <!-- ====================================================== -->

    <el-drawer
      v-model="drawerVisible"
      title="学生成绩明细"
      size="720px"
    >

      <!-- ================================================== -->
      <!-- 学生基本信息 -->
      <!-- ================================================== -->

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


        <!-- ================================================= -->
        <!-- 综合评分 -->
        <!-- ================================================= -->

        <div class="total-score">

          综合评分：

          <strong>
            {{ formatScore(currentTotal) }}
          </strong>

        </div>

      </div>



      <el-divider />



      <!-- ================================================== -->
      <!-- 成绩详情 -->
      <!-- ================================================== -->

      <el-table
        :data="scoreDetails"
        border
        stripe
        v-loading="detailLoading"
      >

        <!-- ================================================= -->
        <!-- 评分项目 -->
        <!-- ================================================= -->

        <el-table-column
          prop="ruleName"
          label="评分项目"
          min-width="150"
        />



        <!-- ================================================= -->
        <!-- 分数 -->
        <!-- ================================================= -->

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



        <!-- ================================================= -->
        <!-- 来源 -->
        <!-- ================================================= -->

        <el-table-column
          label="来源"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            {{ formatSource(row.sourceType) }}

          </template>

        </el-table-column>



        <!-- ================================================= -->
        <!-- 状态 -->
        <!-- ================================================= -->

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



        <!-- ================================================= -->
        <!-- 操作 -->
        <!-- ================================================= -->

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



/*
 * =========================================================
 * 学生分页
 * =========================================================
 */

const loading =
  ref(false)

const students =
  ref([])

const total =
  ref(0)

/*
 * 这里必须叫 pageNum
 *
 * 因为后端：
 *
 * @RequestParam(defaultValue = "1") long pageNum
 *
 * 前端不能再传 page。
 */

const pageNum =
  ref(1)

const pageSize =
  ref(10)

const keyword =
  ref('')



/*
 * =========================================================
 * 成绩详情
 * =========================================================
 */

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



/*
 * =========================================================
 * 加载学生列表
 * =========================================================
 *
 * 后端接口：
 *
 * GET /user/student/list
 *
 * 后端参数：
 *
 * pageNum
 * pageSize
 * keyword
 * studentNo
 * realName
 * className
 * status
 *
 * =========================================================
 */

async function loadStudents() {

  loading.value = true


  try {

    /*
     * =====================================================
     * 这里是本次最重要的修改
     * =====================================================
     *
     * 原来：
     *
     * page: pageNum.value
     *
     * 错误！
     *
     * 后端接收的是：
     *
     * pageNum
     *
     * =====================================================
     */

    const params = {

      pageNum:
      pageNum.value,

      pageSize:
      pageSize.value,

    }



    /*
     * =====================================================
     * 搜索
     * =====================================================
     */

    const key =
      keyword.value.trim()


    if (key) {

      /*
       * 后端本身已经支持 keyword
       *
       * keyword 会同时搜索：
       *
       * 学号
       * 姓名
       * 用户名
       * 班级
       *
       * 所以这里只传 keyword 即可。
       */

      params.keyword =
        key

    }



    /*
     * =====================================================
     * 请求后端
     * =====================================================
     */

    console.log(
      '正在请求学生分页：',
      params,
    )


    const res =
      await request.get(
        '/user/student/list',
        {
          params,
        },
      )


    console.log(
      '学生分页接口返回：',
      res,
    )



    /*
     * =====================================================
     * 获取后端 Result
     * =====================================================
     *
     * 后端：
     *
     * return Result.success(voPage)
     *
     * 所以正常结构：
     *
     * res.data.data
     *
     * =====================================================
     */

    const pageData =
      res.data?.data ??
      {}



    /*
     * =====================================================
     * 学生列表
     * =====================================================
     */

    const records =
      Array.isArray(
        pageData.records
      )
        ? pageData.records
        : []



    /*
     * =====================================================
     * 总数量
     * =====================================================
     */

    const totalCount =
      Number(
        pageData.total ?? 0
      )



    /*
     * =====================================================
     * 更新页面
     * =====================================================
     */

    students.value =
      records

    total.value =
      totalCount



    console.log(
      '当前页：',
      pageNum.value,
    )

    console.log(
      '每页数量：',
      pageSize.value,
    )

    console.log(
      '当前页学生数量：',
      students.value.length,
    )

    console.log(
      '学生总数量：',
      total.value,
    )



    /*
     * =====================================================
     * 查询当前页学生的综合评分
     * =====================================================
     */

    await loadCurrentPageTotals()

  }

  catch (error) {

    console.error(
      '学生分页加载失败：',
      error,
    )


    ElMessage.error(
      '学生列表加载失败',
    )

  }

  finally {

    loading.value =
      false

  }

}



/*
 * =========================================================
 * 加载当前页综合评分
 * =========================================================
 */

async function loadCurrentPageTotals() {

  if (
    !students.value ||
    students.value.length === 0
  ) {

    return

  }



  /*
   * 当前页学生并行查询
   */

  await Promise.all(

    students.value.map(

      async (student) => {

        try {

          const res =
            await getAdminStudentTotal(
              student.id,
            )



          /*
           * 后端：
           *
           * Result<ScoreStatisticsVO>
           *
           */

          const statistics =
            res.data?.data ??
            res.data ??
            {}



          /*
           * 综合评分
           */

          student.totalScore =
            Number(
              statistics.totalScore ?? 0
            )



          /*
           * 保存完整统计
           */

          student.scoreStatistics =
            statistics



          console.log(
            `学生 ${student.realName} 综合评分：`,
            statistics,
          )

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



/*
 * =========================================================
 * 搜索
 * =========================================================
 */

function handleSearch() {

  /*
   * 搜索以后必须回到第一页
   */

  pageNum.value =
    1


  loadStudents()

}



/*
 * =========================================================
 * 重置
 * =========================================================
 */

function handleReset() {

  keyword.value =
    ''

  pageNum.value =
    1

  pageSize.value =
    10


  loadStudents()

}



/*
 * =========================================================
 * 页码变化
 * =========================================================
 */

function handlePageChange(page) {

  /*
   * Element Plus 返回当前页
   *
   * 例如：
   *
   * 第一页 -> 1
   * 第二页 -> 2
   * 第三页 -> 3
   */

  pageNum.value =
    page


  console.log(
    '切换到第',
    pageNum.value,
    '页',
  )


  /*
   * 重新请求后端
   */

  loadStudents()

}



/*
 * =========================================================
 * 每页数量变化
 * =========================================================
 */

function handleSizeChange(size) {

  /*
   * 更新每页数量
   */

  pageSize.value =
    size


  /*
   * 改变每页数量后
   * 回到第一页
   */

  pageNum.value =
    1


  console.log(
    '每页数量修改为：',
    pageSize.value,
  )


  loadStudents()

}



/*
 * =========================================================
 * 表格序号
 * =========================================================
 */

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



/*
 * =========================================================
 * 格式化分数
 * =========================================================
 */

function formatScore(score) {

  const value =
    Number(score)


  if (
    Number.isNaN(value)
  ) {

    return '0'

  }


  /*
   * 整数不显示小数
   *
   * 例如：
   *
   * 5 -> 5
   *
   * 5.5 -> 5.5
   */

  if (
    Number.isInteger(value)
  ) {

    return String(value)

  }


  return value.toFixed(2)

}



/*
 * =========================================================
 * 查看成绩
 * =========================================================
 */

async function openDetail(student) {

  currentStudent.value =
    student

  drawerVisible.value =
    true

  detailLoading.value =
    true

  scoreDetails.value =
    []



  /*
   * 先使用列表中的综合评分
   */

  currentTotal.value =
    Number(
      student.totalScore ?? 0
    )



  try {

    /*
     * =====================================================
     * 同时获取：
     *
     * 1. 成绩明细
     * 2. 综合评分
     * =====================================================
     */

    const [
      detailRes,
      totalRes,
    ] =
      await Promise.all([

        getAdminStudentScores(
          student.id,
        ),

        getAdminStudentTotal(
          student.id,
        ),

      ])



    /*
     * =====================================================
     * 成绩明细
     * =====================================================
     */

    scoreDetails.value =
      detailRes.data?.data ??
      detailRes.data ??
      []



    /*
     * =====================================================
     * 综合评分
     * =====================================================
     */

    const statistics =
      totalRes.data?.data ??
      totalRes.data ??
      {}



    currentTotal.value =
      Number(
        statistics.totalScore ?? 0
      )



    /*
     * 保存统计数据
     */

    currentStudent.value.scoreStatistics =
      statistics



    /*
     * 同步更新列表分数
     */

    currentStudent.value.totalScore =
      currentTotal.value

  }

  catch (error) {

    console.error(
      '成绩加载失败：',
      error,
    )


    ElMessage.error(
      '成绩加载失败',
    )

  }

  finally {

    detailLoading.value =
      false

  }

}



/*
 * =========================================================
 * 隐藏成绩
 * =========================================================
 */

async function handleHide(row) {

  try {

    await ElMessageBox.confirm(

      '隐藏后，学生和辅导员将无法看到这条成绩明细，确定继续吗？',

      '隐藏成绩',

      {
        type:
          'warning',
      },

    )



    /*
     * 隐藏
     */

    await hideScore(
      row.id,
    )



    /*
     * 当前学生
     */

    const studentId =
      currentStudent.value?.id


    if (!studentId) {

      return

    }



    /*
     * 刷新抽屉里的成绩
     */

    await refreshCurrentStudentScore(
      studentId,
    )



    /*
     * 刷新列表
     */

    await loadStudents()



    ElMessage.success(
      '成绩已隐藏',
    )

  }

  catch (error) {

    /*
     * 用户取消
     */

    if (
      error === 'cancel'
    ) {

      return

    }


    console.error(
      '隐藏成绩失败：',
      error,
    )


    ElMessage.error(
      '隐藏失败',
    )

  }

}



/*
 * =========================================================
 * 恢复成绩
 * =========================================================
 */

async function handleShow(row) {

  try {

    await ElMessageBox.confirm(

      '恢复后，学生和辅导员将重新看到这条成绩明细，确定继续吗？',

      '恢复成绩',

      {
        type:
          'info',
      },

    )



    /*
     * 恢复
     */

    await showScore(
      row.id,
    )



    /*
     * 当前学生
     */

    const studentId =
      currentStudent.value?.id


    if (!studentId) {

      return

    }



    /*
     * 刷新当前学生成绩
     */

    await refreshCurrentStudentScore(
      studentId,
    )



    /*
     * 刷新列表
     */

    await loadStudents()



    ElMessage.success(
      '成绩已恢复',
    )

  }

  catch (error) {

    if (
      error === 'cancel'
    ) {

      return

    }


    console.error(
      '恢复成绩失败：',
      error,
    )


    ElMessage.error(
      '恢复失败',
    )

  }

}



/*
 * =========================================================
 * 刷新当前学生成绩
 * =========================================================
 */

async function refreshCurrentStudentScore(
  studentId,
) {

  const [
    detailRes,
    totalRes,
  ] =
    await Promise.all([

      getAdminStudentScores(
        studentId,
      ),

      getAdminStudentTotal(
        studentId,
      ),

    ])



  /*
   * 成绩明细
   */

  scoreDetails.value =
    detailRes.data?.data ??
    detailRes.data ??
    []



  /*
   * 综合评分
   */

  const statistics =
    totalRes.data?.data ??
    totalRes.data ??
    {}



  currentTotal.value =
    Number(
      statistics.totalScore ?? 0
    )



  /*
   * 更新当前学生
   */

  if (
    currentStudent.value
  ) {

    currentStudent.value.scoreStatistics =
      statistics

    currentStudent.value.totalScore =
      currentTotal.value

  }

}



/*
 * =========================================================
 * 来源名称
 * =========================================================
 */

function formatSource(
  sourceType,
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


  return sourceType || '-'

}



/*
 * =========================================================
 * 初始化
 * =========================================================
 */

onMounted(() => {

  loadStudents()

})

</script>



<style scoped>

/* ====================================================== */
/* 页面 */
/* ====================================================== */

.score-page {

  padding:
    24px;

}



/* ====================================================== */
/* 页面标题 */
/* ====================================================== */

.page-header {

  margin-bottom:
    20px;

}



.page-header h2 {

  margin:
    0;

  font-size:
    24px;

  color:
    #303133;

}



.page-header p {

  margin:
    8px 0 0;

  color:
    #909399;

  font-size:
    14px;

}



/* ====================================================== */
/* 搜索 */
/* ====================================================== */

.search-card {

  margin-bottom:
    18px;

}



/* ====================================================== */
/* 表格 */
/* ====================================================== */

.table-card {

  border-radius:
    8px;

}



/* ====================================================== */
/* 分页 */
/* ====================================================== */

.pagination-wrapper {

  display:
    flex;

  justify-content:
    flex-end;

  align-items:
    center;

  padding:
    20px 0 4px;

}



/* ====================================================== */
/* 综合评分 */
/* ====================================================== */

.score {

  font-size:
    18px;

  font-weight:
    600;

  color:
    #409eff;

}



/* ====================================================== */
/* 学生信息 */
/* ====================================================== */

.student-info {

  display:
    grid;

  grid-template-columns:
    1fr 1fr;

  gap:
    15px;

  padding:
    5px 0;

}



.student-info span {

  color:
    #909399;

}



/* ====================================================== */
/* 综合评分 */
/* ====================================================== */

.total-score {

  grid-column:
    1 / 3;

  padding:
    14px;

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



/* ====================================================== */
/* 加分 */
/* ====================================================== */

.score-plus {

  color:
    #67c23a;

  font-weight:
    600;

}



/* ====================================================== */
/* 减分 */
/* ====================================================== */

.score-minus {

  color:
    #f56c6c;

  font-weight:
    600;

}



/* ====================================================== */
/* 移动端 */
/* ====================================================== */

@media (max-width: 768px) {

  .score-page {

    padding:
      12px;

  }


  .student-info {

    grid-template-columns:
      1fr;

  }


  .total-score {

    grid-column:
      auto;

  }


  .pagination-wrapper {

    justify-content:
      center;

  }

}

</style>
