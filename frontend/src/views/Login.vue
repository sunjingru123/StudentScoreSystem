<template>
  <div class="login-page">
    <el-form class="login-form" :model="form">
      <h2>学生综合测评系统</h2>

      <el-form-item>
        <el-input v-model="form.username" placeholder="用户名" />
      </el-form-item>

      <el-form-item>
        <el-input v-model="form.password" type="password" placeholder="密码" show-password />
      </el-form-item>

      <el-button type="primary" class="login-btn" @click="handleLogin">登录</el-button>
    </el-form>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/login'

const router = useRouter()

const form = reactive({
  username: '',
  password: '',
})

const handleLogin = async () => {
  try {
    const res = await login({
      username: form.username,
      password: form.password,
    })

    const code = res?.data?.code
    const user = res?.data?.data

    if (code === 200) {
      if (user) {
        localStorage.setItem('user', JSON.stringify(user))
        if (user.token) {
          localStorage.setItem('token', user.token)
        }
      }

      router.push('/admin/adminHome')
      return
    }

    ElMessage.error(res?.data?.message || '登录失败')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '请求异常，请稍后重试')
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f5f7fa;
}

.login-form {
  width: 360px;
  padding: 32px 24px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.login-form h2 {
  text-align: center;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
}
</style>
