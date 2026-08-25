<template>

  <div class="audit">

    <el-card>

      <template #header>

        <div class="header">

          <span>
            加分申请审核
          </span>

          <el-button
            type="primary"
            :loading="loading"
            @click="load"
          >
            刷新
          </el-button>

        </div>

      </template>


      <!-- =========================
           审核列表
      ========================== -->

      <el-table
        :data="list"
        border
        stripe
        v-loading="loading"
        style="width: 100%"
      >

        <el-table-column
          type="index"
          label="#"
          width="70"
          :index="indexMethod"
        />


        <el-table-column
          prop="studentName"
          label="学生"
          min-width="120"
        />


        <el-table-column
          prop="studentNo"
          label="学号"
          min-width="140"
        />


        <el-table-column
          prop="ruleName"
          label="项目"
          min-width="180"
        />


        <el-table-column
          prop="applyScore"
          label="分数"
          width="100"
        />


        <el-table-column
          label="状态"
          width="110"
        >

          <template #default="scope">

            <el-tag
              v-if="scope.row.status === 0"
              type="warning"
            >
              待审核
            </el-tag>


            <el-tag
              v-else-if="scope.row.status === 1"
              type="success"
            >
              已通过
            </el-tag>


            <el-tag
              v-else-if="scope.row.status === 2"
              type="danger"
            >
              已拒绝
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
          prop="createTime"
          label="申请时间"
          min-width="180"
        />


        <el-table-column
          label="操作"
          min-width="180"
          fixed="right"
        >

          <template #default="scope">

            <template
              v-if="scope.row.status === 0"
            >

              <el-button
                type="success"
                size="small"
                :loading="auditLoadingId === scope.row.id"
                @click="
                  audit(
                    scope.row.id,
                    1
                  )
                "
              >
                通过
              </el-button>


              <el-button
                type="danger"
                size="small"
                :loading="auditLoadingId === scope.row.id"
                @click="
                  audit(
                    scope.row.id,
                    2
                  )
                "
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


      <!-- =========================
           分页
      ========================== -->

      <div class="pagination-wrapper">

        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
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
  ElMessage,
  ElMessageBox
} from 'element-plus'

import request from '@/utils/request'


/*
 * =========================================================
 * 数据
 * =========================================================
 */

const list = ref([])

const loading = ref(false)


/*
 * =========================================================
 * 分页
 * =========================================================
 */

const page = ref(1)

const pageSize = ref(10)

const total = ref(0)


/*
 * =========================================================
 * 当前正在审核的 ID
 * =========================================================
 */

const auditLoadingId = ref(null)


/*
 * =========================================================
 * 表格序号
 * =========================================================
 */

function indexMethod(index) {

  return (
    (page.value - 1) *
    pageSize.value +
    index +
    1
  )

}


/*
 * =========================================================
 * 加载待审核数据
 * =========================================================
 */

async function load() {

  loading.value = true

  try {

    const res =
      await request.get(
        '/scoreApply/pending',
        {
          params: {
            page: page.value,
            pageSize: pageSize.value
          }
        }
      )


    console.log(
      '加分申请分页数据：',
      res
    )


    const data =
      res.data.data


    /*
     * 正常分页结构
     */
    if (
      data &&
      Array.isArray(data.records)
    ) {

      list.value =
        data.records

      total.value =
        Number(data.total || 0)

    } else {

      /*
       * 兼容旧接口
       *
       * 如果后端暂时还是返回数组，
       * 页面仍然可以显示。
       */
      if (
        Array.isArray(data)
      ) {

        list.value = data

        total.value =
          data.length

      } else {

        list.value = []

        total.value = 0

      }

    }

  } catch (e) {

    console.error(
      '获取加分申请失败',
      e
    )

    ElMessage.error(
      '获取加分申请失败'
    )

  } finally {

    loading.value = false

  }

}


/*
 * =========================================================
 * 每页数量变化
 * =========================================================
 */

function handleSizeChange(size) {

  pageSize.value = size

  page.value = 1

  load()

}


/*
 * =========================================================
 * 页码变化
 * =========================================================
 */

function handleCurrentChange(current) {

  page.value = current

  load()

}


/*
 * =========================================================
 * 审核
 *
 * status：
 *
 * 1 = 通过
 * 2 = 拒绝
 * =========================================================
 */

async function audit(
  id,
  status
) {

  if (!id) {

    ElMessage.error(
      '申请记录不存在'
    )

    return

  }


  /*
   * 二次确认
   */
  try {

    await ElMessageBox.confirm(
      status === 1
        ? '确定通过这条加分申请吗？'
        : '确定拒绝这条加分申请吗？',
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


  auditLoadingId.value = id


  try {

    await request.post(
      '/scoreApply/audit',
      {
        id: id,
        status: status
      }
    )


    ElMessage.success(
      status === 1
        ? '审核通过'
        : '已拒绝'
    )


    /*
     * 重新加载当前页
     */
    await load()


    /*
     * 如果当前页最后一条被处理掉，
     * 当前页可能变成空页。
     *
     * 自动回到上一页。
     */
    if (
      list.value.length === 0
      && page.value > 1
    ) {

      page.value--

      await load()

    }

  } catch (err) {

    console.error(
      '审核失败',
      err
    )

    ElMessage.error(
      err.response?.data?.msg
      ||
      err.response?.data?.message
      ||
      '审核失败'
    )

  } finally {

    auditLoadingId.value = null

  }

}


/*
 * =========================================================
 * 初始化
 * =========================================================
 */

onMounted(() => {

  load()

})

</script>


<style scoped>

.audit {
  padding: 30px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  padding-bottom: 10px;
}

.finished {
  color: #999;
}

</style>
