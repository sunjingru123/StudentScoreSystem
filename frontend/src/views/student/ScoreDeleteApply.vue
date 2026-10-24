<template>
  <div class="delete-apply-page">

    <div class="page-header">

      <div>
        <h2>我的删除申请</h2>

        <p>
          档案部部长 / 副部长提交的学生成绩删除申请，需经辅导员审核通过后才会删除
        </p>
      </div>

    </div>


    <el-card class="apply-card">

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        empty-text="暂无删除申请"
      >

        <el-table-column prop="studentName" label="学生" width="110" />

        <el-table-column prop="studentNo" label="学号" width="140" />

        <el-table-column prop="className" label="班级" width="130" />

        <el-table-column prop="projectName" label="加减分项目" min-width="160" show-overflow-tooltip />

        <el-table-column prop="score" label="分值" width="90" />

        <el-table-column prop="reason" label="删除原因" min-width="180" show-overflow-tooltip />

        <el-table-column prop="statusText" label="状态" width="100" />

        <el-table-column prop="reviewerName" label="审核人" width="110" />

        <el-table-column prop="reviewRemark" label="审核意见" min-width="150" show-overflow-tooltip />

        <el-table-column prop="reviewTime" label="审核时间" width="170" />

        <el-table-column prop="createTime" label="申请时间" width="170" />

      </el-table>

    </el-card>

  </div>
</template>


<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { getMyDeleteApplies } from '@/api/scoreDeleteApply'


const loading = ref(false)

const list = ref([])


function unwrap(res) {

  return res?.data?.code !== undefined
    ? res.data
    : res

}


async function loadList() {

  loading.value = true

  try {

    const result = unwrap(await getMyDeleteApplies())

    if (result?.code === 200 || result?.code === 0) {

      list.value = result.data || []

    } else {

      ElMessage.error(result?.message || '获取删除申请失败')

    }

  } catch (error) {

    console.error('获取删除申请失败：', error)

    ElMessage.error('获取删除申请失败')

  } finally {

    loading.value = false

  }

}


onMounted(() => {

  loadList()

})
</script>


<style scoped>

.delete-apply-page {

  padding: 24px;

  background: #f5f7fa;

  min-height: calc(100vh - 120px);

}


.page-header {

  margin-bottom: 20px;

}


.page-header h2 {

  margin: 0 0 8px;

  font-size: 26px;

  color: #303133;

}


.page-header p {

  margin: 0;

  color: #909399;

  font-size: 14px;

}


.apply-card {

  border-radius: 12px;

}

</style>
