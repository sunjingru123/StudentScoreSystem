<template>
  <el-container class="layout">
    <el-aside width="220px">
      <h2>综合测评系统</h2>
      <el-menu router>
        <el-menu-item index="/home"> 首页 </el-menu-item>

        <el-menu-item index="/home/score"> 我的成绩 </el-menu-item>

        <el-menu-item index="/home/apply"> 加分申请 </el-menu-item>

        <el-menu-item index="/home/record"> 申请记录 </el-menu-item>

        <el-menu-item index="/home/message"> 消息通知 </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        学生综合测评系统

        <el-button type="danger" @click="logout"> 退出登录 </el-button>
      </el-header>

      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function logout() {
  localStorage.removeItem('token')

  localStorage.removeItem('user')
  router.replace('/login')
}
</script>
<style scoped>
.layout {
  height: 100vh;
}

.el-aside {
  background: #304156;

  color: white;
}

h2 {
  text-align: center;
}

.el-header {
  background: #409eff;

  color: white;

  font-size: 20px;

  line-height: 60px;
}
</style>
