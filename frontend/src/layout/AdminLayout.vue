<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside width="220px" class="aside">
      <!-- 系统标题 -->
      <div class="logo">
        <div class="logo-title">综合测评系统</div>

        <div class="logo-subtitle">管理员后台</div>
      </div>

      <el-menu :default-active="route.path" router class="menu">
        <el-menu-item index="/admin/adminHome">
          <el-icon>
            <House />
          </el-icon>

          <span>首页</span>
        </el-menu-item>

        <el-menu-item index="/admin/student">
          <el-icon>
            <User />
          </el-icon>

          <span>学生管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/rule">
          <el-icon>
            <Tickets />
          </el-icon>

          <span>评分项目管理</span>
        </el-menu-item>

        <el-menu-item index="/admin/apply">
          <el-icon>
            <DocumentChecked />
          </el-icon>

          <span>加分申请审核</span>
        </el-menu-item>
        <el-menu-item index="/admin/excel-import">
          <el-icon>
            <Upload />
          </el-icon>

          <span>Excel数据导入</span>
        </el-menu-item>
        <!-- 新增：成绩管理 -->
        <el-menu-item index="/admin/score">
          <el-icon>
            <DataAnalysis />
          </el-icon>

          <span>成绩管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 右侧 -->
    <el-container>
      <!-- 顶部 -->
      <el-header class="header">
        <div class="header-left">
          <span class="system-name"> 综合测评系统 </span>

          <span class="divider"> / </span>

          <span class="current-page">
            {{ currentPageName }}
          </span>
        </div>

        <div class="header-right">
          <div class="admin-info">
            <el-avatar :size="38" class="avatar">
              {{ adminName.charAt(0) }}
            </el-avatar>

            <div class="admin-text">
              <span class="welcome"> 欢迎 </span>

              <span class="admin-name">
                {{ adminName }}
              </span>
            </div>
          </div>

          <el-button type="danger" plain @click="logout">
            <el-icon>
              <SwitchButton />
            </el-icon>

            退出登录
          </el-button>
        </div>
      </el-header>

      <!-- 内容 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'

import { useRouter, useRoute } from 'vue-router'

import {
  House,
  User,
  Tickets,
  DocumentChecked,
  DataAnalysis,
  SwitchButton,
} from '@element-plus/icons-vue'

const router = useRouter()

const route = useRoute()

/**
 * 管理员信息
 */
const userStr = localStorage.getItem('user')

let user = {}

try {
  user = userStr ? JSON.parse(userStr) : {}
} catch (e) {
  console.error('用户信息解析失败', e)
}

/**
 * 管理员姓名
 */
const adminName = user.realName || user.username || '管理员'

/**
 * 当前页面名称
 */
const currentPageName = computed(() => {
  const path = route.path

  if (path === '/admin' || path === '/admin/adminHome') {
    return '首页'
  }

  if (path.startsWith('/admin/student')) {
    return '学生管理'
  }

  if (path.startsWith('/admin/rule')) {
    return '评分项目管理'
  }

  if (path.startsWith('/admin/apply')) {
    return '加分申请审核'
  }

  if (path.startsWith('/admin/score')) {
    return '成绩管理'
  }

  return '管理后台'
})

/**
 * 退出登录
 */
function logout() {
  localStorage.removeItem('token')

  localStorage.removeItem('user')

  router.replace('/login')
}
</script>

<style scoped>
.layout {
  height: 100vh;
  background: #f5f7fa;
}

/* =========================
   左侧
========================= */

.aside {
  background: #1f2937;
  color: white;
  overflow: hidden;
}

.logo {
  height: 80px;
  padding: 15px 20px;
  box-sizing: border-box;

  border-bottom: 1px solid rgba(255, 255, 255, 0.08);

  display: flex;
  flex-direction: column;
  justify-content: center;
}

.logo-title {
  font-size: 19px;
  font-weight: 600;
  letter-spacing: 1px;
}

.logo-subtitle {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 5px;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  height: 52px;
  line-height: 52px;

  color: #d1d5db;

  font-size: 15px;
}

.menu :deep(.el-menu-item:hover) {
  background: #374151;
  color: white;
}

.menu :deep(.el-menu-item.is-active) {
  background: #409eff;
  color: white;
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

  background: white;

  border-bottom: 1px solid #ebeef5;

  display: flex;
  align-items: center;
  justify-content: space-between;

  padding: 0 28px;

  box-sizing: border-box;
}

.header-left {
  display: flex;
  align-items: center;
}

.system-name {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.divider {
  margin: 0 12px;
  color: #c0c4cc;
}

.current-page {
  color: #909399;
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 22px;
}

.admin-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  background: #409eff;
  color: white;
  font-size: 16px;
}

.admin-text {
  display: flex;
  flex-direction: column;
  line-height: 20px;
}

.welcome {
  color: #909399;
  font-size: 12px;
}

.admin-name {
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

/* =========================
   主内容
========================= */

.main {
  padding: 0;

  background: #f5f7fa;

  overflow-y: auto;
}
</style>
