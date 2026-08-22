<template>
  <div class="page">
    <el-card>
      <template #header>
        <div class="header">
          <span>学生成绩</span>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>

      <el-table :data="list" border stripe>
        <el-table-column prop="studentName" label="学生" />
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column prop="className" label="班级" />
        <el-table-column prop="totalScore" label="综合评分" />
      </el-table>

      <el-empty v-if="list.length === 0" description="暂无成绩数据" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const list = ref([])

async function load() {
  try {
    const studentRes = await request.get('/user/student/list')
    const students = studentRes.data?.data ?? studentRes.data ?? []

    const rows = []
    for (const student of Array.isArray(students) ? students : []) {
      try {
        const scoreRes = await request.get(`/scoreStatistics/${student.id}`)
        const data = scoreRes.data?.data ?? scoreRes.data ?? {}
        rows.push({
          id: student.id,
          studentNo: student.studentNo,
          realName: student.realName,
          className: student.className,
          totalScore: data.totalScore ?? 0,
        })
      } catch (error) {
        rows.push({
          id: student.id,
          studentNo: student.studentNo,
          realName: student.realName,
          className: student.className,
          totalScore: 0,
        })
      }
    }
    list.value = rows
  } catch (error) {
    list.value = []
  }
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
