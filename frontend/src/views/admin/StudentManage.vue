<template>
  <div class="student-page">

    <!-- =====================================================
         页面标题
    ====================================================== -->
    <div class="page-header">

      <div>
        <h2>学生管理</h2>
        <p>管理学生信息、综合评分及成绩明细</p>
      </div>

      <el-button
        type="primary"
        @click="loadStudentList"
        :loading="loading"
      >
        <el-icon>
          <Refresh />
        </el-icon>
        刷新
      </el-button>

    </div>


    <!-- =====================================================
         搜索区域
    ====================================================== -->
    <el-card
      class="search-card"
      shadow="never"
    >

      <div class="search-row">

        <el-input
          v-model="search.studentNo"
          placeholder="请输入学号"
          clearable
          class="search-item"
          @keyup.enter="handleSearch"
        />

        <el-input
          v-model="search.realName"
          placeholder="请输入姓名"
          clearable
          class="search-item"
          @keyup.enter="handleSearch"
        />

        <el-input
          v-model="search.className"
          placeholder="请输入班级"
          clearable
          class="search-item"
          @keyup.enter="handleSearch"
        />

        <el-select
          v-model="search.status"
          placeholder="账号状态"
          clearable
          class="search-item"
        >

          <el-option
            label="正常"
            :value="1"
          />

          <el-option
            label="禁用"
            :value="0"
          />

        </el-select>


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

    </el-card>


    <!-- =====================================================
         学生列表
    ====================================================== -->
    <el-card
      class="table-card"
      shadow="never"
    >

      <el-table
        v-loading="loading"
        :data="list"
        stripe
        border
        style="width: 100%"
      >

        <!-- =================================================
             序号
        ================================================== -->
        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
          :index="studentIndexMethod"
        />


        <!-- =================================================
             学号
        ================================================== -->
        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="130"
        />


        <!-- =================================================
             姓名
        ================================================== -->
        <el-table-column
          prop="realName"
          label="姓名"
          min-width="100"
        />


        <!-- =================================================
             用户名
        ================================================== -->
        <el-table-column
          prop="username"
          label="用户名"
          min-width="120"
        />


        <!-- =================================================
             班级
        ================================================== -->
        <el-table-column
          prop="className"
          label="班级"
          min-width="150"
        />


        <!-- =================================================
             手机号
        ================================================== -->
        <el-table-column
          prop="phone"
          label="手机号"
          min-width="130"
        />


        <!-- =================================================
             可见加分
        ================================================== -->
        <el-table-column
          label="可见加分"
          min-width="100"
          align="center"
        >

          <template #default="{ row }">

            <span class="bonus-score">
              {{ formatScoreWithPrefix(row.bonusScore, '+') }}
            </span>

          </template>

        </el-table-column>


        <!-- =================================================
             可见减分
        ================================================== -->
        <el-table-column
          label="可见减分"
          min-width="100"
          align="center"
        >

          <template #default="{ row }">

            <span class="deduct-score">
              {{ formatScoreWithPrefix(row.deductScore, '-') }}
            </span>

          </template>

        </el-table-column>


        <!-- =================================================
             当前上限
        ================================================== -->
        <el-table-column
          label="当前上限"
          min-width="100"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="row.actualLimit !== null && row.actualLimit !== undefined"
              type="warning"
            >
              {{ formatScore(row.actualLimit) }} 分
            </el-tag>

            <span v-else>
              ...
            </span>

          </template>

        </el-table-column>


        <!-- =================================================
             最终成绩
        ================================================== -->
        <el-table-column
          label="最终成绩"
          min-width="110"
          align="center"
        >

          <template #default="{ row }">

            <span class="total-score">
              {{ formatScore(row.totalScore) }}
            </span>

          </template>

        </el-table-column>


        <!-- =================================================
             账号状态
        ================================================== -->
        <el-table-column
          label="账号状态"
          width="100"
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


        <!-- =================================================
             操作
        ================================================== -->
        <el-table-column
          label="操作"
          min-width="320"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              size="small"
              type="primary"
              plain
              @click="showDetail(row)"
            >
              查看详情
            </el-button>


            <el-button
              size="small"
              type="success"
              plain
              @click="showScore(row)"
            >
              查看成绩
            </el-button>


            <el-button
              v-if="Number(row.status) === 1"
              size="small"
              type="danger"
              plain
              @click="disableStudent(row)"
            >
              禁用
            </el-button>


            <el-button
              v-else
              size="small"
              type="success"
              plain
              @click="enableStudent(row)"
            >
              启用
            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- =====================================================
           学生列表分页
      ====================================================== -->
      <div
        v-if="!loading && studentTotal > 0"
        class="student-pagination-wrapper"
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


      <!-- =================================================
           空数据
      ================================================== -->
      <el-empty
        v-if="!loading && list.length === 0"
        description="暂无学生数据"
      />

    </el-card>



    <!-- =====================================================
         成绩明细弹窗
    ====================================================== -->
    <el-dialog
      v-model="scoreVisible"
      title="学生成绩明细"
      width="1100px"
      destroy-on-close
    >

      <!-- =================================================
           学生综合成绩
      ================================================== -->
      <div
        v-if="currentStudent"
        class="score-summary"
      >

        <div class="student-name">
          {{ currentStudent.realName }}
        </div>


        <div class="summary-item">

          <span>
            基础上限
          </span>

          <strong>
            {{ formatScore(currentStudent.baseLimit) }} 分
          </strong>

        </div>


        <div class="summary-item">

          <span>
            当前最高上限
          </span>

          <strong>
            {{ formatScore(currentStudent.actualLimit) }} 分
          </strong>

        </div>


        <div class="summary-item">

          <span>
            可见加分
          </span>

          <strong class="bonus-score">
            +{{ formatScore(currentStudent.bonusScore) }}
          </strong>

        </div>


        <div class="summary-item">

          <span>
            可见减分
          </span>

          <strong class="deduct-score">
            -{{ formatScore(currentStudent.deductScore) }}
          </strong>

        </div>


        <div class="summary-item final">

          <span>
            最终成绩
          </span>

          <strong>
            {{ formatScore(currentStudent.totalScore) }} 分
          </strong>

        </div>

      </div>


      <el-divider />


      <!-- =================================================
           成绩明细表
      ================================================== -->
      <el-table
        v-loading="scoreLoading"
        :data="scoreList"
        border
        stripe
        style="width: 100%"
      >

        <!-- =================================================
             成绩序号
        ================================================== -->
        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
          :index="scoreIndexMethod"
        />


        <!-- =================================================
             加分项目
        ================================================== -->
        <el-table-column
          prop="ruleName"
          label="加分项目"
          min-width="180"
          show-overflow-tooltip
        />


        <!-- =================================================
             分值
        ================================================== -->
        <el-table-column
          label="分值"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <span
              :class="
                Number(row.score) >= 0
                  ? 'bonus-score'
                  : 'deduct-score'
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


        <!-- =================================================
             来源
        ================================================== -->
        <el-table-column
          prop="sourceType"
          label="来源"
          width="120"
        />


        <!-- =================================================
             时间
        ================================================== -->
        <el-table-column
          prop="createTime"
          label="时间"
          min-width="170"
        />


        <!-- =================================================
             管理员状态
        ================================================== -->
        <el-table-column
          label="管理员状态"
          width="120"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="Number(row.adminHidden) === 1"
              type="info"
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


        <!-- =================================================
             管理员操作
        ================================================== -->
        <el-table-column
          label="管理员操作"
          width="140"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              v-if="Number(row.adminHidden) === 1"
              size="small"
              type="warning"
              plain
              @click="showHiddenScore(row)"
            >
              恢复
            </el-button>


            <el-button
              v-else
              size="small"
              type="danger"
              plain
              @click="hideScore(row)"
            >
              隐藏
            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <!-- =================================================
           成绩明细分页
      ================================================== -->
      <div
        v-if="scoreTotal > 0"
        class="score-pagination-wrapper"
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


      <!-- =================================================
           成绩为空
      ================================================== -->
      <el-empty
        v-if="!scoreLoading && scoreList.length === 0"
        description="暂无成绩明细"
      />


      <!-- =================================================
           弹窗底部
      ================================================== -->
      <template #footer>

        <el-button
          @click="closeScoreDialog"
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
  ElMessage,
  ElMessageBox
} from 'element-plus'

import {
  Refresh
} from '@element-plus/icons-vue'

import {
  useRouter
} from 'vue-router'

import request from '@/utils/request'


/* =========================================================
 * Router
 * ========================================================= */

const router = useRouter()


/* =========================================================
 * 学生列表
 * ========================================================= */

const list = ref([])

const loading = ref(false)


/* =========================================================
 * 学生列表分页
 * ========================================================= */

const studentPage = ref(1)

const studentPageSize = ref(10)

const studentTotal = ref(0)


/* =========================================================
 * 搜索条件
 * ========================================================= */

const search = ref({

  studentNo: '',

  realName: '',

  className: '',

  status: null

})


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
 * 学生列表：修改每页数量
 * ========================================================= */

async function handleStudentSizeChange(size) {

  studentPageSize.value =
    size

  studentPage.value =
    1

  await loadStudentList()

}


/* =========================================================
 * 学生列表：切换页码
 * ========================================================= */

async function handleStudentCurrentChange(current) {

  studentPage.value =
    current

  await loadStudentList()

}


/* =========================================================
 * 加载学生列表
 *
 * 这里使用真正的后端分页。
 *
 * 后端接口：
 *
 * GET /user/student/list
 *
 * 参数：
 *
 * page
 * pageSize
 * studentNo
 * realName
 * className
 * status
 *
 * ========================================================= */

async function loadStudentList() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/user/student/list',
        {
          params: {

            page:
            studentPage.value,

            pageSize:
            studentPageSize.value,

            studentNo:
              search.value.studentNo || undefined,

            realName:
              search.value.realName || undefined,

            className:
              search.value.className || undefined,

            status:
              search.value.status === null
                ? undefined
                : search.value.status

          }

        }
      )


    console.log(
      '学生分页数据：',
      res
    )


    const data =
      res.data?.data


    /* =====================================================
     * 新接口：
     *
     * MyBatis-Plus Page
     * ===================================================== */

    if (
      data
      &&
      Array.isArray(data.records)
    ) {

      /*
       * 当前页学生
       */

      list.value =
        data.records.map(
          item => ({

            ...item,

            totalScore: null,

            baseLimit: null,

            bonusScore: null,

            deductScore: null,

            actualLimit: null

          })
        )


      /*
       * 总学生数量
       */

      studentTotal.value =
        Number(
          data.total || 0
        )


      /*
       * 同步后端当前页
       */

      if (
        data.current !== undefined
        &&
        data.current !== null
      ) {

        studentPage.value =
          Number(
            data.current
          )

      }


      /*
       * 同步后端每页大小
       */

      if (
        data.size !== undefined
        &&
        data.size !== null
      ) {

        studentPageSize.value =
          Number(
            data.size
          )

      }

    }


    /* =====================================================
     * 兼容旧接口：
     *
     * data = []
     * ===================================================== */

    else if (
      Array.isArray(data)
    ) {

      list.value =
        data.map(
          item => ({

            ...item,

            totalScore: null,

            baseLimit: null,

            bonusScore: null,

            deductScore: null,

            actualLimit: null

          })
        )


      studentTotal.value =
        list.value.length

    }


    /* =====================================================
     * 异常数据
     * ===================================================== */

    else {

      list.value =
        []

      studentTotal.value =
        0

    }


    /*
     * 基础学生信息先显示
     */

    loading.value =
      false


    /*
     * 加载当前页学生综合成绩
     */

    loadAllScoresParallel()

  } catch (error) {

    console.error(
      '获取学生列表失败',
      error
    )

    list.value =
      []

    studentTotal.value =
      0

    loading.value =
      false

    ElMessage.error(
      '获取学生列表失败'
    )

  }

}


/* =========================================================
 * 并行加载学生综合成绩
 *
 * 只加载当前页学生。
 * ========================================================= */

async function loadAllScoresParallel() {

  if (
    !list.value
    ||
    list.value.length === 0
  ) {

    return

  }


  const promises =
    list.value.map(
      async student => {

        try {

          const res =
            await request.get(
              `/scoreStatistics/admin/${student.id}`
            )


          const data =
            res.data?.data


          student.totalScore =
            data?.totalScore ?? 0


          student.baseLimit =
            data?.baseLimit ?? 40


          student.bonusScore =
            data?.bonusScore ?? 0


          student.deductScore =
            data?.deductScore ?? 0


          student.actualLimit =
            data?.actualLimit ?? 40

        } catch (error) {

          console.error(
            `获取学生 ${student.id} 成绩失败`,
            error
          )


          student.totalScore =
            0

          student.baseLimit =
            40

          student.bonusScore =
            0

          student.deductScore =
            0

          student.actualLimit =
            40

        }

      }
    )


  await Promise.all(
    promises
  )

}


/* =========================================================
 * 搜索
 * ========================================================= */

async function handleSearch() {

  studentPage.value =
    1

  await loadStudentList()

}


/* =========================================================
 * 重置搜索
 * ========================================================= */

async function resetSearch() {

  search.value = {

    studentNo: '',

    realName: '',

    className: '',

    status: null

  }


  studentPage.value =
    1


  await loadStudentList()

}


/* =========================================================
 * 查看学生详情
 * ========================================================= */

function showDetail(row) {

  router.push(
    `/admin/student/${row.id}`
  )

}


/* =========================================================
 * =========================================================
 * 成绩明细弹窗
 * =========================================================
 * ========================================================= */

const scoreVisible = ref(false)

const scoreLoading = ref(false)

const scoreList = ref([])

const currentStudent = ref(null)


/* =========================================================
 * 成绩明细分页参数
 * ========================================================= */

const scorePage = ref(1)

const scorePageSize = ref(10)

const scoreTotal = ref(0)


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
 * 查看成绩
 * ========================================================= */

async function showScore(student) {

  /*
   * 保存当前学生
   */

  currentStudent.value =
    student


  /*
   * 打开弹窗
   */

  scoreVisible.value =
    true


  /*
   * 每次打开学生成绩，
   * 都从第一页开始。
   */

  scorePage.value =
    1


  scorePageSize.value =
    10


  scoreTotal.value =
    0


  scoreList.value =
    []


  /*
   * 加载第一页
   */

  await loadScoreDetail()

}


/* =========================================================
 * 加载成绩明细
 *
 * 后端接口：
 *
 * /scoreStatistics/admin/{studentId}/detail
 *
 * 参数：
 *
 * pageNum
 * pageSize
 *
 * ========================================================= */

async function loadScoreDetail() {

  if (
    !currentStudent.value
    ||
    !currentStudent.value.id
  ) {

    return

  }


  scoreLoading.value =
    true


  try {

    const res =
      await request.get(
        `/scoreStatistics/admin/${currentStudent.value.id}/detail`,
        {
          params: {

            pageNum:
            scorePage.value,

            pageSize:
            scorePageSize.value

          }

        }
      )


    console.log(
      '成绩明细分页数据：',
      res
    )


    const data =
      res.data?.data


    /*
     * =====================================================
     * MyBatis-Plus Page
     * =====================================================
     */

    if (
      data
      &&
      Array.isArray(data.records)
    ) {

      scoreList.value =
        data.records


      scoreTotal.value =
        Number(
          data.total || 0
        )


      /*
       * 防止当前页超过实际页数
       */

      if (
        data.pages
        &&
        scorePage.value > Number(data.pages)
      ) {

        scorePage.value =
          Number(data.pages)

        await loadScoreDetail()

        return

      }

    }


    else {

      scoreList.value =
        []

      scoreTotal.value =
        0

    }

  } catch (error) {

    console.error(
      '获取成绩明细失败',
      error
    )


    scoreList.value =
      []

    scoreTotal.value =
      0


    ElMessage.error(
      '获取成绩明细失败'
    )

  } finally {

    scoreLoading.value =
      false

  }

}


/* =========================================================
 * 成绩明细每页数量
 * ========================================================= */

async function handleScoreSizeChange(size) {

  scorePageSize.value =
    size


  scorePage.value =
    1


  await loadScoreDetail()

}


/* =========================================================
 * 成绩明细页码
 * ========================================================= */

async function handleScoreCurrentChange(current) {

  scorePage.value =
    current


  await loadScoreDetail()

}


/* =========================================================
 * 关闭成绩弹窗
 * ========================================================= */

function closeScoreDialog() {

  scoreVisible.value =
    false


  scoreList.value =
    []

  scoreTotal.value =
    0

  scorePage.value =
    1

  currentStudent.value =
    null

}


/* =========================================================
 * 隐藏成绩
 * ========================================================= */

async function hideScore(record) {

  try {

    await ElMessageBox.confirm(

      '隐藏后该成绩不再参与计算。确定继续吗？',

      '提示',

      {
        type: 'warning'
      }

    )


    await request.put(
      `/scoreRecord/admin/hide/${record.id}`
    )


    ElMessage.success(
      '已隐藏'
    )


    /*
     * 刷新当前页成绩明细
     */

    await loadScoreDetail()


    /*
     * 更新当前学生综合成绩
     */

    await refreshCurrentStudentScore()

  } catch (e) {

    /*
     * 用户取消不提示
     */

  }

}


/* =========================================================
 * 恢复成绩
 * ========================================================= */

async function showHiddenScore(record) {

  try {

    await ElMessageBox.confirm(

      '恢复后该成绩重新参与计算。确定继续吗？',

      '提示',

      {
        type: 'warning'
      }

    )


    await request.put(
      `/scoreRecord/admin/show/${record.id}`
    )


    ElMessage.success(
      '已恢复'
    )


    /*
     * 刷新当前页
     */

    await loadScoreDetail()


    /*
     * 更新综合成绩
     */

    await refreshCurrentStudentScore()

  } catch (e) {

    /*
     * 用户取消
     */

  }

}


/* =========================================================
 * 刷新当前学生综合成绩
 * ========================================================= */

async function refreshCurrentStudentScore() {

  if (
    !currentStudent.value
  ) {

    return

  }


  try {

    const res =
      await request.get(
        `/scoreStatistics/admin/${currentStudent.value.id}`
      )


    const data =
      res.data?.data


    if (!data) {

      return

    }


    Object.assign(
      currentStudent.value,
      {

        totalScore:
          data.totalScore ?? 0,

        baseLimit:
          data.baseLimit ?? 40,

        bonusScore:
          data.bonusScore ?? 0,

        deductScore:
          data.deductScore ?? 0,

        actualLimit:
          data.actualLimit ?? 40

      }
    )


    /*
     * 同时更新当前列表中的学生
     */

    const student =
      list.value.find(
        item =>
          Number(item.id)
          ===
          Number(currentStudent.value.id)
      )


    if (student) {

      Object.assign(
        student,
        {

          totalScore:
            data.totalScore ?? 0,

          baseLimit:
            data.baseLimit ?? 40,

          bonusScore:
            data.bonusScore ?? 0,

          deductScore:
            data.deductScore ?? 0,

          actualLimit:
            data.actualLimit ?? 40

        }
      )

    }

  } catch (error) {

    console.error(
      '刷新综合成绩失败',
      error
    )

  }

}


/* =========================================================
 * 禁用学生
 * ========================================================= */

async function disableStudent(student) {

  try {

    await ElMessageBox.confirm(

      `确定禁用“${student.realName}”吗？`,

      '提示',

      {
        type: 'warning'
      }

    )


    await request.put(
      `/user/student/disable/${student.id}`
    )


    ElMessage.success(
      '已禁用'
    )


    /*
     * 重新加载当前页
     */

    await loadStudentList()

  } catch (e) {

    /*
     * 用户取消
     */

  }

}


/* =========================================================
 * 启用学生
 * ========================================================= */

async function enableStudent(student) {

  try {

    await request.put(
      `/user/student/enable/${student.id}`
    )


    ElMessage.success(
      '已启用'
    )


    /*
     * 重新加载当前页
     */

    await loadStudentList()

  } catch (error) {

    console.error(
      '启用学生失败',
      error
    )


    ElMessage.error(
      error.response?.data?.msg
      ||
      error.response?.data?.message
      ||
      '启用失败'
    )

  }

}


/* =========================================================
 * 分数格式化
 * ========================================================= */

function formatScore(score) {

  if (
    score === null
    ||
    score === undefined
  ) {

    return '...'

  }


  const value =
    Number(score)


  if (
    Number.isNaN(value)
  ) {

    return '...'

  }


  if (
    Number.isInteger(value)
  ) {

    return value

  }


  return value.toFixed(2)

}


/* =========================================================
 * 带正负号的分数
 * ========================================================= */

function formatScoreWithPrefix(
  score,
  prefix
) {

  if (
    score === null
    ||
    score === undefined
  ) {

    return '...'

  }


  return (
    prefix
    +
    formatScore(score)
  )

}


/* =========================================================
 * 初始化
 * ========================================================= */

onMounted(() => {

  loadStudentList()

})

</script>


<style scoped>

.student-page {

  padding: 24px;

  background: #f5f7fa;

  min-height:
    calc(100vh - 60px);

}


/* =========================================================
 * 页面标题
 * ========================================================= */

.page-header {

  display: flex;

  justify-content:
    space-between;

  align-items:
    center;

  margin-bottom: 20px;

}


.page-header h2 {

  margin:
    0 0 6px;

  font-size: 24px;

  color: #303133;

}


.page-header p {

  margin: 0;

  color: #909399;

  font-size: 14px;

}


/* =========================================================
 * 搜索
 * ========================================================= */

.search-card {

  margin-bottom: 20px;

}


.search-row {

  display: flex;

  gap: 12px;

  align-items:
    center;

  flex-wrap:
    wrap;

}


.search-item {

  width: 180px;

}


/* =========================================================
 * 表格
 * ========================================================= */

.table-card {

  border-radius: 8px;

}


/* =========================================================
 * 分数颜色
 * ========================================================= */

.bonus-score {

  color: #67c23a;

  font-weight: 600;

}


.deduct-score {

  color: #f56c6c;

  font-weight: 600;

}


.total-score {

  color: #409eff;

  font-size: 18px;

  font-weight: 700;

}


/* =========================================================
 * 学生列表分页
 * ========================================================= */

.student-pagination-wrapper {

  display: flex;

  justify-content:
    flex-end;

  align-items:
    center;

  margin-top: 20px;

  padding-bottom: 5px;

}


/* =========================================================
 * 成绩概览
 * ========================================================= */

.score-summary {

  display: flex;

  align-items:
    center;

  gap: 28px;

  padding:
    10px 0;

  flex-wrap:
    wrap;

}


.student-name {

  font-size: 20px;

  font-weight: 700;

  color: #303133;

  margin-right: 10px;

}


.summary-item {

  display: flex;

  flex-direction:
    column;

  gap: 5px;

  color: #909399;

  font-size: 13px;

}


.summary-item strong {

  font-size: 17px;

  color: #303133;

}


.summary-item.final strong {

  color: #409eff;

  font-size: 22px;

}


/* =========================================================
 * 成绩明细分页
 * ========================================================= */

.score-pagination-wrapper {

  display: flex;

  justify-content:
    flex-end;

  align-items:
    center;

  margin-top: 20px;

  padding-bottom: 5px;

}

</style>
