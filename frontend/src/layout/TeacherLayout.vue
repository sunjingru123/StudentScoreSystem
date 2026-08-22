<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <div class="logo-icon">🎓</div>
        <div class="logo-title">综合测评系统</div>
        <div class="logo-subtitle">辅导员端</div>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/teacher">
          <el-icon>
            <House />
          </el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-menu-item index="/teacher/audit">
          <el-icon>
            <Checked />
          </el-icon>
          <span>加分审核</span>
        </el-menu-item>

        <el-menu-item index="/teacher/student">
          <el-icon>
            <User />
          </el-icon>
          <span>学生管理</span>
        </el-menu-item>

        <el-menu-item index="/teacher/activity">
          <el-icon>
            <Calendar />
          </el-icon>
          <span>活动管理</span>
        </el-menu-item>

        <el-menu-item index="/teacher/score">
          <el-icon>
            <Trophy />
          </el-icon>
          <span>成绩查看</span>
        </el-menu-item>

        <el-menu-item index="/teacher/message">
          <el-icon>
            <Bell />
          </el-icon>
          <span>消息通知</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧 -->
    <el-container>
      <!-- 顶部 -->
      <el-header class="header">
        <div class="header-left">
          <span class="system-title">学生综合测评系统</span>
          <span class="system-role">辅导员端</span>
        </div>

        <div class="header-right">
          <div class="user-info">
            <el-avatar :size="36">
              {{ teacherName.charAt(0) }}
            </el-avatar>

            <span class="teacher-name">
              {{ teacherName }}
            </span>
          </div>

          <el-button type="danger" plain @click="logout">
            <el-icon>
              <SwitchButton />
            </el-icon>

            退出登录
          </el-button>
        </div>
      </el-header>

      <!-- 页面内容 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import { House, Checked, User, Calendar, Trophy, Bell, SwitchButton } from '@element-plus/icons-vue'

const router = useRouter()

const teacherName = ref('辅导员')

onMounted(() => {
  const userStr = localStorage.getItem('user')

  if (!userStr) {
    router.replace('/login')
    return
  }

  try {
    const user = JSON.parse(userStr)

    console.log('当前辅导员：', user)

    teacherName.value = user.realName || user.username || '辅导员'
  } catch (error) {
    console.error('读取用户信息失败：', error)
  }
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')

  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
  min-height: 600px;
}

/* =========================
   左侧
========================= */

.aside {
  background: #304156;
  color: white;
  overflow: hidden;
}

.logo {
  height: 120px;

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;

  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-icon {
  font-size: 28px;
  margin-bottom: 5px;
}

.logo-title {
  font-size: 19px;
  font-weight: bold;
  letter-spacing: 1px;
}

.logo-subtitle {
  margin-top: 5px;
  font-size: 13px;
  color: #aeb8c4;
}

/* 菜单 */

.menu {
  border-right: none;
  padding-top: 10px;
}

.menu :deep(.el-menu-item) {
  height: 52px;
  line-height: 52px;
  font-size: 15px;
  margin: 4px 10px;
  border-radius: 6px;
}

.menu :deep(.el-menu-item:hover) {
  background-color: #263445 !important;
}

.menu :deep(.el-menu-item.is-active) {
  background-color: #409eff !important;
}

.menu :deep(.el-icon) {
  font-size: 18px;
  margin-right: 10px;
}

/* =========================
   顶部
========================= */

.header {
  height: 64px;

  display: flex;
  align-items: center;
  justify-content: space-between;

  padding: 0 25px;

  background: #ffffff;

  border-bottom: 1px solid #ebeef5;

  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.system-title {
  font-size: 20px;
  font-weight: bold;
  color: #303133;
}

.system-role {
  padding: 4px 10px;

  font-size: 12px;

  color: #409eff;

  background: #ecf5ff;

  border-radius: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.teacher-name {
  font-size: 15px;
  color: #303133;
}

/* =========================
   主体
========================= */

.main {
  padding: 25px;

  background: #f5f7fa;

  min-height: calc(100vh - 64px);

  overflow-y: auto;
}
</style>
