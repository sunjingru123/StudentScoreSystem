<template>

  <div class="audit-page">

    <el-card shadow="never">

      <template #header>

        <div class="header">

          <div>

            <h2>
              部门申报审核
            </h2>

            <p>
              审核本部门干事提交的加减分申请
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

      </template>


      <el-table
        :data="list"
        v-loading="loading"
        border
        stripe
      >

        <el-table-column
          type="index"
          label="#"
          width="60"
        />


        <el-table-column
          prop="studentName"
          label="学生"
          min-width="110"
        />


        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="130"
        />


        <el-table-column
          prop="departmentName"
          label="部门"
          min-width="130"
        />


        <el-table-column
          prop="title"
          label="项目"
          min-width="180"
          show-overflow-tooltip
        />


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


        <el-table-column
          label="分值"
          width="90"
          align="center"
        >

          <template #default="{ row }">

            <span
              v-if="Number(row.scoreType) === 1"
              class="plus"
            >
              +{{ row.score }}
            </span>

            <span
              v-else
              class="minus"
            >
              -{{ row.score }}
            </span>

          </template>

        </el-table-column>


        <el-table-column
          prop="applicantName"
          label="申报人"
          min-width="110"
        />


        <el-table-column
          prop="createTime"
          label="申请时间"
          min-width="170"
        />


        <el-table-column
          label="状态"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <el-tag
              v-if="row.status === 0"
              type="warning"
            >
              待审核
            </el-tag>

            <el-tag
              v-else-if="row.status === 1"
              type="success"
            >
              已通过
            </el-tag>

            <el-tag
              v-else
              type="danger"
            >
              已拒绝
            </el-tag>

          </template>

        </el-table-column>


        <el-table-column
          label="操作"
          width="180"
          fixed="right"
          align="center"
        >

          <template #default="{ row }">

            <template
              v-if="row.status === 0"
            >

              <el-button
                type="success"
                link
                @click="audit(row, 1)"
              >
                通过
              </el-button>


              <el-button
                type="danger"
                link
                @click="audit(row, 2)"
              >
                拒绝
              </el-button>

            </template>


            <span
              v-else
              class="finished"
            >
              已处理
            </span>

          </template>

        </el-table-column>

      </el-table>


      <el-empty
        v-if="
          !loading &&
          list.length === 0
        "
        description="暂无待审核的部门申报"
      />

    </el-card>

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

import request from '@/utils/request'


const list =
  ref([])


const loading =
  ref(false)


// =========================================================
// 获取部门审核数据
// =========================================================

async function load() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/departmentScore/pending'
      )


    const result =
      res?.data?.code !== undefined
        ? res.data
        : res


    if (
      result?.code !== 200
      &&
      result?.code !== 0
    ) {

      ElMessage.error(
        result?.message ||
        '获取部门申报失败'
      )

      list.value = []

      return

    }


    const data =
      result?.data


    list.value =
      Array.isArray(data)
        ? data
        : data?.records || []


  } catch (error) {

    console.error(
      '获取部门审核数据失败',
      error
    )

    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取部门审核数据失败'
    )

  } finally {

    loading.value = false

  }

}


// =========================================================
// 审核
// =========================================================

async function audit(
  row,
  status
) {

  try {

    await ElMessageBox.confirm(

      status === 1
        ? '确定通过这条部门加减分申报吗？'
        : '确定拒绝这条部门加减分申报吗？',

      '审核确认',

      {
        confirmButtonText: '确定',

        cancelButtonText: '取消',

        type:
          status === 1
            ? 'success'
            : 'warning'
      }

    )

  } catch {

    return

  }


  try {

    await request.post(
      '/departmentScore/audit',
      {

        id:
        row.id,

        status:
        status

      }
    )


    ElMessage.success(
      status === 1
        ? '审核通过'
        : '已拒绝'
    )


    await load()


  } catch (error) {

    console.error(
      '部门申报审核失败',
      error
    )

    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '审核失败'
    )

  }

}


onMounted(() => {

  load()

})

</script>


<style scoped>

.audit-page {

  padding:
    30px;

}


.header {

  display:
    flex;

  align-items:
    center;

  justify-content:
    space-between;

}


.header h2 {

  margin:
    0 0 8px;

}


.header p {

  margin:
    0;

  color:
    #909399;

}


.plus {

  color:
    #67c23a;

  font-weight:
    bold;

}


.minus {

  color:
    #f56c6c;

  font-weight:
    bold;

}


.finished {

  color:
    #999;

}

</style>
