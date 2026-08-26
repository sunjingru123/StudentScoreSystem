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

      <el-button
        type="primary"
        :loading="loading"
        @click="loadStudents"
      >
        刷新
      </el-button>

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
            @keyup.enter="search"
          />

        </el-form-item>


        <el-form-item>

          <el-button
            type="primary"
            @click="search"
          >
            搜索
          </el-button>

          <el-button
            @click="reset"
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
        :data="filteredStudents"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
      >

        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
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
          min-width="180"
        />


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


        <el-table-column
          label="账号状态"
          width="110"
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
           空数据
      ====================================================== -->
      <el-empty
        v-if="
          !loading
          &&
          filteredStudents.length === 0
        "
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
    >

      <!-- ===================================================
           学生信息
      ==================================================== -->
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


        <div>

          <span>
            用户名：
          </span>

          <strong>
            {{ currentStudent.username || '-' }}
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
      ==================================================== -->
      <el-table
        :data="scoreDetails"
        border
        stripe
        v-loading="detailLoading"
        style="width: 100%"
      >

        <el-table-column
          type="index"
          label="#"
          width="55"
          align="center"
        />


        <el-table-column
          prop="ruleName"
          label="评分项目"
          min-width="150"
          show-overflow-tooltip
        />


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


        <el-table-column
          label="操作"
          width="110"
          fixed="right"
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
           无成绩
      ==================================================== -->
      <el-empty
        v-if="
          !detailLoading
          &&
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
  computed,
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

import request from '@/api/request'


/* =========================================================
 * 学生列表
 * ========================================================= */

const loading =
  ref(false)

const students =
  ref([])


/* =========================================================
 * 搜索
 * ========================================================= */

const keyword =
  ref('')


/* =========================================================
 * 成绩详情
 * ========================================================= */

const drawerVisible =
  ref(false)

const detailLoading =
  ref(false)

const currentStudent =
  ref(null)

const scoreDetails =
  ref([])

const currentTotal =
  ref(0)


/* =========================================================
 * 搜索后的学生
 * ========================================================= */

const filteredStudents =
  computed(() => {

    const key =
      keyword.value
        .trim()
        .toLowerCase()


    if (!key) {

      return students.value

    }


    return students.value.filter(
      student => {

        return (

          String(
            student.studentNo || ''
          )
            .toLowerCase()
            .includes(key)

          ||

          String(
            student.realName || ''
          )
            .toLowerCase()
            .includes(key)

          ||

          String(
            student.className || ''
          )
            .toLowerCase()
            .includes(key)

        )

      }
    )

  })


/* =========================================================
 * 获取学生列表
 *
 * 使用：
 *
 * GET /user/student/all
 *
 * 注意：
 *
 * 这个接口返回全部学生，
 * 不走学生管理页面的分页。
 * ========================================================= */

async function loadStudents() {
  loading.value = true

  try {
    const res = await request.get('/user/student/list')

    console.log('学生列表原始返回：', res)

    const data = res.data?.data

    let list = []

    // 兼容分页接口
    if (data && Array.isArray(data.records)) {
      list = data.records
    }
    // 兼容旧接口直接返回数组
    else if (Array.isArray(data)) {
      list = data
    }
    // 兼容 axios 返回数组
    else if (Array.isArray(res.data)) {
      list = res.data
    }

    console.log('解析后的学生列表：', list)

    if (!Array.isArray(list)) {
      throw new Error('学生列表数据格式异常')
    }

    const result = []

    for (const student of list) {
      let total = 0

      try {
        const totalRes = await getAdminStudentTotal(student.id)

        const totalData = totalRes.data?.data

        if (
          typeof totalData === 'object' &&
          totalData !== null
        ) {
          total =
            totalData.totalScore ??
            totalData.score ??
            0
        } else {
          total =
            totalData ??
            totalRes.data ??
            0
        }

      } catch (e) {
        console.error(
          `获取学生 ${student.id} 总成绩失败：`,
          e
        )
      }

      result.push({
        ...student,
        totalScore: total
      })
    }

    students.value = result

  } catch (error) {
    console.error(
      '获取学生列表失败：',
      error
    )

    students.value = []

    ElMessage.error(
      error.response?.data?.msg ||
      error.response?.data?.message ||
      error.message ||
      '学生列表加载失败'
    )

  } finally {
    loading.value = false
  }
}


/* =========================================================
 * 加载所有学生综合成绩
 * ========================================================= */

async function loadStudentTotals() {

  const studentList =
    students.value


  if (
    !studentList
    ||
    studentList.length === 0
  ) {

    return

  }


  await Promise.all(

    studentList.map(
      async student => {

        try {

          const totalRes =
            await getAdminStudentTotal(
              student.id
            )


          const data =
            totalRes?.data?.data


          /*
           * 兼容：
           *
           * Result.success(数字)
           *
           * Result.success({totalScore: xx})
           */

          if (
            data !== null
            &&
            typeof data === 'object'
            &&
            data.totalScore !== undefined
          ) {

            student.totalScore =
              data.totalScore

          } else {

            student.totalScore =
              data ?? 0

          }

        } catch (error) {

          console.error(
            `学生 ${student.id} 获取综合成绩失败：`,
            error
          )


          student.totalScore =
            0

        }

      }
    )

  )

}


/* =========================================================
 * 搜索
 * ========================================================= */

function search() {

  /*
   * computed 自动过滤。
   *
   * 这里不需要重新请求服务器。
   */

}


/* =========================================================
 * 重置
 * ========================================================= */

function reset() {

  keyword.value =
    ''

}


/* =========================================================
 * 查看成绩
 * ========================================================= */

async function openDetail(student) {

  currentStudent.value =
    student


  drawerVisible.value =
    true


  detailLoading.value =
    true


  scoreDetails.value =
    []


  currentTotal.value =
    student.totalScore ?? 0


  try {

    const [
      detailRes,
      totalRes
    ] =
      await Promise.all([

        getAdminStudentScores(
          student.id
        ),

        getAdminStudentTotal(
          student.id
        )

      ])


    /*
     * =====================================================
     * 解析成绩明细
     * =====================================================
     */

    const detailData =
      detailRes?.data?.data


    if (
      Array.isArray(detailData)
    ) {

      scoreDetails.value =
        detailData

    }

    else if (
      detailData
      &&
      Array.isArray(
        detailData.records
      )
    ) {

      scoreDetails.value =
        detailData.records

    }

    else {

      scoreDetails.value =
        []

    }


    /*
     * =====================================================
     * 解析总成绩
     * =====================================================
     */

    const totalData =
      totalRes?.data?.data


    if (
      totalData
      &&
      typeof totalData === 'object'
      &&
      totalData.totalScore !== undefined
    ) {

      currentTotal.value =
        totalData.totalScore

    }

    else {

      currentTotal.value =
        totalData ?? 0

    }


    /*
     * 同步学生列表中的总成绩
     */

    student.totalScore =
      currentTotal.value


  } catch (error) {

    console.error(
      '成绩详情加载失败：',
      error
    )


    console.error(
      '成绩详情后端返回：',
      error?.response?.data
    )


    ElMessage.error(
      error?.response?.data?.msg
      ||
      error?.response?.data?.message
      ||
      '成绩加载失败'
    )

  } finally {

    detailLoading.value =
      false

  }

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


    /*
     * 隐藏成绩
     */

    await hideScore(
      row.id
    )


    /*
     * 当前学生 ID
     */

    const studentId =
      getStudentId(row)


    /*
     * 重新加载成绩明细和总成绩
     */

    await reloadCurrentStudent(
      studentId
    )


    /*
     * 刷新学生列表
     */

    await loadStudents()


    ElMessage.success(
      '成绩已隐藏'
    )

  } catch (error) {

    /*
     * Element Plus 点击取消
     * 通常返回：
     *
     * 'cancel'
     */

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
      error?.response?.data?.msg
      ||
      error?.response?.data?.message
      ||
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


    /*
     * 恢复
     */

    await showScore(
      row.id
    )


    /*
     * 获取学生 ID
     */

    const studentId =
      getStudentId(row)


    /*
     * 重新加载
     */

    await reloadCurrentStudent(
      studentId
    )


    /*
     * 刷新学生列表
     */

    await loadStudents()


    ElMessage.success(
      '成绩已恢复'
    )

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
      error?.response?.data?.msg
      ||
      error?.response?.data?.message
      ||
      '恢复失败'
    )

  }

}


/* =========================================================
 * 获取成绩记录对应的学生 ID
 * ========================================================= */

function getStudentId(row) {

  if (
    row?.studentId !== undefined
    &&
    row?.studentId !== null
  ) {

    return row.studentId

  }


  if (
    row?.userId !== undefined
    &&
    row?.userId !== null
  ) {

    return row.userId

  }


  /*
   * 最后的兼容：
   *
   * 某些接口可能直接把学生 ID 放在 id。
   */

  return row?.id

}


/* =========================================================
 * 重新加载当前学生成绩
 * ========================================================= */

async function reloadCurrentStudent(
  studentId
) {

  if (
    studentId === undefined
    ||
    studentId === null
  ) {

    return

  }


  try {

    const [
      detailRes,
      totalRes
    ] =
      await Promise.all([

        getAdminStudentScores(
          studentId
        ),

        getAdminStudentTotal(
          studentId
        )

      ])


    /*
     * 成绩明细
     */

    const detailData =
      detailRes?.data?.data


    if (
      Array.isArray(detailData)
    ) {

      scoreDetails.value =
        detailData

    }

    else if (
      detailData
      &&
      Array.isArray(
        detailData.records
      )
    ) {

      scoreDetails.value =
        detailData.records

    }

    else {

      scoreDetails.value =
        []

    }


    /*
     * 总成绩
     */

    const totalData =
      totalRes?.data?.data


    if (
      totalData
      &&
      typeof totalData === 'object'
      &&
      totalData.totalScore !== undefined
    ) {

      currentTotal.value =
        totalData.totalScore

    }

    else {

      currentTotal.value =
        totalData ?? 0

    }


    /*
     * 更新当前学生
     */

    if (
      currentStudent.value
      &&
      Number(currentStudent.value.id)
      ===
      Number(studentId)
    ) {

      currentStudent.value.totalScore =
        currentTotal.value

    }

  } catch (error) {

    console.error(
      '刷新当前学生成绩失败：',
      error
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
 * 分数格式
 * ========================================================= */

function formatScore(
  score
) {

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

  min-height: calc(100vh - 60px);

}


/* =========================================================
 * 页面标题
 * ========================================================= */

.page-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

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
 * 综合成绩
 * ========================================================= */

.score {

  font-size: 18px;

  font-weight: 600;

  color: #409eff;

}


/* =========================================================
 * 学生信息
 * ========================================================= */

.student-info {

  display: grid;

  grid-template-columns: 1fr 1fr;

  gap: 15px;

  padding: 5px 0;

}


.student-info span {

  color: #909399;

}


.student-info strong {

  color: #303133;

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


/* =========================================================
 * 分数
 * ========================================================= */

.score-plus {

  color: #67c23a;

  font-weight: 600;

}


.score-minus {

  color: #f56c6c;

  font-weight: 600;

}

</style>
