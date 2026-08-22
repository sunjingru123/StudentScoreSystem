<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>活动管理</span>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>

      <el-table :data="list" border stripe>
        <el-table-column prop="name" label="活动名称" />
        <el-table-column prop="description" label="活动说明" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" />
      </el-table>

      <el-empty v-if="list.length === 0" description="暂无活动" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref([])

function load() {
  request
    .get('/activity/list')
    .then((res) => {
      if (res.data.code === 200) {
        list.value = res.data.data || []
      }
    })
    .catch(() => {
      list.value = []
    })
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.page {
  padding: 10px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 20px;
  font-weight: bold;
}
</style>
