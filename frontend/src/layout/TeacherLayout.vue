<template>
  <el-container class="layout">

    <!-- =========================
         左侧菜单
    ========================== -->
    <el-aside
      width="220px"
      class="aside"
    >

      <!-- Logo -->
      <div class="logo">

        <div class="logo-icon">
          🎓
        </div>

        <div class="logo-title">
          学生综合测评系统
        </div>

        <div class="logo-subtitle">
          辅导员端
        </div>

      </div>


      <!-- =========================
           菜单
      ========================== -->
      <el-menu
        :default-active="$route.path"
        router
        class="menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
      >

        <!-- 首页 -->
        <el-menu-item
          index="/teacher"
        >

          <el-icon>
            <House />
          </el-icon>

          <span>
            首页
          </span>

        </el-menu-item>


        <!-- =========================
             部门申报终审
        ========================== -->
        <el-menu-item
          index="/teacher/department-score-audit"
        >

          <el-icon>
            <Checked />
          </el-icon>

          <span>
            部门申报终审
          </span>

        </el-menu-item>


        <!-- =========================
             活动管理
        ========================== -->
        <el-menu-item
          index="/teacher/activity"
        >

          <el-icon>
            <Calendar />
          </el-icon>

          <span>
            活动管理
          </span>

        </el-menu-item>


        <!-- =========================
             成绩查看
             学生管理已经合并到这里
        ========================== -->
        <el-menu-item
          index="/teacher/score"
        >

          <el-icon>
            <Trophy />
          </el-icon>

          <span>
            成绩查看
          </span>

        </el-menu-item>


        <!-- =========================
             消息通知
        ========================== -->
        <el-menu-item
          index="/teacher/message"
        >

          <el-icon>
            <Bell />
          </el-icon>

          <span>
            消息通知
          </span>

        </el-menu-item>

      </el-menu>

    </el-aside>


    <!-- =========================
         移动端抽屉菜单
    ========================== -->

    <el-drawer
      v-model="mobileMenuVisible"
      direction="ltr"
      size="280px"
      :with-header="false"
      class="mobile-drawer"
    >

      <div class="mobile-drawer-logo">
        <div class="mobile-drawer-logo-icon">🎓</div>
        <div class="mobile-drawer-logo-title">学生综合测评系统</div>
        <div class="mobile-drawer-logo-subtitle">辅导员端</div>
      </div>

      <el-menu
        :default-active="$route.path"
        router
        class="menu mobile-drawer-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        @select="closeMobileMenu"
      >

        <el-menu-item index="/teacher">
          <el-icon><House /></el-icon>
          <span>首页</span>
        </el-menu-item>

        <el-menu-item index="/teacher/department-score-audit">
          <el-icon><Checked /></el-icon>
          <span>部门申报终审</span>
        </el-menu-item>

        <el-menu-item index="/teacher/activity">
          <el-icon><Calendar /></el-icon>
          <span>活动管理</span>
        </el-menu-item>

        <el-menu-item index="/teacher/score">
          <el-icon><Trophy /></el-icon>
          <span>成绩查看</span>
        </el-menu-item>

        <el-menu-item index="/teacher/message">
          <el-icon><Bell /></el-icon>
          <span>消息通知</span>
        </el-menu-item>

      </el-menu>

    </el-drawer>

    <!-- =========================
         右侧主体
    ========================== -->
    <el-container>

      <!-- =========================
           顶部
      ========================== -->
      <el-header class="header">

        <!-- 左侧标题 -->
        <div class="header-left">

          <el-button
            class="mobile-menu-button"
            circle
            @click="mobileMenuVisible = true"
            aria-label="打开菜单"
          >
            <el-icon><Menu /></el-icon>
          </el-button>

          <span class="system-title">
            学生综合测评系统
          </span>

          <span class="system-role">
            辅导员端
          </span>

        </div>


        <!-- =========================
             右侧用户信息
        ========================== -->
        <div class="header-right">

          <div class="user-info">

            <el-avatar :size="36">
              {{ teacherName.charAt(0) }}
            </el-avatar>

            <span class="teacher-name">
              {{ teacherName }}
            </span>

          </div>


          <!-- 修改密码 -->
          <el-button
            type="primary"
            plain
            @click="goChangePassword"
          >

            <el-icon>
              <Lock />
            </el-icon>

            修改密码

          </el-button>


          <!-- 退出登录 -->
          <el-button
            type="danger"
            plain
            @click="logout"
          >

            <el-icon>
              <SwitchButton />
            </el-icon>

            退出登录

          </el-button>

        </div>

      </el-header>


      <!-- =========================
           页面内容
      ========================== -->
      <el-main class="main">

        <router-view />

      </el-main>

    </el-container>

  </el-container>
</template>


<script setup>

import {
  ref,
  onMounted
} from 'vue'

import {
  useRouter
} from 'vue-router'

import {
  House,
  Checked,
  Calendar,
  Trophy,
  Bell,
  SwitchButton,
  Lock,
  Menu
} from '@element-plus/icons-vue'


const router =
  useRouter()


// ============================================================
// 当前辅导员姓名
// ============================================================

const teacherName =
  ref('辅导员')


// ============================================================
// 获取当前登录用户
// ============================================================

function loadTeacher() {

  const userStr =
    localStorage.getItem('user')


  // 没有登录信息
  if (!userStr) {

    router.replace('/login')

    return
  }


  try {

    const user =
      JSON.parse(userStr)


    console.log(
      '当前辅导员：',
      user
    )


    teacherName.value =
      user.realName ||
      user.username ||
      '辅导员'

  } catch (error) {

    console.error(
      '读取用户信息失败：',
      error
    )

  }

}


// ============================================================
// 退出登录
// ============================================================

const mobileMenuVisible = ref(false)

function closeMobileMenu() {
  mobileMenuVisible.value = false
}

function goChangePassword() {

  router.push(
    '/change-password'
  )

}


// ============================================================
// 退出登录
// ============================================================

function logout() {

  localStorage.removeItem(
    'token'
  )

  localStorage.removeItem(
    'user'
  )


  router.replace(
    '/login'
  )

}


// ============================================================
// 页面初始化
// ============================================================

onMounted(() => {

  loadTeacher()

})

</script>


<style scoped>

/* ============================================================
   整体布局
============================================================ */

.layout {

  height: 100vh;

  min-height: 600px;

}


/* ============================================================
   左侧菜单
============================================================ */

.aside {

  background: #304156;

  color: white;

  overflow: hidden;

}


/* ============================================================
   Logo
============================================================ */

.logo {

  height: 120px;

  display: flex;

  flex-direction: column;

  align-items: center;

  justify-content: center;

  border-bottom:
    1px solid
    rgba(255, 255, 255, 0.08);

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


/* ============================================================
   菜单
============================================================ */

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

  background-color:
    #263445 !important;

}


.menu :deep(.el-menu-item.is-active) {

  background-color:
    #409eff !important;

}


.menu :deep(.el-icon) {

  font-size: 18px;

  margin-right: 10px;

}


/* ============================================================
   顶部
============================================================ */

.header {

  height: 64px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  padding: 0 25px;

  background: #ffffff;

  border-bottom:
    1px solid
    #ebeef5;

  box-shadow:
    0 1px 4px
    rgba(0, 0, 0, 0.06);

}


/* ============================================================
   顶部左侧
============================================================ */

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


/* ============================================================
   顶部右侧
============================================================ */

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


/* ============================================================
   主体
============================================================ */

.main {

  padding: 25px;

  background: #f5f7fa;

  min-height:
    calc(100vh - 64px);

  overflow-y: auto;

}




.mobile-drawer-logo {
  height: 120px;
  padding: 20px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #304156;
  color: #fff;
  border-bottom: 1px solid rgba(255,255,255,.08);
}

.mobile-drawer-logo-icon {
  font-size: 28px;
  margin-bottom: 5px;
}

.mobile-drawer-logo-title {
  font-size: 19px;
  font-weight: 600;
  letter-spacing: .5px;
}

.mobile-drawer-logo-subtitle {
  margin-top: 5px;
  font-size: 13px;
  color: #aeb8c4;
}

:deep(.mobile-drawer .el-drawer__body) {
  padding: 0;
  background: #304156;
}

:deep(.mobile-drawer .el-menu-item) {
  margin: 4px 10px;
  border-radius: 6px;
}

/* =========================================================
   移动端响应式
========================================================= */

.mobile-menu-button {

  display: none;

  margin-right: 8px;

}

.mobile-drawer-menu {

  border-right: none;

}

@media (max-width: 768px) {

  .layout {
    min-height: 100vh;
    height: 100dvh;
    width: 100%;
  }

  .aside {
    display: none;
  }

  .mobile-menu-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    padding: 0;
    flex: 0 0 auto;
  }

  .header {
    height: 56px !important;
    min-height: 56px !important;
    padding: 0 12px !important;
    gap: 8px;
  }

  .header-left {
    min-width: 0;
    flex: 1;
    gap: 6px !important;
    overflow: hidden;
  }

  .system-name,
  .system-title {
    font-size: 16px !important;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .system-role {
    flex: 0 0 auto;
    padding: 3px 7px !important;
    font-size: 11px !important;
  }

  .divider,
  .current-page {
    display: none;
  }

  .header-right {
    flex: 0 0 auto;
    gap: 6px !important;
  }

  .user-text,
  .teacher-name,
  .admin-text {
    display: none !important;
  }

  .user-info,
  .admin-info {
    gap: 0 !important;
  }

  .header-right .el-button {
    width: 40px;
    height: 40px;
    padding: 0;
    margin: 0;
  }

  .header-right .el-button .el-icon {
    margin: 0;
  }

  .main {
    padding: 12px !important;
    min-height: calc(100dvh - 56px) !important;
    overflow-x: hidden;
  }
}

@media (max-width: 480px) {

  .header {
    padding: 0 8px !important;
  }

  .system-name,
  .system-title {
    font-size: 15px !important;
  }

  .main {
    padding: 10px !important;
  }
}

</style>
