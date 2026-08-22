<template>
  <div class="record">
    <el-card>
      <template #header> 我的申请记录 </template>

      <el-table :data="list" style="width: 100%">
        <el-table-column prop="ruleName" label="项目" />

        <el-table-column prop="applyScore" label="申请分数" />

        <el-table-column label="状态">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning"> 待审核 </el-tag>

            <el-tag v-if="scope.row.status === 1" type="success"> 已通过 </el-tag>

            <el-tag v-if="scope.row.status === 2" type="danger"> 已拒绝 </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="申请时间" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

import request from '@/api/request'

const list = ref([])

onMounted(() => {
  request.get('/scoreApply/my').then((res) => {
    console.log('我的申请记录：', res)

    list.value = res.data.data
  })
})
</script>

<style scoped>
.record {
  padding: 30px;
}
</style>
