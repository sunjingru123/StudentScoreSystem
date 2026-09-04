<template>

  <div class="password-page">

    <el-card
      class="password-card"
      shadow="always"
    >

      <template #header>

        <div class="card-header">

          <span>
            {{ isFirstLogin
            ? '首次登录，请修改密码'
            : '修改密码'
            }}
          </span>

        </div>

      </template>


      <el-alert
        v-if="isFirstLogin"
        title="为了账号安全，首次登录必须修改初始密码"
        type="warning"
        :closable="false"
        show-icon
        class="notice"
      />


      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >

        <el-form-item
          label="原密码"
          prop="oldPassword"
        >

          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
          />

        </el-form-item>


        <el-form-item
          label="新密码"
          prop="newPassword"
        >

          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />

        </el-form-item>


        <el-form-item
          label="确认密码"
          prop="confirmPassword"
        >

          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />

        </el-form-item>


        <el-form-item>

          <el-button
            type="primary"
            :loading="loading"
            @click="handleSubmit"
          >
            {{ isFirstLogin
            ? '修改密码并进入系统'
            : '确认修改'
            }}
          </el-button>

          <el-button
            v-if="!isFirstLogin"
            @click="goBack"
          >
            返回
          </el-button>

        </el-form-item>

      </el-form>

    </el-card>

  </div>

</template>


<script setup>

import {
  computed,
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


const formRef =
  ref()


const loading =
  ref(false)


const form =
  reactive({

    oldPassword: '',

    newPassword: '',

    confirmPassword: '',

  })


const user =
  computed(() => {

    const userStr =
      localStorage.getItem('user')

    if (!userStr) {

      return null
    }

    try {

      return JSON.parse(
        userStr
      )

    } catch {

      return null
    }

  })


const isFirstLogin =
  computed(() => {

    return (
      user.value?.firstLogin === true
      ||
      user.value?.firstLogin === 1
    )

  })


const validateConfirmPassword =
  (
    rule,
    value,
    callback
  ) => {

    if (!value) {

      callback(
        new Error(
          '请确认新密码'
        )
      )

      return
    }


    if (
      value !==
      form.newPassword
    ) {

      callback(
        new Error(
          '两次输入的新密码不一致'
        )
      )

      return
    }


    callback()

  }


const rules = {

  oldPassword: [

    {
      required: true,

      message:
        '请输入原密码',

      trigger:
        'blur',

    },

  ],


  newPassword: [

    {
      required: true,

      message:
        '请输入新密码',

      trigger:
        'blur',

    },

    {

      min: 6,

      max: 50,

      message:
        '密码长度为6-50位',

      trigger:
        'blur',

    },

  ],


  confirmPassword: [

    {
      validator:
      validateConfirmPassword,

      trigger:
        'blur',

    },

  ],

}


const handleSubmit =
  async () => {

    if (!formRef.value) {

      return
    }


    try {

      await formRef.value.validate()

    } catch {

      return
    }


    loading.value =
      true


    try {

      const res =
        await request.post(
          '/user/password/change',
          {
            oldPassword:
            form.oldPassword,

            newPassword:
            form.newPassword,

            confirmPassword:
            form.confirmPassword,
          }
        )


      if (res.code !== 200) {

        ElMessage.error(
          res.message ||
          '密码修改失败'
        )

        return
      }


      ElMessage.success(
        '密码修改成功'
      )


      // =====================================================
      // 更新本地用户状态
      // =====================================================

      const currentUser =
        user.value


      if (currentUser) {

        currentUser.firstLogin =
          false

        localStorage.setItem(
          'user',
          JSON.stringify(
            currentUser
          )
        )
      }


      // =====================================================
      // 修改成功后根据角色进入系统
      // =====================================================

      const role =
        currentUser?.role


      if (role === '管理员') {

        await router.replace(
          '/admin/adminHome'
        )

        return
      }


      if (role === '辅导员') {

        await router.replace(
          '/teacher'
        )

        return
      }


      await router.replace(
        '/home'
      )

    } catch (error) {

      console.error(
        '修改密码失败：',
        error
      )


      ElMessage.error(
        error?.response?.data?.message
        ||
        '请求异常，请稍后重试'
      )

    } finally {

      loading.value =
        false
    }

  }


const goBack =
  () => {

    if (
      isFirstLogin.value
    ) {

      return
    }


    router.back()
  }

</script>


<style scoped>

.password-page {

  min-height: 100vh;

  display: flex;

  justify-content: center;

  align-items: center;

  padding: 20px;

  background: #f5f7fa;

}


.password-card {

  width: 520px;

  max-width: 100%;

}


.card-header {

  font-size: 20px;

  font-weight: 600;

}


.notice {

  margin-bottom: 24px;

}

</style>
