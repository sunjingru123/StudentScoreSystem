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
   获取学生列表
========================================================= */

async function load() {

  loading.value = true


  try {

    console.log(
      '===================================='
    )

    console.log(
      '开始获取学生成绩'
    )

    console.log(
      '当前页：',
      pageNum.value
    )

    console.log(
      '每页：',
      pageSize.value
    )

    console.log(
      '搜索关键词：',
      searchKeyword.value
    )


    /* =====================================================
       第一步
       获取分页学生
    ===================================================== */

    const params = {

      pageNum: pageNum.value,

      pageSize: pageSize.value

    }


    /*
     * 关键：
     *
     * 搜索框输入的内容，
     * 同时去匹配：
     *
     * 学号
     * 姓名
     * 账号
     * 班级
     *
     * 后端目前 student/list
     * 是分字段接收参数的，
     * 所以这里需要把关键词分别传给四个字段。
     */

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


    /* =====================================================
       第二步
       解析后端分页
    ===================================================== */

    const responseData =
      studentRes?.data


    const pageData =
      responseData?.data ??
      responseData


    console.log(
      '分页数据：',
      pageData
    )


    if (
      !pageData ||
      !Array.isArray(
        pageData.records
      )
    ) {

      console.error(
        '后端没有返回正确分页结构：',
        studentRes
      )


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


    console.log(
      '当前页学生：',
      students
    )


    console.log(
      '学生总数：',
      total.value
    )


    /* =====================================================
       第三步
       获取当前页学生成绩
    ===================================================== */

    const rows = []


    /*
     * 注意：
     *
     * 这里现在只查询当前页的学生成绩。
     *
     * 比如：
     *
     * 第1页 10人
     *
     * 就只请求这10人的成绩。
     *
     * 翻到第2页，
     * 再请求第2页10人的成绩。
     *
     * 不会一次请求全部几百个学生。
     */

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


      /* =================================================
         学生基本信息
      ================================================== */

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


      /* =================================================
         查询综合成绩
      ================================================== */

      try {

        const scoreRes =
          await request.get(
            `/scoreStatistics/${student.id}`
          )


        console.log(
          `学生 ${student.id} 成绩：`,
          scoreRes
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


        /*
         * 没成绩也不能影响学生显示
         */

        row.totalScore = 0

      }


      rows.push(row)

    }


    /* =====================================================
       最终数据
    ===================================================== */

    list.value = rows


    console.log(
      '最终成绩列表：',
      list.value
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

  /*
   * 搜索必须从第一页开始。
   */

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


  /*
   * 改变每页数量以后，
   * 从第一页开始。
   */

  pageNum.value = 1


  load()

}


/* =========================================================
   查看详情
========================================================= */

function viewDetail(row) {

  console.log(
    '查看学生详情：',
    row
  )


  /*
   * 这里暂时不跳转学生端。
   *
   * 后面可以直接弹出
   * 该学生的完整成绩。
   */

  ElMessage.info(
    `${row.studentName} 的综合评分为 ${row.totalScore} 分`
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
