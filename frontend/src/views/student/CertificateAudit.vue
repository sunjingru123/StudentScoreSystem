<template>

  <div class="certificate-page">

    <el-card shadow="never">

      <template #header>

        <div class="header">

          <div>

            <h2>
              个人证书审核
            </h2>

            <p>
              审核学生自主提交的证书、奖状等加分材料
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
          prop="title"
          label="证书/获奖项目"
          min-width="180"
        />


        <el-table-column
          prop="score"
          label="申请分值"
          width="100"
          align="center"
        />


        <el-table-column
          prop="description"
          label="申请说明"
          min-width="220"
          show-overflow-tooltip
        />


        <el-table-column
          prop="createTime"
          label="提交时间"
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
          label="材料"
          width="100"
          align="center"
        >

          <template #default="{ row }">

            <el-button
              v-if="row.fileUrl"
              type="primary"
              link
              @click="openFile(row.fileUrl)"
            >
              查看材料
            </el-button>

            <span
              v-else
              class="no-file"
            >
              无材料
            </span>

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
        description="暂无待审核的个人证书"
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
// 获取个人证书审核列表
// =========================================================

async function load() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/certificate/pending'
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
        '获取证书审核数据失败'
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
      '获取证书审核数据失败',
      error
    )

    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '获取证书审核数据失败'
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
        ? '确定通过这份个人证书加分申请吗？'
        : '确定拒绝这份个人证书加分申请吗？',

      '审核确认',

      {

        confirmButtonText:
          '确定',

        cancelButtonText:
          '取消',

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
      '/certificate/audit',
      {

        id:
        row.id,

        status:
        status

      }
    )


    ElMessage.success(

      status === 1
        ? '证书审核通过'
        : '证书审核已拒绝'

    )


    await load()


  } catch (error) {

    console.error(
      '证书审核失败',
      error
    )

    ElMessage.error(
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      '证书审核失败'
    )

  }

}


// =========================================================
// 查看材料
// =========================================================

function openFile(url) {

  if (!url) {

    return

  }

  window.open(
    url,
    '_blank'
  )

}


onMounted(() => {

  load()

})

</script>


<style scoped>

.certificate-page {

  padding:
    30px;

}


.header {

  display:
    flex;

  justify-content:
    space-between;

  align-items:
    center;

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


.finished {

  color:
    #999;

}


.no-file {

  color:
    #c0c4cc;

}

</style>
