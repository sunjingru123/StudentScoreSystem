<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>消息通知</span>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>

      <el-timeline v-if="list.length > 0">
        <el-timeline-item v-for="item in list" :key="item.id" :timestamp="item.createTime">
          <div class="message-title">
            {{ item.title }}
          </div>

          <div class="message-content">
            {{ item.content }}
          </div>
        </el-timeline-item>
      </el-timeline>

      <el-empty v-else description="暂无消息通知" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref([])

function load() {
  request
    .get('/message/list')
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

.message-title {
  font-size: 17px;
  font-weight: bold;
  margin-bottom: 5px;
}

.message-content {
  color: #606266;
}
</style>
