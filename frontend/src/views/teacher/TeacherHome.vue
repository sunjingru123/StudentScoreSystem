<template>
  <div class="teacher-home">

    <!-- =========================
         欢迎区域
    ========================== -->
    <el-card class="welcome-card">
      <div class="welcome-content">

        <div class="welcome-left">

          <div class="welcome-title">
            👋 欢迎，{{ teacherName }}老师
          </div>

          <div class="welcome-subtitle">
            学生综合测评管理工作台
          </div>

          <div class="welcome-tip">
            今天也辛苦啦，下面是当前需要处理的工作。
          </div>

        </div>

        <div class="welcome-icon">
          🎓
        </div>

      </div>
    </el-card>


    <!-- =========================
         数据统计
    ========================== -->
    <div class="statistics">

      <!-- =========================
           待终审
      ========================== -->
      <el-card
        class="stat-card clickable"
        @click="$router.push('/teacher/department-score-audit')"
      >

        <div class="stat-content">

          <div class="stat-icon warning">
            <el-icon>
              <Clock />
            </el-icon>
          </div>

          <div class="stat-info">

            <div class="stat-label">
              待终审申请
            </div>

            <div class="stat-number">
              {{ pendingCount }}
            </div>

            <div class="stat-desc">
              等待您处理
            </div>

          </div>

        </div>

      </el-card>


      <!-- =========================
           今日申请
      ========================== -->
      <el-card class="stat-card">

        <div class="stat-content">

          <div class="stat-icon primary">
            <el-icon>
              <Document />
            </el-icon>
          </div>

          <div class="stat-info">

            <div class="stat-label">
              今日申请
            </div>

            <div class="stat-number">
              {{ todayCount }}
            </div>

            <div class="stat-desc">
              今日新增部门申报
            </div>

          </div>

        </div>

      </el-card>


      <!-- =========================
           已审核
      ========================== -->
      <el-card class="stat-card">

        <div class="stat-content">

          <div class="stat-icon success">
            <el-icon>
              <CircleCheck />
            </el-icon>
          </div>

          <div class="stat-info">

            <div class="stat-label">
              已审核
            </div>

            <div class="stat-number">
              {{ auditedCount }}
            </div>

            <div class="stat-desc">
              部门申报已完成终审
            </div>

          </div>

        </div>

      </el-card>


      <!-- =========================
           学生人数
           点击进入成绩查看
      ========================== -->
      <el-card
        class="stat-card clickable"
        @click="$router.push('/teacher/score')"
      >

        <div class="stat-content">

          <div class="stat-icon info">
            <el-icon>
              <User />
            </el-icon>
          </div>

          <div class="stat-info">

            <div class="stat-label">
              学生人数
            </div>

            <div class="stat-number">
              {{ studentCount }}
            </div>

            <div class="stat-desc">
              当前管理学生
            </div>

          </div>

        </div>

      </el-card>

    </div>


    <!-- =========================
         下方内容
    ========================== -->
    <div class="content-grid">


      <!-- =========================
           待终审申请
      ========================== -->
      <el-card class="panel">

        <template #header>

          <div class="panel-header">

            <span>
              待终审申请
            </span>

            <el-button
              type="primary"
              link
              @click="$router.push('/teacher/department-score-audit')"
            >
              查看全部 →
            </el-button>

          </div>

        </template>


        <el-table
          v-loading="loading"
          :data="pendingList"
          style="width: 100%"
        >

          <!-- 学生 -->
          <el-table-column
            prop="studentName"
            label="学生"
            min-width="100"
          />


          <!-- 部门 -->
          <el-table-column
            prop="departmentName"
            label="部门"
            min-width="100"
          />


          <!-- 申报项目 -->
          <el-table-column
            prop="title"
            label="申报项目"
            min-width="140"
          />


          <!-- 分数 -->
          <el-table-column
            label="分数"
            width="100"
          >

            <template #default="scope">

              <span
                class="score"
                :class="{
                  minus:
                    scope.row.scoreType === -1
                }"
              >

                {{
                  scope.row.scoreType === -1
                    ? '-'
                    : '+'
                }}

                {{ scope.row.score }}

              </span>

            </template>

          </el-table-column>


          <!-- 申请时间 -->
          <el-table-column
            prop="createTime"
            label="申请时间"
            min-width="170"
          />

        </el-table>


        <!-- 空数据 -->
        <el-empty
          v-if="
            !loading &&
            pendingList.length === 0
          "
          description="暂无待终审申请"
        />

      </el-card>


      <!-- =========================
           快捷操作
      ========================== -->
      <el-card class="panel quick-panel">

        <template #header>

          <div class="panel-title">
            快捷操作
          </div>

        </template>


        <div class="quick-actions">


          <!-- =========================
               部门申报审核
          ========================== -->
          <div
            class="quick-item"
            @click="
              $router.push(
                '/teacher/department-score-audit'
              )
            "
          >

            <div class="quick-icon audit">

              <el-icon>
                <Checked />
              </el-icon>

            </div>

            <div>

              <div class="quick-title">
                部门申报审核
              </div>

              <div class="quick-desc">
                审核部长初审通过的部门申报
              </div>

            </div>

          </div>


          <!-- =========================
               成绩查看
          ========================== -->
          <div
            class="quick-item"
            @click="$router.push('/teacher/score')"
          >

            <div class="quick-icon student">

              <el-icon>
                <Trophy />
              </el-icon>

            </div>

            <div>

              <div class="quick-title">
                成绩查看
              </div>

              <div class="quick-desc">
                查看学生综合测评加减分及成绩
              </div>

            </div>

          </div>


          <!-- =========================
               活动管理
          ========================== -->
          <div
            class="quick-item"
            @click="$router.push('/teacher/activity')"
          >

            <div class="quick-icon activity">

              <el-icon>
                <Calendar />
              </el-icon>

            </div>

            <div>

              <div class="quick-title">
                活动管理
              </div>

              <div class="quick-desc">
                查看学生参与活动
              </div>

            </div>

          </div>


        </div>

      </el-card>

    </div>

  </div>
</template>


<script setup>

import {
  ref,
  onMounted
} from 'vue'

import {
  useRouter
} from 'vue-router'

import request from '@/utils/request'

import {
  Clock,
  Document,
  CircleCheck,
  User,
  Checked,
  Calendar,
  Trophy
} from '@element-plus/icons-vue'


const router = useRouter()


/* =========================
   当前辅导员
========================= */

const teacherName =
  ref('辅导员')


/* =========================
   统计数据
========================= */

const pendingCount =
  ref(0)

const todayCount =
  ref(0)

const auditedCount =
  ref(0)

const studentCount =
  ref(0)


/* =========================
   待终审列表
========================= */

const pendingList =
  ref([])

const loading =
  ref(false)


/* ============================================================
   获取当前辅导员
============================================================ */

function loadTeacher() {

  const userStr =
    localStorage.getItem('user')


  if (!userStr) {

    router.replace('/login')

    return
  }


  try {

    const user =
      JSON.parse(userStr)


    console.log(
      '当前辅导员：',
      user
    )


    teacherName.value =
      user.realName ||
      user.username ||
      '辅导员'


  } catch (error) {

    console.error(
      '读取用户信息失败：',
      error
    )

  }

}


/* ============================================================
   获取辅导员待最终审核部门申报
============================================================ */

async function loadPending() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/departmentScoreApply/final-audit/list'
      )


    console.log(
      '辅导员最终审核完整响应：',
      res
    )


    const result =
      res?.data


    console.log(
      '接口 data：',
      result
    )


    /*
     * 情况1
     *
     * {
     *   code: 200,
     *   data: []
     * }
     */

    if (
      Array.isArray(
        result?.data
      )
    ) {

      pendingList.value =
        result.data

      pendingCount.value =
        result.data.length

      return

    }


    /*
     * 情况2
     *
     * {
     *   code: 200,
     *   data: {
     *      list: []
     *   }
     * }
     */

    if (
      Array.isArray(
        result?.data?.list
      )
    ) {

      pendingList.value =
        result.data.list

      pendingCount.value =
        result.data.list.length

      return

    }


    /*
     * 情况3
     *
     * {
     *   code: 200,
     *   data: {
     *      records: []
     *   }
     * }
     */

    if (
      Array.isArray(
        result?.data?.records
      )
    ) {

      pendingList.value =
        result.data.records

      pendingCount.value =
        result.data.records.length

      return

    }


    console.warn(
      '最终审核接口没有找到数组数据：',
      result
    )


    pendingList.value = []

    pendingCount.value = 0


  } catch (error) {

    console.error(
      '获取部门最终审核申请失败：',
      error
    )


    pendingList.value = []

    pendingCount.value = 0


  } finally {

    loading.value = false

  }

}


/* ============================================================
   获取今日部门申报数量
============================================================ */

async function loadTodayCount() {

  try {

    const res =
      await request.get(
        '/departmentScoreApply/final-audit/list'
      )


    const data =
      res?.data?.data


    if (
      !Array.isArray(data)
    ) {

      todayCount.value = 0

      return

    }


    const today =
      new Date()


    const year =
      today.getFullYear()


    const month =
      String(
        today.getMonth() + 1
      ).padStart(
        2,
        '0'
      )


    const day =
      String(
        today.getDate()
      ).padStart(
        2,
        '0'
      )


    const todayStr =
      `${year}-${month}-${day}`


    todayCount.value =
      data.filter(
        item => {

          if (
            !item.createTime
          ) {

            return false

          }


          return item.createTime
            .startsWith(
              todayStr
            )

        }
      ).length


  } catch (error) {

    console.error(
      '获取今日部门申报失败：',
      error
    )


    todayCount.value = 0

  }

}


/* ============================================================
   已审核数量
============================================================ */

function loadAuditedCount() {

  /*
   * 当前没有专门的历史接口，
   * 所以暂时保持 0。
   */

  auditedCount.value = 0

}


/* ============================================================
   获取学生人数
   与“成绩查看”保持一致
============================================================ */

async function loadStudentCount() {

  try {

    /*
     * 与成绩查看使用同一个接口
     *
     * pageSize = 1 就够了，
     * 我们真正需要的是后端返回的 total。
     */

    const res =
      await request.get(
        '/user/student/list',
        {
          params: {
            pageNum: 1,
            pageSize: 1
          }
        }
      )


    console.log(
      '首页学生人数响应：',
      res
    )


    const responseData =
      res?.data


    /*
     * 与成绩查看页面保持同样的数据结构处理方式
     *
     * 常见情况：
     *
     * res.data = {
     *   code: 200,
     *   data: {
     *     records: [...],
     *     total: 123
     *   }
     * }
     */

    const pageData =
      responseData?.data ??
      responseData


    /*
     * 直接读取 total
     *
     * 这就是成绩查看页面顶部：
     *
     * 共 {{ total }} 名学生
     *
     * 使用的数量。
     */

    if (
      pageData &&
      pageData.total !== undefined
    ) {

      studentCount.value =
        Number(pageData.total) || 0

      console.log(
        '首页当前管理学生人数：',
        studentCount.value
      )

      return

    }


    /*
     * 如果后端没有返回 total，
     * 再尝试兼容 records。
     */

    if (
      Array.isArray(
        pageData?.records
      )
    ) {

      studentCount.value =
        pageData.records.length

      return

    }


    /*
     * 最后兼容原来的数组格式
     */

    if (
      Array.isArray(
        pageData
      )
    ) {

      studentCount.value =
        pageData.length

      return

    }


    console.warn(
      '学生人数接口没有找到有效数据：',
      pageData
    )


    studentCount.value = 0

  }

  catch (error) {

    console.error(
      '获取学生人数失败：',
      error
    )


    studentCount.value = 0

  }

}

/* ============================================================
   页面加载
============================================================ */

onMounted(() => {

  loadTeacher()

  loadPending()

  loadTodayCount()

  loadAuditedCount()

  loadStudentCount()

})

</script>


<style scoped>

.teacher-home {
  width: 100%;
}


/* =========================
   欢迎区域
========================= */

.welcome-card {
  margin-bottom: 20px;

  border: none;

  background:
    linear-gradient(
      135deg,
      #409eff 0%,
      #66b1ff 100%
    );

  color: white;

  overflow: hidden;
}


.welcome-content {
  min-height: 150px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  padding: 10px 20px;
}


.welcome-left {
  display: flex;

  flex-direction: column;

  gap: 10px;
}


.welcome-title {
  font-size: 28px;

  font-weight: bold;
}


.welcome-subtitle {
  font-size: 18px;

  opacity: 0.95;
}


.welcome-tip {
  font-size: 14px;

  opacity: 0.8;
}


.welcome-icon {
  font-size: 90px;

  opacity: 0.18;

  margin-right: 50px;
}


/* =========================
   数据统计
========================= */

.statistics {
  display: grid;

  grid-template-columns:
    repeat(4, 1fr);

  gap: 20px;

  margin-bottom: 20px;
}


.stat-card {
  border: none;

  transition:
    all 0.2s;
}


.stat-card:hover {
  transform:
    translateY(-3px);

  box-shadow:
    0 6px 20px
    rgba(
      0,
      0,
      0,
      0.08
    );
}


.clickable {
  cursor: pointer;
}


.stat-content {
  display: flex;

  align-items: center;

  gap: 18px;
}


.stat-icon {
  width: 60px;

  height: 60px;

  border-radius: 12px;

  display: flex;

  align-items: center;

  justify-content: center;

  font-size: 30px;
}


.stat-icon.warning {
  background: #fdf6ec;

  color: #e6a23c;
}


.stat-icon.primary {
  background: #ecf5ff;

  color: #409eff;
}


.stat-icon.success {
  background: #f0f9eb;

  color: #67c23a;
}


.stat-icon.info {
  background: #f4f4f5;

  color: #909399;
}


.stat-label {
  font-size: 14px;

  color: #909399;

  margin-bottom: 5px;
}


.stat-number {
  font-size: 30px;

  font-weight: bold;

  color: #303133;
}


.stat-desc {
  font-size: 12px;

  color: #c0c4cc;

  margin-top: 2px;
}


/* =========================
   下方区域
========================= */

.content-grid {
  display: grid;

  grid-template-columns:
    2fr 1fr;

  gap: 20px;
}


.panel {
  border: none;
}


.panel-header {
  display: flex;

  align-items: center;

  justify-content: space-between;

  font-size: 17px;

  font-weight: bold;
}


.panel-title {
  font-size: 17px;

  font-weight: bold;
}


/* =========================
   分数
========================= */

.score {
  color: #67c23a;

  font-weight: bold;
}


.score.minus {
  color: #f56c6c;
}


/* =========================
   快捷操作
========================= */

.quick-actions {
  display: flex;

  flex-direction: column;

  gap: 5px;
}


.quick-item {
  display: flex;

  align-items: center;

  gap: 15px;

  padding: 15px;

  border-radius: 8px;

  cursor: pointer;

  transition:
    all 0.2s;
}


.quick-item:hover {
  background: #f5f7fa;

  transform:
    translateX(3px);
}


.quick-icon {
  width: 45px;

  height: 45px;

  border-radius: 10px;

  display: flex;

  align-items: center;

  justify-content: center;

  font-size: 22px;
}


.quick-icon.audit {
  background: #ecf5ff;

  color: #409eff;
}


.quick-icon.student {
  background: #f0f9eb;

  color: #67c23a;
}


.quick-icon.activity {
  background: #fdf6ec;

  color: #e6a23c;
}


.quick-icon.score {
  background: #f4f4f5;

  color: #909399;
}


.quick-title {
  font-size: 15px;

  font-weight: bold;

  color: #303133;
}


.quick-desc {
  font-size: 12px;

  color: #909399;

  margin-top: 4px;
}


/* =========================
   响应式
========================= */

@media (max-width: 1100px) {

  .statistics {
    grid-template-columns:
      repeat(2, 1fr);
  }


  .content-grid {
    grid-template-columns:
      1fr;
  }

}


@media (max-width: 700px) {

  .statistics {
    grid-template-columns:
      1fr;
  }


  .welcome-title {
    font-size: 22px;
  }


  .welcome-icon {
    display: none;
  }

}

</style>
