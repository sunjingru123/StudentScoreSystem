<template>
  <div class="archive-page">

    <!-- =========================
         页面头部
    ========================== -->
    <el-card shadow="never">

      <template #header>

        <div class="page-header">

          <div>

            <h2>
              学生加减分档案
            </h2>

            <p>
              查看、管理和导出学生综合测评加减分档案
            </p>

          </div>


          <div class="header-actions">

            <el-button
              :loading="loading"
              @click="loadList"
            >
              <el-icon>
                <Refresh />
              </el-icon>

              刷新
            </el-button>


            <!-- =====================
                 导出
            ====================== -->
            <el-button
              type="primary"
              :loading="exporting"
              @click="handleExport"
            >

              <el-icon>
                <Download />
              </el-icon>

              导出档案

            </el-button>

          </div>

        </div>

      </template>


      <!-- =========================
           查询区域
      ========================== -->

      <div class="search-area">

        <el-input
          v-model="searchForm.studentName"
          placeholder="请输入学生姓名"
          clearable
          style="width: 180px"
          @keyup.enter="loadList"
        />


        <el-input
          v-model="searchForm.studentNo"
          placeholder="请输入学号"
          clearable
          style="width: 180px"
          @keyup.enter="loadList"
        />


        <el-select
          v-model="searchForm.scoreType"
          placeholder="加减分类型"
          clearable
          style="width: 140px"
        >

          <el-option
            label="全部"
            :value="null"
          />

          <el-option
            label="加分"
            :value="1"
          />

          <el-option
            label="减分"
            :value="-1"
          />

        </el-select>


        <el-button
          type="primary"
          @click="loadList"
        >
          查询
        </el-button>


        <el-button
          @click="resetSearch"
        >
          重置
        </el-button>

      </div>


      <!-- =========================
           数据表格
      ========================== -->

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        style="width: 100%"
      >

        <!-- 序号 -->

        <el-table-column
          type="index"
          label="#"
          width="60"
          align="center"
        />


        <!-- 学生 -->

        <el-table-column
          prop="studentName"
          label="学生"
          min-width="120"
        />


        <!-- 学号 -->

        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="140"
        />


        <!-- 部门 -->

        <el-table-column
          prop="departmentName"
          label="申报部门"
          min-width="130"
        />


        <!-- 项目 -->

        <el-table-column
          prop="title"
          label="加减分项目"
          min-width="180"
          show-overflow-tooltip
        />


        <!-- 类型 -->

        <el-table-column
          label="类型"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="Number(row.scoreType) === 1"
              type="success"
            >
              加分
            </el-tag>


            <el-tag
              v-else-if="Number(row.scoreType) === -1"
              type="danger"
            >
              减分
            </el-tag>


            <el-tag
              v-else
              type="info"
            >
              未知
            </el-tag>

          </template>

        </el-table-column>


        <!-- 分值 -->

        <el-table-column
          label="分值"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <span
              v-if="Number(row.scoreType) === 1"
              class="score-plus"
            >
              +{{ row.score }}
            </span>


            <span
              v-else-if="Number(row.scoreType) === -1"
              class="score-minus"
            >
              -{{ row.score }}
            </span>


            <span v-else>
              {{ row.score }}
            </span>

          </template>

        </el-table-column>


        <!-- 申请人 -->

        <el-table-column
          prop="applicantName"
          label="申报人"
          min-width="120"
        />


        <!-- 时间 -->

        <el-table-column
          prop="createTime"
          label="申请时间"
          width="180"
          align="center"
        />


        <!-- 状态 -->

        <el-table-column
          label="状态"
          width="110"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="row.status === 1"
              type="success"
            >
              已通过
            </el-tag>


            <el-tag
              v-else-if="row.status === 0"
              type="warning"
            >
              待审核
            </el-tag>


            <el-tag
              v-else
              type="danger"
            >
              已驳回
            </el-tag>

          </template>

        </el-table-column>


        <!-- 操作 -->

        <el-table-column
          label="操作"
          width="120"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              type="primary"
              link
              @click="viewDetail(row)"
            >
              查看
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
        description="暂无学生加减分档案"
      />

    </el-card>


    <!-- =========================
         查看详情
    ========================== -->

    <el-dialog
      v-model="detailVisible"
      title="加减分档案详情"
      width="600px"
    >

      <el-descriptions
        :column="2"
        border
      >

        <el-descriptions-item label="学生">
          {{ currentRow?.studentName || '—' }}
        </el-descriptions-item>


        <el-descriptions-item label="学号">
          {{ currentRow?.studentNo || '—' }}
        </el-descriptions-item>


        <el-descriptions-item label="部门">
          {{ currentRow?.departmentName || '—' }}
        </el-descriptions-item>


        <el-descriptions-item label="申报人">
          {{ currentRow?.applicantName || '—' }}
        </el-descriptions-item>


        <el-descriptions-item label="项目">
          {{ currentRow?.title || '—' }}
        </el-descriptions-item>


        <el-descriptions-item label="分值">

          <span
            v-if="Number(currentRow?.scoreType) === 1"
            class="score-plus"
          >
            +{{ currentRow?.score }}
          </span>

          <span
            v-else-if="Number(currentRow?.scoreType) === -1"
            class="score-minus"
          >
            -{{ currentRow?.score }}
          </span>

          <span v-else>
            {{ currentRow?.score || '—' }}
          </span>

        </el-descriptions-item>


        <el-descriptions-item
          label="申请时间"
          :span="2"
        >
          {{ currentRow?.createTime || '—' }}
        </el-descriptions-item>


        <el-descriptions-item
          label="申请说明"
          :span="2"
        >
          {{ currentRow?.description || '—' }}
        </el-descriptions-item>

      </el-descriptions>


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
  Refresh,
  Download
} from '@element-plus/icons-vue'

import request from '@/utils/request'


/* =========================================================
   数据
========================================================= */

const list = ref([])

const loading = ref(false)

const exporting = ref(false)

const detailVisible = ref(false)

const currentRow = ref(null)


/* =========================================================
   查询条件
========================================================= */

const searchForm = ref({

  studentName: '',

  studentNo: '',

  scoreType: null

})


/* =========================================================
   查询档案
========================================================= */

async function loadList() {

  loading.value = true

  try {

    const params = {}

    if (
      searchForm.value.studentName
    ) {

      params.studentName =
        searchForm.value.studentName.trim()

    }


    if (
      searchForm.value.studentNo
    ) {

      params.studentNo =
        searchForm.value.studentNo.trim()

    }


    if (
      searchForm.value.scoreType !== null &&
      searchForm.value.scoreType !== ''
    ) {

      params.scoreType =
        searchForm.value.scoreType

    }


    const res =
      await request.get(
        '/archive/score/list',
        {
          params
        }
      )


    if (
      res.data?.code !== 200
    ) {

      ElMessage.error(
        res.data?.message ||
        res.data?.msg ||
        '获取档案失败'
      )

      list.value = []

      return

    }


    const data =
      res.data?.data


    if (Array.isArray(data)) {

      list.value = data

    }

    else if (
      data &&
      Array.isArray(data.records)
    ) {

      list.value =
        data.records

    }

    else {

      list.value = []

    }

  }

  catch (error) {

    console.error(
      '获取学生档案失败：',
      error
    )

    list.value = []

    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取学生档案失败'
    )

  }

  finally {

    loading.value = false

  }

}


/* =========================================================
   重置
========================================================= */

function resetSearch() {

  searchForm.value = {

    studentName: '',

    studentNo: '',

    scoreType: null

  }

  loadList()

}


/* =========================================================
   查看详情
========================================================= */

function viewDetail(row) {

  currentRow.value = row

  detailVisible.value = true

}


/* =========================================================
   导出
========================================================= */

async function handleExport() {

  if (exporting.value) {

    return

  }


  exporting.value = true

  try {

    const params = {}

    if (
      searchForm.value.studentName
    ) {

      params.studentName =
        searchForm.value.studentName.trim()

    }


    if (
      searchForm.value.studentNo
    ) {

      params.studentNo =
        searchForm.value.studentNo.trim()

    }


    if (
      searchForm.value.scoreType !== null &&
      searchForm.value.scoreType !== ''
    ) {

      params.scoreType =
        searchForm.value.scoreType

    }


    const res =
      await request.get(
        '/archive/score/export',
        {
          params,
          responseType: 'blob'
        }
      )


    const blob =
      new Blob(
        [res.data],
        {
          type:
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        }
      )


    const url =
      window.URL.createObjectURL(blob)


    const link =
      document.createElement('a')


    link.href = url


    link.download =
      `学生加减分档案_${getDateString()}.xlsx`


    document.body.appendChild(link)

    link.click()

    document.body.removeChild(link)


    window.URL.revokeObjectURL(url)


    ElMessage.success(
      '档案导出成功'
    )

  }

  catch (error) {

    console.error(
      '档案导出失败：',
      error
    )

    ElMessage.error(
      '档案导出失败，请检查后端接口'
    )

  }

  finally {

    exporting.value = false

  }

}


/* =========================================================
   日期
========================================================= */

function getDateString() {

  const date =
    new Date()

  const year =
    date.getFullYear()

  const month =
    String(
      date.getMonth() + 1
    ).padStart(2, '0')

  const day =
    String(
      date.getDate()
    ).padStart(2, '0')

  return `${year}${month}${day}`

}


/* =========================================================
   初始化
========================================================= */

onMounted(() => {

  loadList()

})

</script>


<style scoped>

.archive-page {

  width: 100%;

  min-height:
    calc(100vh - 60px);

  padding: 30px;

  box-sizing: border-box;

  background:
    #f5f7fa;

}


.page-header {

  display: flex;

  justify-content:
    space-between;

  align-items:
    center;

}


.page-header h2 {

  margin:
    0 0 8px;

  font-size:
    24px;

  color:
    #303133;

}


.page-header p {

  margin: 0;

  color:
    #909399;

  font-size:
    14px;

}


.header-actions {

  display: flex;

  align-items:
    center;

  gap:
    12px;

}


.search-area {

  display: flex;

  align-items:
    center;

  gap:
    12px;

  margin-bottom:
    20px;

  padding:
    18px;

  background:
    #f8fafc;

  border-radius:
    8px;

}


.score-plus {

  color:
    #67c23a;

  font-weight:
    bold;

}


.score-minus {

  color:
    #f56c6c;

  font-weight:
    bold;

}


.el-empty {

  padding:
    50px 0;

}


@media (max-width: 900px) {

  .page-header {

    flex-direction:
      column;

    align-items:
      stretch;

    gap:
      15px;

  }


  .header-actions {

    justify-content:
      flex-end;

  }


  .search-area {

    flex-wrap:
      wrap;

  }

}

</style>
