<template>
  <div class="login-page">

    <el-form
      class="login-form"
      :model="form"
      @submit.prevent
    >

      <h2>
        学生综合测评系统
      </h2>

      <el-form-item>
        <el-input
          v-model="form.username"
          placeholder="用户名"
          clearable
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <el-form-item>
        <el-input
          v-model="form.password"
          type="password"
          placeholder="密码"
          show-password
          @keyup.enter="handleLogin"
        />
      </el-form-item>

      <el-button
        type="primary"
        class="login-btn"
        :loading="loading"
        @click="handleLogin"
      >
        登录
      </el-button>

    </el-form>

  </div>
</template>

<script setup>

import {
  reactive,
  ref
} from 'vue'

import {
  useRouter
} from 'vue-router'

import {
  ElMessage
} from 'element-plus'

import request from '@/utils/request'


const router =
  useRouter()


const loading =
  ref(false)


const form =
  reactive({

    username: '',

    password: '',

  })


const handleLogin =
  async () => {

    if (!form.username.trim()) {

      ElMessage.warning(
        '请输入用户名'
      )

      return
    }


    if (!form.password) {

      ElMessage.warning(
        '请输入密码'
      )

      return
    }


    loading.value =
      true


    try {

      const res =
        await request.post(
          '/login',
          {
            username:
              form.username.trim(),

            password:
            form.password,
          }
        )


      if (res.code !== 200) {

        ElMessage.error(
          res.message ||
          '登录失败'
        )

        return
      }


      const user =
        res.data


      if (!user) {

        ElMessage.error(
          '登录信息异常'
        )

        return
      }


      // =====================================================
      // 保存用户信息
      // =====================================================

      localStorage.setItem(
        'user',
        JSON.stringify(user)
      )


      if (user.token) {

        localStorage.setItem(
          'token',
          user.token
        )
      }


      // =====================================================
      // 首次登录
      // =====================================================

      if (user.firstLogin === true) {

        ElMessage.warning(
          '这是你第一次登录，请先修改密码'
        )


        await router.push(
          '/change-password'
        )

        return
      }


      // =====================================================
      // 正常登录
      // =====================================================

      if (user.role === '管理员') {

        await router.push(
          '/admin/adminHome'
        )

        return
      }


      if (user.role === '辅导员') {

        await router.push(
          '/teacher'
        )

        return
      }


      // 学生、部长、副部长、干事
      await router.push(
        '/home'
      )

    } catch (error) {

      console.error(
        '登录失败：',
        error
      )


      ElMessage.error(
        error?.response?.data?.message
        ||
        error?.message
        ||
        '请求异常，请稍后重试'
      )

    } finally {

      loading.value =
        false
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

  box-shadow:
    0 8px 24px
    rgba(0, 0, 0, 0.08);

}


.login-form h2 {

  text-align: center;

  margin-bottom: 24px;

}


.login-btn {

  width: 100%;

}

</style>
