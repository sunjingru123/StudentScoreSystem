<template>
  <div class="score-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div>
        <h2>成绩管理</h2>
        <p>查看和管理学生综合测评成绩</p>
      </div>
    </div>

    <!-- 搜索 -->
    <el-card shadow="never" class="search-card">
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
          <el-button type="primary" @click="handleSearch">
            搜索
          </el-button>

          <el-button @click="handleReset">
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 学生列表 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="students"
        border
        stripe
        v-loading="loading"
        row-key="id"
      >
        <el-table-column
          type="index"
          label="#"
          width="60"
          :index="indexMethod"
        />

        <el-table-column
          prop="studentNo"
          label="学号"
          width="150"
        />

        <el-table-column
          prop="realName"
          label="姓名"
          width="120"
        />

        <el-table-column
          prop="className"
          label="班级"
        />

        <el-table-column
          label="综合评分"
          width="130"
        >
          <template #default="{ row }">
            <span class="score">
              {{ row.totalScore ?? 0 }}
            </span>
          </template>
        </el-table-column>

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

      <!-- 空数据 -->
      <el-empty
        v-if="!loading && students.length === 0"
        description="暂无学生数据"
      />

      <!-- 分页 -->
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
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <!-- 成绩详情 -->
    <el-drawer
      v-model="drawerVisible"
      title="学生成绩明细"
      size="720px"
    >
      <div
        v-if="currentStudent"
        class="student-info"
      >
        <div>
          <span>姓名：</span>

          <strong>
            {{ currentStudent.realName }}
          </strong>
        </div>

        <div>
          <span>学号：</span>

          <strong>
            {{ currentStudent.studentNo }}
          </strong>
        </div>

        <div>
          <span>班级：</span>

          <strong>
            {{ currentStudent.className }}
          </strong>
        </div>

        <div class="total-score">
          综合评分：

          <strong>
            {{ currentTotal }}
          </strong>
        </div>
      </div>

      <el-divider />

      <el-table
        :data="scoreDetails"
        border
        stripe
        v-loading="detailLoading"
      >
        <el-table-column
          prop="ruleName"
          label="评分项目"
          min-width="150"
        />

        <el-table-column
          label="分数"
          width="100"
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
                Number(row.score) >= 0 ? '+' : ''
              }}

              {{ row.score }}
            </span>
          </template>
        </el-table-column>

        <el-table-column
          label="来源"
          width="100"
        >
          <template #default="{ row }">
            {{ formatSource(row.sourceType) }}
          </template>
        </el-table-column>

        <el-table-column
          label="状态"
          width="100"
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

        <el-table-column
          label="操作"
          width="110"
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

const loading = ref(false)

const students = ref([])

const total = ref(0)

const pageNum = ref(1)

const pageSize = ref(10)

const keyword = ref('')


/*
 * =========================================================
 * 成绩详情
 * =========================================================
 */

const detailLoading = ref(false)

const drawerVisible = ref(false)

const currentStudent = ref(null)

const scoreDetails = ref([])

const currentTotal = ref(0)


/*
 * =========================================================
 * 加载学生列表
 *
 * 重点：
 *
 * 这里绝对不能再使用：
 *
 * /user/student/all
 *
 * 必须使用：
 *
 * /user/student/list
 *
 * 并且把 page 和 pageSize 传给后端。
 * =========================================================
 */

async function loadStudents() {
  loading.value = true

  try {
    const params = {
      page: pageNum.value,
      pageSize: pageSize.value,
    }


    /*
     * 如果有搜索关键字，
     * 后端分别搜索学号、姓名、班级。
     *
     * 这里统一使用 keyword。
     *
     * 后端没有 keyword 参数，
     * 所以三个字段都传。
     */

    const key = keyword.value.trim()

    if (key) {
      params.studentNo = key
      params.realName = key
      params.className = key
    }


    /*
     * =====================================================
     * 真正调用分页接口
     * =====================================================
     */

    const res = await request.get(
      '/user/student/list',
      {
        params,
      },
    )


    console.log(
      '成绩管理学生分页接口返回：',
      res,
    )


    /*
     * =====================================================
     * 兼容你的 Result<Page<StudentVO>>
     *
     * 后端实际结构一般是：
     *
     * {
     *   code: 200,
     *   message: "...",
     *   data: {
     *     records: [],
     *     total: 100,
     *     current: 1,
     *     size: 10
     *   }
     * }
     * =====================================================
     */

    const pageData =
      res.data?.data ||
      res.data ||
      {}


    /*
     * 学生列表
     */

    const list =
      Array.isArray(pageData.records)
        ? pageData.records
        : []


    /*
     * 总学生数量
     */

    const totalCount =
      Number(pageData.total ?? 0)


    students.value = list

    total.value = totalCount


    /*
     * =====================================================
     * 给当前页学生加载综合成绩
     *
     * 注意：
     *
     * 这里只给“当前页”的学生请求成绩。
     *
     * 不再把全部学生加载进前端。
     * =====================================================
     */

    await loadCurrentPageTotals()

  } catch (error) {

    console.error(
      '学生分页加载失败：',
      error,
    )

    ElMessage.error(
      '学生列表加载失败',
    )

  } finally {

    loading.value = false
  }
}


/*
 * =========================================================
 * 加载当前页综合成绩
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
   * 并行请求当前页学生成绩
   */

  await Promise.all(
    students.value.map(
      async (student) => {

        try {

          const res =
            await getAdminStudentTotal(
              student.id,
            )


          student.totalScore =
            res.data?.data ??
            res.data ??
            0

        } catch (error) {

          console.error(
            '获取学生总成绩失败：',
            student.id,
            error,
          )

          student.totalScore = 0
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
   * 搜索必须从第一页开始
   */

  pageNum.value = 1

  loadStudents()
}


/*
 * =========================================================
 * 重置
 * =========================================================
 */

function handleReset() {

  keyword.value = ''

  pageNum.value = 1

  pageSize.value = 10

  loadStudents()
}


/*
 * =========================================================
 * 页码变化
 *
 * 关键！
 *
 * 点击第2页：
 *
 * pageNum = 2
 *
 * 然后重新请求：
 *
 * /user/student/list?page=2&pageSize=10
 *
 * =========================================================
 */

function handlePageChange(page) {

  pageNum.value = page

  loadStudents()
}


/*
 * =========================================================
 * 每页数量变化
 * =========================================================
 */

function handleSizeChange(size) {

  pageSize.value = size

  /*
   * 修改每页数量后重新从第一页开始
   */

  pageNum.value = 1

  loadStudents()
}


/*
 * =========================================================
 * 表格序号
 *
 * 第1页：
 * 1 2 3 ...
 *
 * 第2页：
 * 11 12 13 ...
 *
 * =========================================================
 */

function indexMethod(index) {

  return (
    (pageNum.value - 1) *
    pageSize.value +
    index +
    1
  )
}


/*
 * =========================================================
 * 查看成绩
 * =========================================================
 */

async function openDetail(student) {

  currentStudent.value = student

  drawerVisible.value = true

  detailLoading.value = true

  scoreDetails.value = []

  currentTotal.value =
    student.totalScore ?? 0


  try {

    /*
     * 注意：
     *
     * 成绩详情接口必须传当前学生 ID。
     */

    const [
      detailRes,
      totalRes,
    ] = await Promise.all([
      getAdminStudentScores(
        student.id,
      ),

      getAdminStudentTotal(
        student.id,
      ),
    ])


    scoreDetails.value =
      detailRes.data?.data ||
      detailRes.data ||
      []


    currentTotal.value =
      totalRes.data?.data ??
      totalRes.data ??
      0

  } catch (error) {

    console.error(
      '成绩加载失败：',
      error,
    )

    ElMessage.error(
      '成绩加载失败',
    )

  } finally {

    detailLoading.value = false
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
        type: 'warning',
      },
    )


    /*
     * 隐藏当前成绩记录
     */

    await hideScore(
      row.id,
    )


    /*
     * 当前学生 ID
     */

    const studentId =
      currentStudent.value?.id


    if (!studentId) {
      return
    }


    /*
     * 重新加载当前学生成绩
     */

    await refreshCurrentStudentScore(
      studentId,
    )


    /*
     * 重新加载当前页学生
     */

    await loadStudents()


    ElMessage.success(
      '成绩已隐藏',
    )

  } catch (error) {

    /*
     * 用户点击取消
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
        type: 'info',
      },
    )


    await showScore(
      row.id,
    )


    const studentId =
      currentStudent.value?.id


    if (!studentId) {
      return
    }


    await refreshCurrentStudentScore(
      studentId,
    )


    await loadStudents()


    ElMessage.success(
      '成绩已恢复',
    )

  } catch (error) {

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
  ] = await Promise.all([
    getAdminStudentScores(
      studentId,
    ),

    getAdminStudentTotal(
      studentId,
    ),
  ])


  scoreDetails.value =
    detailRes.data?.data ||
    detailRes.data ||
    []


  currentTotal.value =
    totalRes.data?.data ??
    totalRes.data ??
    0


  /*
   * 更新抽屉里的当前学生
   */

  if (
    currentStudent.value &&
    currentStudent.value.id ===
    studentId
  ) {

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
.score-page {
  padding: 24px;
}

/* 页面标题 */

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

/* 搜索 */

.search-card {
  margin-bottom: 18px;
}

/* 表格 */

.table-card {
  border-radius: 8px;
}

/* 分页 */

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 20px 0 4px;
}

/* 总分 */

.score {
  font-size: 18px;
  font-weight: 600;
  color: #409eff;
}

/* 学生信息 */

.student-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
  padding: 5px 0;
}

.student-info span {
  color: #909399;
}

.total-score {
  grid-column: 1 / 3;
  padding: 14px;
  background: #f0f9ff;
  border-radius: 8px;
  color: #606266;
}

.total-score strong {
  color: #409eff;
  font-size: 25px;
  margin-left: 8px;
}

/* 分数 */

.score-plus {
  color: #67c23a;
  font-weight: 600;
}

.score-minus {
  color: #f56c6c;
  font-weight: 600;
}
</style>
