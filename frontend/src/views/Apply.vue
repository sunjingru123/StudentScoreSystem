<template>
  <div class="apply-page">
    <el-card>
      <template #header>
        <div class="card-title">
          <span>加分申请</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <!-- 申请项目 -->
        <el-form-item label="申请项目" prop="ruleName">
          <el-select v-model="form.ruleName" placeholder="请选择申请项目" style="width: 300px">
            <el-option label="竞赛获奖" value="竞赛获奖" />
            <el-option label="社会实践" value="社会实践" />
            <el-option label="志愿服务" value="志愿服务" />
            <el-option label="其他加分" value="其他加分" />
          </el-select>
        </el-form-item>

        <!-- 申请分数 -->
        <el-form-item label="申请分数" prop="score">
          <el-input-number v-model="form.score" :min="1" :max="20" />
          <span class="unit"> 分 </span>
        </el-form-item>

        <!-- 申请说明 -->
        <el-form-item label="申请说明" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="请详细说明你的加分事由"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <!-- 提交按钮 -->
        <el-form-item>
          <el-button type="primary" @click="submitApply"> 提交申请 </el-button>
          <el-button @click="resetForm"> 重置 </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { addApply } from '@/api/apply'

const formRef = ref()

// 只定义一次form
const form = reactive({
  ruleName: '',
  score: 5,
  description: '',
})

// 名称映射ruleId
const ruleMap = {
  竞赛获奖: 1,
  社会实践: 2,
  志愿服务: 3,
  其他加分: 4,
}

const rules = {
  ruleName: [
    {
      required: true,
      message: '请选择申请项目',
      trigger: 'change',
    },
  ],
  score: [
    {
      required: true,
      message: '请输入申请分数',
      trigger: 'change',
    },
  ],
  description: [
    {
      required: true,
      message: '请输入申请说明',
      trigger: 'blur',
    },
    {
      min: 5,
      message: '申请说明至少填写5个字',
      trigger: 'blur',
    },
  ],
}

function submitApply() {
  formRef.value.validate((valid) => {
    if (!valid) {
      return
    }
    const user = JSON.parse(localStorage.getItem('user'))

    const data = {
      studentId: user.id,
      activityId: 1,
      ruleId: 1,
      applyScore: form.score,
      materialFile: '',
      description: form.description,
    }
    addApply(data)
      .then((res) => {
        console.log('接口返回：', res)
        ElMessage.success('申请提交成功！')
        resetForm()
      })
      .catch((err) => {
        console.log(err)
        ElMessage.error('提交失败')
      })
  })
}

function resetForm() {
  formRef.value.resetFields()
}
</script>

<style scoped>
.apply-page {
  padding: 30px;
}
.card-title {
  font-size: 20px;
  font-weight: bold;
}
.el-form {
  max-width: 700px;
}
.unit {
  margin-left: 10px;
}
</style>
