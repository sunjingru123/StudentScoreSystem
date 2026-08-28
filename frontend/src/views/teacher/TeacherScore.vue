<template>
  <div class="page">

    <!-- =========================
         页面标题
    ========================== -->
    <el-card class="title-card">

      <div class="header">

        <div>

          <h2>
            学生综合测评
          </h2>

          <p>
            查看所负责学生的综合测评成绩
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

    </el-card>


    <!-- =========================
         成绩列表
    ========================== -->
    <el-card class="table-card">

      <template #header>

        <div class="table-header">

          <div class="table-title">
            <span>
              学生成绩
            </span>

            <span class="count">
              共 {{ filteredList.length }} 名学生
            </span>
          </div>


          <!-- =====================
               搜索
          ====================== -->

          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索姓名 / 学号 / 班级"
            style="width: 280px"
            @clear="handleSearch"
          >

            <template #prefix>

              <el-icon>
                <Search />
              </el-icon>

            </template>

          </el-input>

        </div>

      </template>


      <!-- =========================
           成绩表格
      ========================== -->

      <el-table
        v-loading="loading"
        :data="pagedList"
        border
        stripe
        style="width: 100%"
      >

        <!-- =====================
             学生
        ====================== -->

        <el-table-column
          label="学生"
          min-width="180"
        >

          <template #default="scope">

            <div class="student-cell">

              <el-avatar :size="40">

                {{
                  (
                    scope.row.studentName ||
                    '学'
                  ).substring(0, 1)
                }}

              </el-avatar>


              <div class="student-info">

                <strong>
                  {{ scope.row.studentName || '未知学生' }}
                </strong>

                <span>
                  {{ scope.row.studentNo || '暂无学号' }}
                </span>

              </div>

            </div>

          </template>

        </el-table-column>


        <!-- =====================
             学号
        ====================== -->

        <el-table-column
          prop="studentNo"
          label="学号"
          width="160"
          align="center"
        />


        <!-- =====================
             班级
        ====================== -->

        <el-table-column
          prop="className"
          label="班级"
          min-width="160"
        >

          <template #default="scope">

            {{ scope.row.className || '未填写' }}

          </template>

        </el-table-column>


        <!-- =====================
             综合评分
        ====================== -->

        <el-table-column
          prop="totalScore"
          label="综合评分"
          width="140"
          align="center"
        >

          <template #default="scope">

            <span class="score">

              {{ scope.row.totalScore ?? 0 }}

            </span>

            <span class="score-unit">
              分
            </span>

          </template>

        </el-table-column>


        <!-- =====================
             操作
        ====================== -->

        <el-table-column
          label="操作"
          width="130"
          fixed="right"
          align="center"
        >

          <template #default="scope">

            <el-button
              type="primary"
              link
              @click="viewDetail(scope.row)"
            >

              查看详情

            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- =========================
           空数据
      ========================== -->

      <el-empty
        v-if="
          !loading &&
          filteredList.length === 0
        "
        description="暂无成绩数据"
      />


      <!-- =========================
           分页
      ========================== -->

      <div
        v-if="
          !loading &&
          filteredList.length > 0
        "
        class="pagination-box"
      >

        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredList.length"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />

      </div>

    </el-card>


    <!-- =========================
         学生成绩详情弹窗
    ========================== -->

    <el-dialog
      v-model="detailDialogVisible"
      title="学生成绩详情"
      width="850px"
      destroy-on-close
    >

      <div
        v-if="currentStudent"
        class="detail-dialog"
      >

        <!-- =====================
             学生信息
        ====================== -->

        <div class="detail-header">

          <div class="detail-user">

            <el-avatar :size="60">

              {{
                (
                  currentStudent.studentName ||
                  '学'
                ).substring(0, 1)
              }}

            </el-avatar>


            <div class="detail-user-info">

              <strong>
                {{
                  currentStudent.studentName ||
                  '未知学生'
                }}
              </strong>

              <span>
                学号：
                {{
                  currentStudent.studentNo ||
                  '暂无'
                }}
              </span>

              <span>
                班级：
                {{
                  currentStudent.className ||
                  '未填写'
                }}
              </span>

            </div>

          </div>


          <!-- 综合评分 -->

          <div class="detail-total">

            <span>
              综合评分
            </span>

            <strong>
              {{ detailScore.totalScore ?? currentStudent.totalScore ?? 0 }}
            </strong>

            <small>
              分
            </small>

          </div>

        </div>


        <el-divider />


        <!-- =====================
             成绩统计
        ====================== -->

        <div class="score-statistics">

          <div class="score-stat-item">

            <span>
              综合评分
            </span>

            <strong>
              {{ detailScore.totalScore ?? 0 }}
            </strong>

          </div>


          <div class="score-stat-item">

            <span>
              平均分
            </span>

            <strong>
              {{ detailScore.avgScore ?? 0 }}
            </strong>

          </div>


          <div class="score-stat-item">

            <span>
              最高分
            </span>

            <strong>
              {{ detailScore.maxScore ?? 0 }}
            </strong>

          </div>


          <div class="score-stat-item">

            <span>
              最低分
            </span>

            <strong>
              {{ detailScore.minScore ?? 0 }}
            </strong>

          </div>

        </div>


        <el-divider />


        <!-- =====================
             成绩明细
        ====================== -->

        <div class="detail-title">

          <strong>
            成绩明细
          </strong>

          <span>
            共 {{ detailList.length }} 条记录
          </span>

        </div>


        <el-table
          v-loading="detailLoading"
          :data="detailList"
          border
          stripe
          style="width: 100%"
        >

          <!-- 项目 -->

          <el-table-column
            prop="ruleName"
            label="项目"
            min-width="180"
          >

            <template #default="scope">

              {{
                scope.row.ruleName ||
                scope.row.title ||
                scope.row.name ||
                '未命名项目'
              }}

            </template>

          </el-table-column>


          <!-- 分数 -->

          <el-table-column
            prop="score"
            label="分数"
            width="100"
            align="center"
          >

            <template #default="scope">

              <span
                :class="
                  Number(scope.row.score) < 0
                    ? 'negative-score'
                    : 'detail-score'
                "
              >

                {{
                  Number(scope.row.score) > 0
                    ? '+' + scope.row.score
                    : scope.row.score ?? 0
                }}

              </span>

            </template>

          </el-table-column>


          <!-- 来源 -->

          <el-table-column
            prop="sourceType"
            label="来源"
            width="130"
            align="center"
          >

            <template #default="scope">

              {{ formatSource(scope.row.sourceType) }}

            </template>

          </el-table-column>


          <!-- 时间 -->

          <el-table-column
            prop="createTime"
            label="时间"
            min-width="170"
          >

            <template #default="scope">

              {{ scope.row.createTime || '—' }}

            </template>

          </el-table-column>

        </el-table>


        <!-- =====================
             没有明细
        ====================== -->

        <el-empty
          v-if="
            !detailLoading &&
            detailList.length === 0
          "
          description="暂无成绩明细"
        />

      </div>


      <template #footer>

        <el-button
          @click="detailDialogVisible = false"
        >
          关闭
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
  ElMessage
} from 'element-plus'


import {
  Search
} from '@element-plus/icons-vue'


import request from '@/utils/request'


/* =========================================================
   学生数据
========================================================= */

const list = ref([])

const loading = ref(false)


/* =========================================================
   搜索
========================================================= */

const keyword = ref('')


/* =========================================================
   分页
========================================================= */

const currentPage = ref(1)

const pageSize = ref(10)


/* =========================================================
   成绩详情
========================================================= */

const detailDialogVisible = ref(false)

const detailLoading = ref(false)

const currentStudent = ref(null)


const detailScore = ref({

  studentName: '',

  totalScore: 0,

  avgScore: 0,

  maxScore: 0,

  minScore: 0,

  detail: []

})


/* =========================================================
   成绩明细
========================================================= */

const detailList = computed(() => {

  const detail =
    detailScore.value?.detail

  return Array.isArray(detail)
    ? detail
    : []

})


/* =========================================================
   解析学生列表
========================================================= */

function parseStudentList(res) {

  console.log(
    '===================================='
  )

  console.log(
    '学生列表完整响应：',
    res
  )

  console.log(
    'res.data：',
    res?.data
  )

  console.log(
    'res.data.data：',
    res?.data?.data
  )


  const responseData =
    res?.data


  /*
   * 情况一：
   *
   * axios 返回：
   *
   * {
   *   data: [...]
   * }
   */

  if (
    Array.isArray(responseData)
  ) {

    return responseData

  }


  /*
   * 情况二：
   *
   * {
   *   data: {
   *     records: []
   *   }
   * }
   */

  const data =
    responseData?.data


  if (
    Array.isArray(data)
  ) {

    return data

  }


  if (
    data &&
    Array.isArray(data.records)
  ) {

    return data.records

  }


  if (
    data &&
    Array.isArray(data.list)
  ) {

    return data.list

  }


  /*
   * 情况三：
   *
   * responseData.records
   */

  if (
    responseData &&
    Array.isArray(
      responseData.records
    )
  ) {

    return responseData.records

  }


  /*
   * 情况四：
   *
   * responseData.list
   */

  if (
    responseData &&
    Array.isArray(
      responseData.list
    )
  ) {

    return responseData.list

  }


  return null

}


/* =========================================================
   获取学生成绩列表
========================================================= */

async function load() {

  loading.value = true


  try {

    console.log(
      '===================================='
    )

    console.log(
      '开始获取辅导员学生成绩'
    )


    /* =====================================================
       第一步
       获取学生列表
    ===================================================== */

    const studentRes =
      await request.get(
        '/user/student/list'
      )


    const students =
      parseStudentList(
        studentRes
      )


    console.log(
      '解析后的学生数组：',
      students
    )


    /*
     * 检查数据格式
     */

    if (
      !Array.isArray(students)
    ) {

      console.error(
        '学生列表数据格式异常：',
        studentRes
      )

      list.value = []

      ElMessage.error(
        '学生列表数据格式异常，请检查后端 /user/student/list 返回值'
      )

      return

    }


    /* =====================================================
       第二步
       获取每个学生的综合评分
    ===================================================== */

    const rows = []


    /*
     * 使用 Promise.all
     *
     * 比一个一个 await 快很多
     */

    const results =
      await Promise.all(

        students.map(
          async student => {

            if (
              !student ||
              !student.id
            ) {

              return null

            }


            /*
             * 学生基本信息
             */

            const row = {

              id:
              student.id,

              studentName:
                student.realName ??
                student.studentName ??
                student.name ??
                '未知学生',

              studentNo:
                student.studentNo ??
                student.studentNumber ??
                student.username ??
                '',

              className:
                student.className ??
                student.classNameName ??
                '',

              totalScore: 0

            }


            /*
             * 查询成绩
             */

            try {

              const scoreRes =
                await request.get(
                  `/scoreStatistics/${student.id}`
                )


              console.log(
                `学生 ${student.id} 成绩响应：`,
                scoreRes
              )


              const scoreData =
                scoreRes?.data?.data ??
                scoreRes?.data


              if (
                scoreData &&
                typeof scoreData === 'object'
              ) {

                row.totalScore =
                  scoreData.totalScore ??
                  scoreData.score ??
                  0

              }

            }

            catch (error) {

              console.warn(
                `学生 ${student.id} 成绩获取失败：`,
                error
              )


              /*
               * 成绩获取失败
               * 仍然显示这个学生
               */

              row.totalScore = 0

            }


            return row

          }
        )

      )


    /*
     * 删除无效数据
     */

    for (
      const row of results
      ) {

      if (row) {

        rows.push(row)

      }

    }


    /* =====================================================
       最终数据
    ===================================================== */

    list.value = rows


    /*
     * 刷新后回到第一页
     */

    currentPage.value = 1


    console.log(
      '===================================='
    )

    console.log(
      '最终学生成绩列表：',
      list.value
    )

    console.log(
      '学生数量：',
      list.value.length
    )

    console.log(
      '===================================='
    )

  }

  catch (error) {

    console.error(
      '获取学生成绩失败：',
      error
    )


    list.value = []


    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取学生成绩失败，请检查后端服务'
    )

  }

  finally {

    loading.value = false

  }

}


/* =========================================================
   搜索过滤
========================================================= */

const filteredList =
  computed(() => {

    const key =
      keyword.value
        .trim()
        .toLowerCase()


    /*
     * 没有搜索条件
     */

    if (!key) {

      return list.value

    }


    /*
     * 根据：
     *
     * 姓名
     * 学号
     * 班级
     *
     * 搜索
     */

    return list.value.filter(
      student => {

        const studentName =
          String(
            student.studentName || ''
          ).toLowerCase()


        const studentNo =
          String(
            student.studentNo || ''
          ).toLowerCase()


        const className =
          String(
            student.className || ''
          ).toLowerCase()


        return (

          studentName.includes(key) ||

          studentNo.includes(key) ||

          className.includes(key)

        )

      }
    )

  })


/* =========================================================
   当前页数据
========================================================= */

const pagedList =
  computed(() => {

    const start =
      (
        currentPage.value - 1
      ) * pageSize.value


    const end =
      start + pageSize.value


    return filteredList.value.slice(
      start,
      end
    )

  })


/* =========================================================
   搜索
========================================================= */

function handleSearch() {

  /*
   * 搜索以后
   * 自动回到第一页
   */

  currentPage.value = 1

}


/* =========================================================
   每页数量改变
========================================================= */

function handleSizeChange(size) {

  pageSize.value = size

  currentPage.value = 1

}


/* =========================================================
   当前页改变
========================================================= */

function handleCurrentChange(page) {

  currentPage.value = page

}


/* =========================================================
   查看学生详情
========================================================= */

async function viewDetail(row) {

  console.log(
    '查看学生成绩详情：',
    row
  )


  /*
   * 保存当前学生
   */

  currentStudent.value = row


  /*
   * 打开弹窗
   */

  detailDialogVisible.value = true


  /*
   * 开始加载
   */

  detailLoading.value = true


  /*
   * 先显示当前列表里的综合评分
   */

  detailScore.value = {

    studentName:
      row.studentName || '',

    totalScore:
      row.totalScore ?? 0,

    avgScore: 0,

    maxScore: 0,

    minScore: 0,

    detail: []

  }


  try {

    /*
     * 再请求一次完整成绩
     */

    const res =
      await request.get(
        `/scoreStatistics/${row.id}`
      )


    console.log(
      '学生完整成绩：',
      res
    )


    const data =
      res?.data?.data ??
      res?.data


    if (
      data &&
      typeof data === 'object'
    ) {

      detailScore.value = {

        studentName:
          data.studentName ??
          row.studentName ??
          '',

        totalScore:
          data.totalScore ??
          data.score ??
          row.totalScore ??
          0,

        avgScore:
          data.avgScore ??
          data.averageScore ??
          0,

        maxScore:
          data.maxScore ??
          0,

        minScore:
          data.minScore ??
          0,

        detail:
          Array.isArray(data.detail)
            ? data.detail
            : []

      }

    }

  }

  catch (error) {

    console.error(
      '获取学生详细成绩失败：',
      error
    )


    ElMessage.error(
      '获取学生详细成绩失败'
    )

  }

  finally {

    detailLoading.value = false

  }

}


/* =========================================================
   成绩来源转换
========================================================= */

function formatSource(sourceType) {

  if (!sourceType) {

    return '未知'

  }


  const source =
    String(sourceType)
      .toUpperCase()


  const map = {

    DEPARTMENT:
      '部门加减分',

    COURSE:
      '课程成绩',

    ACTIVITY:
      '活动',

    SYSTEM:
      '系统',

    MANUAL:
      '人工录入'

  }


  return (
    map[source] ||
    sourceType
  )

}


/* =========================================================
   初始化
========================================================= */

onMounted(() => {

  load()

})

</script>


<style scoped>

.page {

  width: 100%;

}


/* =========================================================
   标题
========================================================= */

.title-card {

  margin-bottom: 20px;

}


.header {

  display: flex;

  justify-content: space-between;

  align-items: center;

}


.header h2 {

  margin:
    0 0 8px;

  font-size:
    24px;

  color:
    #303133;

}


.header p {

  margin: 0;

  color:
    #909399;

  font-size:
    14px;

}


/* =========================================================
   表格
========================================================= */

.table-card {

  border: none;

}


.table-header {

  display: flex;

  align-items: center;

  justify-content: space-between;

  gap: 20px;

}


.table-title {

  display: flex;

  align-items: center;

  gap: 15px;

  font-size: 17px;

  font-weight: bold;

}


.count {

  color:
    #909399;

  font-size:
    13px;

  font-weight:
    normal;

}


/* =========================================================
   学生
========================================================= */

.student-cell {

  display: flex;

  align-items: center;

  gap: 12px;

}


.student-info {

  display: flex;

  flex-direction: column;

  gap: 4px;

}


.student-info strong {

  color:
    #303133;

  font-size:
    15px;

}


.student-info span {

  color:
    #909399;

  font-size:
    13px;

}


/* =========================================================
   成绩
========================================================= */

.score {

  font-size:
    18px;

  font-weight:
    bold;

  color:
    #409eff;

}


.score-unit {

  margin-left: 4px;

  color:
    #909399;

  font-size:
    13px;

}


/* =========================================================
   分页
========================================================= */

.pagination-box {

  display: flex;

  justify-content: flex-end;

  align-items: center;

  margin-top: 20px;

  padding: 10px 0;

}


/* =========================================================
   详情弹窗
========================================================= */

.detail-dialog {

  min-height: 300px;

}


/* =========================================================
   详情头部
========================================================= */

.detail-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

}


.detail-user {

  display: flex;

  align-items: center;

  gap: 15px;

}


.detail-user-info {

  display: flex;

  flex-direction: column;

  gap: 6px;

}


.detail-user-info strong {

  color:
    #303133;

  font-size:
    21px;

}


.detail-user-info span {

  color:
    #909399;

  font-size:
    13px;

}


/* =========================================================
   综合评分
========================================================= */

.detail-total {

  min-width: 150px;

  text-align: center;

}


.detail-total span {

  display: block;

  color:
    #909399;

  font-size:
    13px;

  margin-bottom: 4px;

}


.detail-total strong {

  color:
    #409eff;

  font-size:
    38px;

}


.detail-total small {

  margin-left: 4px;

  color:
    #909399;

}


/* =========================================================
   成绩统计
========================================================= */

.score-statistics {

  display: grid;

  grid-template-columns:
    repeat(4, 1fr);

  gap: 15px;

}


.score-stat-item {

  padding: 16px;

  background:
    #f5f7fa;

  border-radius:
    8px;

  text-align:
    center;

}


.score-stat-item span {

  display: block;

  margin-bottom: 8px;

  color:
    #909399;

  font-size:
    13px;

}


.score-stat-item strong {

  color:
    #303133;

  font-size:
    22px;

}


/* =========================================================
   明细标题
========================================================= */

.detail-title {

  display: flex;

  align-items: center;

  justify-content: space-between;

  margin-bottom: 15px;

}


.detail-title strong {

  color:
    #303133;

  font-size:
    16px;

}


.detail-title span {

  color:
    #909399;

  font-size:
    13px;

}


/* =========================================================
   明细成绩
========================================================= */

.detail-score {

  color:
    #409eff;

  font-weight:
    bold;

}


.negative-score {

  color:
    #f56c6c;

  font-weight:
    bold;

}


/* =========================================================
   响应式
========================================================= */

@media (max-width: 800px) {

  .table-header {

    flex-direction:
      column;

    align-items:
      stretch;

  }


  .table-title {

    justify-content:
      space-between;

  }


  .score-statistics {

    grid-template-columns:
      repeat(2, 1fr);

  }


  .detail-header {

    flex-direction:
      column;

    align-items:
      flex-start;

    gap: 20px;

  }


  .detail-total {

    text-align:
      left;

  }


  .pagination-box {

    justify-content:
      center;

  }

}


@media (max-width: 600px) {

  .header {

    align-items:
      flex-start;

    gap:
      15px;

  }


  .score-statistics {

    grid-template-columns:
      1fr;

  }

}

</style>
