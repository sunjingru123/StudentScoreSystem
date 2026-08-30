<template>
  <div class="page">

    <!-- =========================
         页面标题
    ========================== -->
    <el-card class="title-card">

      <div class="header">

        <div>
          <h2>学生综合测评</h2>

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

          <div class="left">

            <span class="title">
              学生成绩
            </span>

            <span class="count">
              共 {{ total }} 名学生
            </span>

          </div>

        </div>


        <!-- =====================
             搜索栏
        ====================== -->

        <div class="search-bar">

          <el-input
            v-model="searchKeyword"
            placeholder="搜索学号、姓名、账号或班级"
            clearable
            style="width: 350px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >

            <template #prefix>

              <el-icon>
                <Search />
              </el-icon>

            </template>

          </el-input>


          <el-button
            type="primary"
            @click="handleSearch"
          >
            搜索
          </el-button>


          <el-button
            @click="resetSearch"
          >
            重置
          </el-button>

        </div>

      </template>


      <!-- =========================
           学生成绩表格
      ========================== -->

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        style="width: 100%"
      >

        <!-- 学生 -->

        <el-table-column
          label="学生"
          min-width="180"
        >

          <template #default="scope">

            <div class="student-cell">

              <el-avatar :size="38">

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
                  {{ scope.row.username || '' }}
                </span>

              </div>

            </div>

          </template>

        </el-table-column>


        <!-- 学号 -->

        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="150"
        />


        <!-- 班级 -->

        <el-table-column
          prop="className"
          label="班级"
          min-width="160"
        />


        <!-- 综合评分 -->

        <el-table-column
          prop="totalScore"
          label="综合评分"
          min-width="120"
          align="center"
        >

          <template #default="scope">

            <span class="score">

              {{ scope.row.totalScore ?? 0 }}

            </span>

          </template>

        </el-table-column>


        <!-- 操作 -->

        <el-table-column
          label="操作"
          width="120"
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
          list.length === 0
        "
        description="暂无成绩数据"
      />


      <!-- =========================
           分页
      ========================== -->

      <div
        v-if="total > 0"
        class="pagination"
      >

        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />

      </div>

    </el-card>


    <!-- =====================================================
         学生流水详情弹窗
    ====================================================== -->

    <el-dialog
      v-model="detailVisible"
      :title="`${detailStudent.studentName || '学生'} 的加减分流水`"
      width="800px"
      destroy-on-close
    >

      <!-- =========================
           学生基本信息
      ========================== -->

      <div class="detail-student">

        <el-avatar :size="48">

          {{
            (
              detailStudent.studentName ||
              '学'
            ).substring(0, 1)
          }}

        </el-avatar>


        <div class="detail-student-info">

          <div class="detail-name">
            {{ detailStudent.studentName || '未知学生' }}
          </div>

          <div class="detail-meta">

            学号：
            {{ detailStudent.studentNo || '-' }}

            <span class="divider">
              |
            </span>

            班级：
            {{ detailStudent.className || '-' }}

          </div>

        </div>

      </div>


      <!-- =========================
           流水加载
      ========================== -->

      <div
        v-loading="detailLoading"
        class="detail-content"
      >

        <!-- 有流水 -->

        <el-table
          v-if="detailRecords.length > 0"
          :data="detailRecords"
          border
          stripe
          style="width: 100%"
        >

          <!-- =====================
               计分项
          ====================== -->

          <el-table-column
            label="计分项"
            min-width="180"
          >

            <template #default="scope">

              <span>
                {{ formatRuleName(scope.row.ruleName) }}
              </span>

            </template>

          </el-table-column>


          <!-- =====================
               分数
          ====================== -->

          <el-table-column
            label="分数"
            width="100"
            align="center"
          >

            <template #default="scope">

              <span
                :class="
                  Number(scope.row.score) > 0
                    ? 'bonus-score'
                    : 'deduct-score'
                "
              >

                {{
                  formatScore(
                    scope.row.score
                  )
                }}

              </span>

            </template>

          </el-table-column>


          <!-- =====================
               来源
          ====================== -->

          <el-table-column
            label="来源"
            width="120"
            align="center"
          >

            <template #default="scope">

              <el-tag
                size="small"
                type="info"
              >

                {{
                  formatSourceType(
                    scope.row.sourceType
                  )
                }}

              </el-tag>

            </template>

          </el-table-column>


          <!-- =====================
               时间
          ====================== -->

          <el-table-column
            label="时间"
            min-width="180"
          >

            <template #default="scope">

              {{
                formatTime(
                  scope.row.createTime
                )
              }}

            </template>

          </el-table-column>

        </el-table>


        <!-- 没有流水 -->

        <el-empty
          v-else-if="!detailLoading"
          description="暂无公开加减分流水"
        />

      </div>


      <!-- =========================
           弹窗底部
      ========================== -->

      <template #footer>

        <el-button
          @click="detailVisible = false"
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
   分页
========================================================= */

const pageNum = ref(1)

const pageSize = ref(10)

const total = ref(0)


/* =========================================================
   搜索
========================================================= */

const searchKeyword = ref('')


/* =========================================================
   学生详情
========================================================= */

const detailVisible = ref(false)

const detailLoading = ref(false)

const detailRecords = ref([])

const detailStudent = ref({})


/* =========================================================
   获取学生列表
========================================================= */

async function load() {

  loading.value = true

  try {

    const params = {

      pageNum: pageNum.value,

      pageSize: pageSize.value

    }


    /* =========================
       搜索
    ========================== */

    if (
      searchKeyword.value &&
      searchKeyword.value.trim()
    ) {

      params.keyword =
        searchKeyword.value.trim()

    }


    console.log(
      '学生列表请求参数：',
      params
    )


    /* =========================
       获取学生
    ========================== */

    const studentRes =
      await request.get(
        '/user/student/list',
        {
          params
        }
      )


    console.log(
      '学生列表响应：',
      studentRes
    )


    const responseData =
      studentRes?.data


    const pageData =
      responseData?.data ??
      responseData


    if (
      !pageData ||
      !Array.isArray(
        pageData.records
      )
    ) {

      list.value = []

      total.value = 0

      ElMessage.error(
        '学生分页数据格式异常'
      )

      return

    }


    const students =
      pageData.records


    total.value =
      Number(
        pageData.total ?? 0
      )


    /* =========================
       获取当前页成绩
    ========================== */

    const rows = []


    for (
      const student
      of students
      ) {

      if (
        !student ||
        !student.id
      ) {

        continue

      }


      const row = {

        id: student.id,

        studentName:
          student.realName ??
          student.studentName ??
          student.name ??
          '未知学生',

        studentNo:
          student.studentNo ??
          student.studentNumber ??
          '',

        username:
          student.username ??
          '',

        className:
          student.className ??
          student.classNameName ??
          '',

        totalScore: 0

      }


      /* =========================
         查询综合成绩
      ========================== */

      try {

        const scoreRes =
          await request.get(
            `/scoreStatistics/${student.id}`
          )


        const scoreData =
          scoreRes?.data?.data ??
          scoreRes?.data ??
          {}


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

        row.totalScore = 0

      }


      rows.push(row)

    }


    list.value = rows

  }

  catch (error) {

    console.error(
      '获取学生成绩失败：',
      error
    )


    list.value = []

    total.value = 0


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
   搜索
========================================================= */

function handleSearch() {

  pageNum.value = 1

  load()

}


/* =========================================================
   重置搜索
========================================================= */

function resetSearch() {

  searchKeyword.value = ''

  pageNum.value = 1

  load()

}


/* =========================================================
   页码改变
========================================================= */

function handlePageChange(page) {

  pageNum.value = page

  load()

}


/* =========================================================
   每页数量改变
========================================================= */

function handleSizeChange(size) {

  pageSize.value = size

  pageNum.value = 1

  load()

}


/* =========================================================
   查看详情
========================================================= */

async function viewDetail(row) {

  console.log(
    '查看学生公开流水：',
    row
  )


  /* =========================
     打开弹窗
  ========================== */

  detailVisible.value = true


  /* =========================
     保存学生信息
  ========================== */

  detailStudent.value = {

    id: row.id,

    studentName:
    row.studentName,

    studentNo:
    row.studentNo,

    className:
    row.className,

    username:
    row.username

  }


  /* =========================
     清空旧数据
  ========================== */

  detailRecords.value = []

  detailLoading.value = true


  try {

    /* =====================================================
       使用普通公开接口

       /score/student/{studentId}/records

       这里不会返回 adminHidden = 1 的记录。
    ===================================================== */

    const res =
      await request.get(
        `/score/student/${row.id}/records`
      )


    console.log(
      '学生公开流水：',
      res
    )


    const data =
      res?.data?.data ??
      res?.data ??
      []


    if (
      Array.isArray(data)
    ) {

      detailRecords.value =
        data

    }

    else {

      detailRecords.value =
        []

    }

  }

  catch (error) {

    console.error(
      '获取学生流水失败：',
      error
    )


    detailRecords.value = []


    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取学生流水失败'
    )

  }

  finally {

    detailLoading.value = false

  }

}


/* =========================================================
   来源类型中文转换
========================================================= */

/*
 * 以后你要增加、删除、修改名称，
 * 主要就在这里改。
 *
 * 例如：
 *
 * CERTIFICATE: '证书'
 *
 * 如果以后数据库新增：
 *
 * INTERNSHIP: '实习'
 *
 * 就在这里继续增加。
 */

function formatSourceType(type) {

  if (!type) {

    return '-'

  }


  const typeMap = {

    CERTIFICATE: '证书',

    ACTIVITY: '活动',

    COURSE: '课程',

    COMPETITION: '竞赛',

    VOLUNTEER: '志愿服务',

    AWARD: '获奖',

    OTHER: '其他'

  }


  return typeMap[type] || type

}


/* =========================================================
   计分项中文转换
========================================================= */

/*
 * 后端目前返回的 ruleName 可能是：
 *
 * 计分项: CERTIFICATE
 *
 * 所以这里专门处理 ruleName。
 *
 * 注意：
 *
 * 不直接使用 formatSourceType，
 * 避免以后“来源”和“计分项”需要不同中文名称时互相影响。
 */

function formatRuleName(ruleName) {

  if (!ruleName) {

    return '-'

  }


  let name =
    String(ruleName)


  /*
   * 去掉后端暂时拼接的：
   *
   * 计分项:
   */

  name =
    name.replace(
      /^计分项[:：]\s*/,
      ''
    )


  /*
   * 如果后端返回的是：
   *
   * CERTIFICATE
   *
   * 就转换成：
   *
   * 证书
   */

  const ruleMap = {

    CERTIFICATE: '证书',

    ACTIVITY: '活动',

    COURSE: '课程',

    COMPETITION: '竞赛',

    VOLUNTEER: '志愿服务',

    AWARD: '获奖',

    OTHER: '其他'

  }


  return ruleMap[name] || name

}


/* =========================================================
   格式化分数
========================================================= */

function formatScore(score) {

  if (
    score === null ||
    score === undefined
  ) {

    return '0'

  }


  const number =
    Number(score)


  if (
    Number.isNaN(number)
  ) {

    return score

  }


  if (number > 0) {

    return `+${number}`

  }


  return `${number}`

}


/* =========================================================
   格式化时间
========================================================= */

function formatTime(time) {

  if (!time) {

    return '-'

  }


  return String(time)
    .replace('T', ' ')

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
  margin: 0 0 8px;

  font-size: 24px;

  color: #303133;
}


.header p {
  margin: 0;

  color: #909399;

  font-size: 14px;
}


/* =========================================================
   表格卡片
========================================================= */

.table-card {
  border: none;
}


/* =========================================================
   表格头部
========================================================= */

.table-header {
  margin-bottom: 15px;
}


.table-header .left {
  display: flex;

  align-items: center;

  gap: 15px;
}


.table-header .title {
  font-size: 17px;

  font-weight: bold;
}


.count {
  color: #909399;

  font-size: 13px;

  font-weight: normal;
}


/* =========================================================
   搜索
========================================================= */

.search-bar {
  display: flex;

  align-items: center;

  gap: 10px;
}


/* =========================================================
   学生
========================================================= */

.student-cell {
  display: flex;

  align-items: center;

  gap: 10px;
}


.student-info {
  display: flex;

  flex-direction: column;

  gap: 3px;
}


.student-info strong {
  color: #303133;

  font-size: 14px;
}


.student-info span {
  color: #909399;

  font-size: 12px;
}


/* =========================================================
   成绩
========================================================= */

.score {
  font-size: 18px;

  font-weight: bold;

  color: #409eff;
}


/* =========================================================
   分页
========================================================= */

.pagination {
  display: flex;

  justify-content: flex-end;

  margin-top: 20px;

  padding-top: 15px;

  border-top: 1px solid #ebeef5;
}


/* =========================================================
   详情学生
========================================================= */

.detail-student {

  display: flex;

  align-items: center;

  gap: 15px;

  padding: 10px 5px 20px;

  border-bottom: 1px solid #ebeef5;

  margin-bottom: 20px;

}


.detail-student-info {

  display: flex;

  flex-direction: column;

  gap: 6px;

}


.detail-name {

  font-size: 18px;

  font-weight: bold;

  color: #303133;

}


.detail-meta {

  font-size: 13px;

  color: #909399;

}


.divider {

  margin: 0 8px;

  color: #dcdfe6;

}


/* =========================================================
   详情内容
========================================================= */

.detail-content {

  min-height: 180px;

}


/* =========================================================
   加分
========================================================= */

.bonus-score {

  color: #67c23a;

  font-weight: bold;

  font-size: 16px;

}


/* =========================================================
   扣分
========================================================= */

.deduct-score {

  color: #f56c6c;

  font-weight: bold;

  font-size: 16px;

}


/* =========================================================
   响应式
========================================================= */

@media (max-width: 700px) {

  .header {

    align-items: flex-start;

    gap: 15px;

  }


  .search-bar {

    flex-wrap: wrap;

  }


  .pagination {

    justify-content: center;

  }

}

</style>
