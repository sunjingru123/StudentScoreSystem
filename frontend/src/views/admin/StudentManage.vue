<template>
  <div class="score-page">

    <!-- =====================================================
         页面标题
         ===================================================== -->

    <div class="page-header">

      <div>

        <h2>
          {{ pageTitle }}
        </h2>

        <p>
          {{ pageDescription }}
        </p>

      </div>


      <!-- =================================================
           管理员：添加单个学生
           （档案部正副部长只维护成绩，不新建账号）
           ================================================== -->

      <div
        v-if="isAdmin"
        class="page-header-actions"
      >

        <el-button
          plain
          @click="openRecycleDialog"
        >
          回收站
        </el-button>

        <el-button
          type="primary"
          @click="openAddDialog"
        >
          添加学生
        </el-button>

      </div>


      <!-- =================================================
           档案部正副部长：我的删除申请入口
           ================================================== -->

      <div
        v-else
        class="page-header-actions"
      >

        <el-button
          type="primary"
          plain
          @click="goMyDeleteApplies"
        >
          我的删除申请
        </el-button>

      </div>

    </div>


    <!-- =====================================================
         搜索
         ===================================================== -->

    <el-card
      shadow="never"
      class="search-card"
    >

      <el-form
        :inline="true"
        @submit.prevent
      >

        <el-form-item label="搜索">

          <el-input
            v-model="keyword"
            placeholder="学号 / 姓名 / 用户名 / 班级"
            clearable
            style="width: 280px"
            @keyup.enter="handleSearch"
          />

        </el-form-item>


        <el-form-item label="班级">

          <el-select
            v-model="classFilter"
            placeholder="全部班级"
            clearable
            filterable
            style="width: 180px"
            @change="handleSearch"
          >

            <el-option
              v-for="item in classOptions"
              :key="item"
              :label="item"
              :value="item"
            />

          </el-select>

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
          width="330"
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
              v-if="isAdmin"
              type="warning"
              link
              @click="handleResetPassword(row)"
            >
              重置密码
            </el-button>

            <el-button
              v-if="isAdmin"
              type="danger"
              link
              @click="openStudentDeleteDialog(row)"
            >
              删除
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

            {{ row.sourceName || formatSource(row.sourceType) }}

          </template>

        </el-table-column>


        <!-- 状态 -->

        <el-table-column
          label="状态"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <!-- 已删除 / 已作废 -->

            <el-tag
              v-if="Number(row.status) === 0"
              type="danger"
            >
              已删除
            </el-tag>


            <!-- 管理员隐藏（历史数据） -->

            <el-tag
              v-else-if="Number(row.adminHidden) === 1"
              type="warning"
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

            <!--
              已删除 / 已隐藏的记录可以恢复，
              正常记录可以删除
            -->

            <el-button
              v-if="isRemoved(row)"
              type="success"
              link
              @click="handleRestore(row)"
            >
              恢复
            </el-button>


            <el-button
              v-else
              type="danger"
              link
              @click="handleDelete(row)"
            >
              {{ isAdmin ? '删除' : '申请删除' }}
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


    <!-- =====================================================
         添加学生
         ===================================================== -->

    <el-dialog
      v-model="addDialogVisible"
      title="添加学生"
      width="520px"
      :close-on-click-modal="false"
    >

      <!--
        autocomplete="off"：
        避免浏览器把已保存的登录凭据自动填到
        「邮箱」「初始密码」等字段里
        （测试报告 BUG-04）。
      -->

      <el-form
        ref="addFormRef"
        :model="addForm"
        :rules="addRules"
        label-width="96px"
        autocomplete="off"
        @submit.prevent
      >

        <el-form-item
          label="学号"
          prop="studentNo"
        >

          <el-input
            v-model="addForm.studentNo"
            maxlength="20"
            autocomplete="off"
            placeholder="必填，系统内唯一；同时作为登录用户名"
          />

        </el-form-item>


        <el-form-item
          label="姓名"
          prop="realName"
        >

          <el-input
            v-model="addForm.realName"
            maxlength="50"
            autocomplete="off"
            placeholder="必填"
          />

        </el-form-item>


        <el-form-item
          label="班级"
          prop="className"
        >

          <el-input
            v-model="addForm.className"
            maxlength="100"
            autocomplete="off"
            placeholder="必填，例如：2025级7班"
          />

        </el-form-item>


        <el-form-item
          label="手机号"
          prop="phone"
        >

          <el-input
            v-model="addForm.phone"
            maxlength="20"
            autocomplete="off"
            placeholder="选填"
          />

        </el-form-item>


        <el-form-item
          label="邮箱"
          prop="email"
        >

          <el-input
            v-model="addForm.email"
            maxlength="100"
            autocomplete="off"
            placeholder="选填"
          />

        </el-form-item>


        <el-form-item
          label="初始密码"
          prop="password"
        >

          <el-input
            v-model="addForm.password"
            type="password"
            show-password
            maxlength="50"
            autocomplete="new-password"
            placeholder="留空则使用默认密码 123456"
          />

        </el-form-item>

      </el-form>


      <div class="form-tip">

        新建学生首次登录时必须修改密码。

      </div>


      <template #footer>

        <el-button
          @click="addDialogVisible = false"
        >
          取消
        </el-button>

        <el-button
          type="primary"
          :loading="addSubmitting"
          @click="submitAddStudent"
        >
          确定
        </el-button>

      </template>

    </el-dialog>


    <!-- =====================================================
         删除学生（软删除，进入回收站）
         ===================================================== -->

    <el-dialog
      v-model="deleteDialogVisible"
      title="删除学生"
      width="480px"
      :close-on-click-modal="false"
    >

      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="删除后学生将无法登录，
        历史成绩、加减分、获奖和审核记录都会保留，可在回收站恢复。"
        style="margin-bottom: 16px"
      />


      <div
        v-if="deleteTarget"
        class="delete-target"
      >
        学生：
        <strong>
          {{ deleteTarget.realName || '-' }}
        </strong>
        （{{ deleteTarget.studentNo || '-' }}）
      </div>


      <el-form
        label-width="80px"
        @submit.prevent
      >

        <el-form-item
          label="删除原因"
          required
        >

          <el-input
            v-model="deleteReason"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="必填，例如：学生已退学 / 重复录入"
          />

        </el-form-item>

      </el-form>


      <template #footer>

        <el-button
          @click="deleteDialogVisible = false"
        >
          取消
        </el-button>

        <el-button
          type="danger"
          :loading="deleteSubmitting"
          @click="submitDeleteStudent"
        >
          确认删除
        </el-button>

      </template>

    </el-dialog>


    <!-- =====================================================
         回收站
         ===================================================== -->

    <el-dialog
      v-model="recycleVisible"
      title="学生回收站"
      width="980px"
      :close-on-click-modal="false"
    >

      <div class="recycle-toolbar">

        <el-input
          v-model="recycleKeyword"
          placeholder="学号 / 姓名 / 用户名 / 班级"
          clearable
          style="width: 300px"
          @keyup.enter="handleRecycleSearch"
          @clear="handleRecycleSearch"
        />


        <el-button
          type="primary"
          @click="handleRecycleSearch"
        >
          搜索
        </el-button>

      </div>


      <el-table
        :data="recycleList"
        border
        stripe
        v-loading="recycleLoading"
      >

        <el-table-column
          prop="studentNo"
          label="学号"
          width="140"
        />

        <el-table-column
          prop="realName"
          label="姓名"
          width="110"
        />

        <el-table-column
          prop="className"
          label="班级"
          min-width="140"
        />

        <el-table-column
          label="删除时间"
          width="170"
        >

          <template #default="{ row }">

            {{ formatDateTime(row.deletedTime) }}

          </template>

        </el-table-column>


        <el-table-column
          label="操作管理员"
          width="120"
        >

          <template #default="{ row }">

            {{ row.deletedByName || '-' }}

          </template>

        </el-table-column>


        <el-table-column
          prop="deleteReason"
          label="删除原因"
          min-width="180"
          show-overflow-tooltip
        />


        <el-table-column
          label="操作"
          width="100"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              type="success"
              link
              @click="handleRestoreStudent(row)"
            >
              恢复
            </el-button>

          </template>

        </el-table-column>

      </el-table>


      <el-empty
        v-if="
          !recycleLoading &&
          recycleList.length === 0
        "
        description="回收站为空"
      />


      <div
        v-if="recycleTotal > 0"
        class="pagination-wrapper"
      >

        <el-pagination
          v-model:current-page="recyclePageNum"
          :page-size="recyclePageSize"
          layout="total, prev, pager, next"
          :total="recycleTotal"
          background
          :hide-on-single-page="false"
          @current-change="handleRecyclePageChange"
        />

      </div>

    </el-dialog>

  </div>
</template>


<script setup>

import {
  ref,
  computed,
  nextTick,
  onMounted,
} from 'vue'


import {
  useRouter,
} from 'vue-router'


import {
  ElMessage,
  ElMessageBox,
} from 'element-plus'


import {
  getAdminStudentScores,
  getAdminStudentTotal,
  deleteScoreRecord,
  restoreScoreRecord,
} from '@/api/adminScore'


import {
  submitDeleteApply,
} from '@/api/scoreDeleteApply'


import request from '@/api/request'
import {
  addStudent,
  resetStudentPassword,
  deleteStudentWithReason,
  restoreStudent,
  getRecycleStudents,
} from '@/api/student'


/* =========================================================
 * 当前登录用户
 *
 * 仅用于控制“添加学生 / 重置密码”等管理员专属入口的显示，
 * 真正的权限校验始终在后端。
 * ========================================================= */

function readStoredUser() {

  try {

    return JSON.parse(
      localStorage.getItem('user') || '{}',
    ) || {}

  }
  catch (error) {

    return {}

  }

}


const storedUser =
  readStoredUser()


const router =
  useRouter()


function goMyDeleteApplies() {

  router.push(
    '/home/score-delete-apply',
  )

}


const isAdmin =
  computed(
    () => storedUser.role === '管理员',
  )


const pageTitle =
  computed(
    () =>
      isAdmin.value
        ? '学生管理'
        : '学生成绩管理',
  )


const pageDescription =
  computed(
    () =>
      isAdmin.value
        ? '查看学生信息及综合测评成绩'
        : '查看全部学生成绩；删除加减分需提交删除申请，经辅导员审核通过后执行',
  )


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


/* 班级筛选：'' = 全部班级 */

const classFilter =
  ref('')


/* 班级下拉选项：来自后端 /user/student/classes */

const classOptions =
  ref([])


/* =========================================================
 * 班级下拉选项
 * ========================================================= */

async function loadClassOptions() {

  try {

    const res =
      await request.get(
        '/user/student/classes'
      )


    const data =
      unwrapResult(res)


    classOptions.value =
      Array.isArray(data)
        ? data
        : []

  } catch (error) {

    /*
     * 班级下拉加载失败不影响学生列表本身，
     * 只记录错误，保留关键字搜索能力。
     */
    console.error(
      '加载班级列表失败：',
      error
    )

    classOptions.value = []
  }
}


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

            className:
              classFilter.value
              || undefined,

          },
        },
      )




    const data =
      unwrapResult(res)




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

  classFilter.value =
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




    const data =
      unwrapResult(res)




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
 * 是否为已删除 / 已隐藏记录
 * ========================================================= */

function isRemoved(row) {

  return (
    Number(row?.status) === 0
    ||
    Number(row?.adminHidden) === 1
  )

}


/* =========================================================
 * 删除一条成绩记录
 *
 * 管理员：直接删除
 * 档案部部长 / 副部长：提交删除申请，经辅导员审核后执行
 * ========================================================= */

async function handleDelete(row) {

  if (isAdmin.value) {

    await directDeleteRecord(row)

    return

  }


  await submitDeleteRecordApply(row)

}


/* =========================================================
 * 管理员直接删除
 * ========================================================= */

async function directDeleteRecord(row) {

  try {

    await ElMessageBox.confirm(
      `确定删除「${row.ruleName || '该条'}」这条加减分吗？\n\n删除后该记录不再计入学生成绩，可由管理员恢复。`,
      '删除成绩',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
      },
    )


    const res =
      await deleteScoreRecord(
        row.id,
      )


    if (Number(res?.code) !== 200) {

      ElMessage.error(
        res?.message
        || '删除成绩失败',
      )

      return

    }


    ElMessage.success(
      '删除成功',
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
        '删除成绩失败：',
        error,
      )

      ElMessage.error(
        error?.response?.data?.message
        || '删除成绩失败',
      )

    }

  }

}


/* =========================================================
 * 档案部正副部长：提交删除申请
 * ========================================================= */

async function submitDeleteRecordApply(row) {

  try {

    const { value } =
      await ElMessageBox.prompt(
        `确定删除「${row.ruleName || '该条'}」这条加减分吗？\n\n提交后需经辅导员（高级辅导员 / 档案部负责辅导员）审核通过后才会真正删除。`,
        '申请删除成绩',
        {
          type: 'warning',
          confirmButtonText: '提交申请',
          cancelButtonText: '取消',
          inputPlaceholder: '删除原因（建议填写）',
          inputValue: '',
        },
      )


    const res =
      await submitDeleteApply({
        recordId: row.id,
        reason: value || '',
      })


    if (Number(res?.code) !== 200) {

      ElMessage.error(
        res?.message
        || '提交删除申请失败',
      )

      return

    }


    ElMessage.success(
      '删除申请已提交，请等待辅导员审核',
    )

  }
  catch (error) {

    if (
      error !== 'cancel' &&
      error !== 'close'
    ) {

      console.error(
        '提交删除申请失败：',
        error,
      )

      ElMessage.error(
        error?.response?.data?.message
        || '提交删除申请失败',
      )

    }

  }

}


/* =========================================================
 * 恢复一条被删除 / 隐藏的成绩
 *
 * 权限：管理员 / 档案部部长、副部长（后端校验）
 * ========================================================= */

async function handleRestore(row) {

  try {

    await ElMessageBox.confirm(
      '确定恢复这条成绩吗？恢复后会重新计入学生成绩。',
      '恢复成绩',
      {
        type: 'warning',
        confirmButtonText: '确定恢复',
        cancelButtonText: '取消',
      },
    )


    const res =
      await restoreScoreRecord(
        row.id,
      )


    if (Number(res?.code) !== 200) {

      ElMessage.error(
        res?.message
        || '恢复成绩失败',
      )

      return

    }


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
        error?.response?.data?.message
        || '恢复成绩失败',
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
 * 学生删除（软删除）/ 回收站
 *
 * 权限：仅管理员（后端 @RequireRole 校验）
 * ========================================================= */

const deleteDialogVisible =
  ref(false)


const deleteSubmitting =
  ref(false)


const deleteTarget =
  ref(null)


const deleteReason =
  ref('')


function openStudentDeleteDialog(row) {

  deleteTarget.value =
    row

  deleteReason.value =
    ''

  deleteDialogVisible.value =
    true

}


async function submitDeleteStudent() {

  if (!deleteTarget.value) {

    return

  }


  const reason =
    deleteReason.value.trim()


  if (!reason) {

    ElMessage.warning(
      '请填写删除原因',
    )

    return

  }


  try {

    /*
     * 二次确认
     */

    await ElMessageBox.confirm(
      `确定删除学生「${deleteTarget.value.realName || deleteTarget.value.studentNo || ''}」吗？\n\n删除后该学生无法登录，历史成绩与审核记录会保留，可在回收站恢复。`,
      '确认删除',
      {
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
      },
    )

  }
  catch (error) {

    if (error === 'cancel' || error === 'close') {

      return

    }

    throw error

  }


  deleteSubmitting.value =
    true

  try {

    const res =
      await deleteStudentWithReason(
        deleteTarget.value.id,
        reason,
      )


    if (Number(res?.code) !== 200) {

      ElMessage.error(
        res?.message
        || '删除学生失败',
      )

      return

    }


    ElMessage.success(
      '学生已移入回收站',
    )


    deleteDialogVisible.value =
      false


    await loadStudents()


    /*
     * 删除的是当前页最后一条时，回退一页，
     * 避免停在一个空页上。
     */
    if (
      students.value.length === 0 &&
      pageNum.value > 1
    ) {

      pageNum.value =
        pageNum.value - 1

      await loadStudents()

    }

  }
  catch (error) {

    console.error(
      '删除学生失败：',
      error,
    )

    ElMessage.error(
      error?.response?.data?.message
      || error?.message
      || '删除学生失败',
    )

  }
  finally {

    deleteSubmitting.value =
      false

  }

}


/* =========================================================
 * 回收站
 * ========================================================= */

const recycleVisible =
  ref(false)


const recycleLoading =
  ref(false)


const recycleList =
  ref([])


const recycleTotal =
  ref(0)


const recyclePageNum =
  ref(1)


const recyclePageSize =
  10


const recycleKeyword =
  ref('')


async function openRecycleDialog() {

  recycleVisible.value =
    true

  recyclePageNum.value =
    1

  recycleKeyword.value =
    ''

  await loadRecycleStudents()

}


async function loadRecycleStudents() {

  recycleLoading.value =
    true

  try {

    const res =
      await getRecycleStudents({
        pageNum:
          recyclePageNum.value,

        pageSize:
          recyclePageSize,

        keyword:
          recycleKeyword.value.trim()
          || undefined,
      })


    if (Number(res?.code) !== 200) {

      ElMessage.error(
        res?.message
        || '回收站加载失败',
      )

      recycleList.value =
        []

      recycleTotal.value =
        0

      return

    }


    const data =
      unwrapResult(res)


    if (data && Array.isArray(data.records)) {

      recycleList.value =
        data.records

      recycleTotal.value =
        Number(data.total || 0)

    }
    else {

      recycleList.value =
        []

      recycleTotal.value =
        0

    }

  }
  catch (error) {

    console.error(
      '回收站加载失败：',
      error,
    )

    recycleList.value =
      []

    recycleTotal.value =
      0

    ElMessage.error(
      '回收站加载失败',
    )

  }
  finally {

    recycleLoading.value =
      false

  }

}


function handleRecycleSearch() {

  recyclePageNum.value =
    1

  loadRecycleStudents()

}


function handleRecyclePageChange(page) {

  recyclePageNum.value =
    page

  loadRecycleStudents()

}


async function handleRestoreStudent(row) {

  try {

    await ElMessageBox.confirm(
      `确定恢复学生「${row.realName || row.studentNo || ''}」吗？恢复后该学生将重新出现在学生列表中。`,
      '恢复学生',
      {
        type: 'warning',
        confirmButtonText: '确定恢复',
        cancelButtonText: '取消',
      },
    )

  }
  catch (error) {

    if (error === 'cancel' || error === 'close') {

      return

    }

    throw error

  }


  try {

    const res =
      await restoreStudent(
        row.id,
      )


    if (Number(res?.code) !== 200) {

      ElMessage.error(
        res?.message
        || '恢复学生失败',
      )

      return

    }


    ElMessage.success(
      '学生已恢复',
    )


    await loadRecycleStudents()

    await loadStudents()

  }
  catch (error) {

    console.error(
      '恢复学生失败：',
      error,
    )

    ElMessage.error(
      error?.response?.data?.message
      || error?.message
      || '恢复学生失败',
    )

  }

}


/* =========================================================
 * 时间格式化（LocalDateTime -> yyyy-MM-dd HH:mm:ss）
 * ========================================================= */

function formatDateTime(value) {

  if (!value) {

    return '-'

  }


  const text =
    String(value).replace('T', ' ')


  return text.length > 19
    ? text.slice(0, 19)
    : text

}


/* =========================================================
 * 添加学生
 * ========================================================= */

const addDialogVisible =
  ref(false)


const addSubmitting =
  ref(false)


const addFormRef =
  ref(null)


function createEmptyAddForm() {

  return {

    studentNo: '',

    realName: '',

    className: '',

    phone: '',

    email: '',

    password: '',

  }

}


const addForm =
  ref(createEmptyAddForm())


const addRules = {

  studentNo: [
    {
      required: true,
      message: '请输入学号',
      trigger: 'blur',
    },
  ],

  realName: [
    {
      required: true,
      message: '请输入姓名',
      trigger: 'blur',
    },
  ],

  className: [
    {
      required: true,
      message: '请输入班级',
      trigger: 'blur',
    },
  ],

  email: [
    {
      type: 'email',
      message: '邮箱格式不正确',
      trigger: 'blur',
    },
  ],

}


function openAddDialog() {

  addForm.value =
    createEmptyAddForm()

  addDialogVisible.value =
    true

  nextTick(
    () => addFormRef.value?.clearValidate(),
  )

}


async function submitAddStudent() {

  if (!addFormRef.value) {

    return

  }


  try {

    await addFormRef.value.validate()

  }
  catch (error) {

    /*
     * 表单校验未通过
     */

    return

  }


  addSubmitting.value =
    true

  try {

    const form =
      addForm.value


    /*
     * 只提交有值的可选字段
     */

    const payload = {

      studentNo:
        form.studentNo.trim(),

      /*
       * 登录用户名固定使用学号
       */

      username:
        form.studentNo.trim(),

      realName:
        form.realName.trim(),

      className:
        form.className.trim(),

      phone:
        form.phone.trim()
        || undefined,

      email:
        form.email.trim()
        || undefined,

      password:
        form.password.trim()
        || undefined,

    }


    const res =
      await addStudent(payload)


    if (Number(res?.code) !== 200) {

      ElMessage.error(
        res?.message
        || '添加学生失败',
      )

      return

    }


    ElMessage.success(
      payload.password
        ? '学生添加成功'
        : '学生添加成功，初始密码为 123456（首次登录需修改）',
    )


    addDialogVisible.value =
      false


    pageNum.value =
      1


    await loadStudents()

  }
  catch (error) {

    console.error(
      '添加学生失败：',
      error,
    )

    ElMessage.error(
      error?.response?.data?.message
      || '添加学生失败',
    )

  }
  finally {

    addSubmitting.value =
      false

  }

}


/* =========================================================
 * 初始化
 * ========================================================= */

onMounted(() => {

  loadClassOptions()

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
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
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


.page-header-actions {
  flex-shrink: 0;
}


.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}


.delete-target {
  margin-bottom: 12px;
  font-size: 14px;
  color: #303133;
}


.recycle-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
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

@media (max-width: 768px) {
  .score-page { padding: 12px; }
  .page-header h2 { font-size: 20px; }
  .search-card :deep(.el-form-item) { width: 100%; margin-right: 0; }
  .search-card :deep(.el-input) { width: 100% !important; }
  .table-card { overflow-x: auto; }
  .table-card :deep(.el-table) { min-width: 950px; }
  .pagination-wrapper { justify-content: flex-start; min-width: 560px; }
  :deep(.el-drawer) { width: 100% !important; }
  .student-info { grid-template-columns: 1fr; }
  .total-score { grid-column: auto; }
}

</style>
