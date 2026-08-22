<template>
  <div class="audit">
    <el-card>
      <h2>加分申请审核</h2>

      <el-table :data="list">
        <el-table-column prop="studentName" label="学生" />

        <el-table-column prop="ruleName" label="项目" />

        <el-table-column prop="applyScore" label="分数" />

        <el-table-column label="状态">
          <template #default="scope">
            <el-tag v-if="scope.row.status === 0" type="warning"> 待审核 </el-tag>

            <el-tag v-if="scope.row.status === 1" type="success"> 已通过 </el-tag>

            <el-tag v-if="scope.row.status === 2" type="danger"> 已拒绝 </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="success" @click="audit(scope.row.id, 1)"> 通过 </el-button>

            <el-button type="danger" @click="audit(scope.row.id, 2)"> 拒绝 </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

import request from '@/api/request'

import { ElMessage } from 'element-plus'

const list = ref([])

function load() {
  request
    .get('/scoreApply/pending')

    .then((res) => {

      console.log(res)

      list.value = res.data.data
    })
}

function audit(id, status) {
  request
    .post('/scoreApply/audit', {
      id: id,

      status: status,
    })

    .then(() => {
      ElMessage.success('操作成功')

      load()
    })
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.audit {
  padding: 30px;
}
</style>
