<template>
  <div class="adjust-page">
    <el-card>
      <template #header>
        <div class="header">
          <span>管理员成绩调整</span>
          <el-button type="primary" @click="openDialog">新增调整</el-button>
        </div>
      </template>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="studentName" label="学生姓名" />
        <el-table-column prop="studentNo" label="学号" />
        <el-table-column label="调整类型">
          <template #default="scope">
            <el-tag v-if="scope.row.adjustType === 1" type="success">加分</el-tag>
            <el-tag v-if="scope.row.adjustType === -1" type="danger">减分</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="score" label="调整分数" />
        <el-table-column prop="reason" label="调整原因" />
        <el-table-column prop="adminName" label="操作管理员" />
        <el-table-column prop="createTime" label="操作时间" />
      </el-table>
    </el-card>

    <!--新增弹窗-->
    <el-dialog v-model="dialogVisible" title="成绩调整" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="学生">
          <el-select v-model="form.studentId" placeholder="请选择学生" filterable clearable>
            <el-option
              v-for="s in studentList"
              :key="s.id"
              :label="`${s.realName}(${s.studentNo})`"
              :value="s.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调整类型">
          <el-radio-group v-model="form.adjustType">
            <el-radio :label="1">加分</el-radio>
            <el-radio :label="-1">减分</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分数">
          <el-input-number v-model="form.score" :min="0.01" :step="0.5" />
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input v-model="form.reason" type="textarea" rows="3" placeholder="填写调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确认调整</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
// 改成你项目里真实的request路径，看别的页面抄
import request from '@/utils/request'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const studentList = ref([])

const form = ref({
  studentId: null,
  adjustType: 1,
  score: 0,
  reason: '',
})

function openDialog() {
  form.value = { studentId: null, adjustType: 1, score: 0, reason: '' }
  dialogVisible.value = true
}

async function loadStudent() {
  const res = await request.get('/user/student/list')
  studentList.value = res.data.data || []
}

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/admin/scoreAdjustment/list')
    list.value = res.data.data
  } catch (e) {
    ElMessage.error('获取调整记录失败')
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!form.value.studentId) return ElMessage.warning('请选择学生')
  if (!form.value.score) return ElMessage.warning('请填写分数')
  try {
    await request.post('/admin/scoreAdjustment/add', form.value)
    ElMessage.success('调整成功')
    dialogVisible.value = false
    loadList()
  } catch (err) {
    ElMessage.error(err.response?.data?.msg || '操作失败')
  }
}

onMounted(() => {
  loadStudent()
  loadList()
})
</script>

<style scoped>
.adjust-page {
  padding: 30px;
}
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
