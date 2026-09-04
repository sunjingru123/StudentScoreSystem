<template>

  <el-container class="layout">

    <!-- =====================================================
         左侧菜单
    ====================================================== -->

    <el-aside
      width="220px"
      class="aside"
    >

      <div class="logo">

        <div class="logo-title">
          学生综合测评系统
        </div>

        <div class="logo-subtitle">
          管理员后台
        </div>

      </div>

      <el-menu
        :default-active="route.path"
        router
        class="menu"
      >

        <el-menu-item
          index="/admin/adminHome"
        >

          <el-icon>
            <House />
          </el-icon>

          <span>
            首页
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/student"
        >

          <el-icon>
            <User />
          </el-icon>

          <span>
            学生管理
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/teacher"
        >

          <el-icon>
            <UserFilled />
          </el-icon>

          <span>
            教师管理
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/rule"
        >

          <el-icon>
            <Tickets />
          </el-icon>

          <span>
            评分项目管理
          </span>

        </el-menu-item>


        <!-- =================================================
             加减分调整
        ================================================== -->

        <el-menu-item
          index="/admin/score-adjustment"
        >

          <el-icon>
            <EditPen />
          </el-icon>

          <span>
            加减分调整
          </span>

        </el-menu-item>


        <!-- =================================================
             Excel数据导入
        ================================================== -->

        <el-menu-item
          index="/admin/excel-import"
        >

          <el-icon>
            <Upload />
          </el-icon>

          <span>
            Excel数据导入
          </span>

        </el-menu-item>


        <!-- =================================================
             学期管理
        ================================================== -->

        <el-menu-item
          index="/admin/semester"
        >

          <el-icon>
            <Calendar />
          </el-icon>

          <span>
            学期管理
          </span>

        </el-menu-item>


        <!-- =================================================
             汇总导出
        ================================================== -->

        <el-menu-item
          index="/admin/score-export"
        >

          <el-icon>
            <Download />
          </el-icon>

          <span>
            加减分汇总导出
          </span>

        </el-menu-item>

      </el-menu>

    </el-aside>


    <!-- =====================================================
         移动端抽屉菜单
    ====================================================== -->

    <el-drawer
      v-model="mobileMenuVisible"
      direction="ltr"
      size="280px"
      :with-header="false"
      class="mobile-drawer"
    >

      <div class="mobile-drawer-logo">

        <div class="mobile-drawer-logo-title">
          学生综合测评系统
        </div>

        <div class="mobile-drawer-logo-subtitle">
          管理员后台
        </div>

      </div>


      <el-menu
        :default-active="route.path"
        router
        class="menu mobile-drawer-menu"
        @select="closeMobileMenu"
      >

        <el-menu-item
          index="/admin/adminHome"
        >

          <el-icon>
            <House />
          </el-icon>

          <span>
            首页
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/student"
        >

          <el-icon>
            <User />
          </el-icon>

          <span>
            学生管理
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/teacher"
        >

          <el-icon>
            <UserFilled />
          </el-icon>

          <span>
            教师管理
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/rule"
        >

          <el-icon>
            <Tickets />
          </el-icon>

          <span>
            评分项目管理
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/score-adjustment"
        >

          <el-icon>
            <EditPen />
          </el-icon>

          <span>
            加减分调整
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/excel-import"
        >

          <el-icon>
            <Upload />
          </el-icon>

          <span>
            Excel数据导入
          </span>

        </el-menu-item>


        <!-- =================================================
             移动端学期管理
        ================================================== -->

        <el-menu-item
          index="/admin/semester"
        >

          <el-icon>
            <Calendar />
          </el-icon>

          <span>
            学期管理
          </span>

        </el-menu-item>


        <el-menu-item
          index="/admin/score-export"
        >

          <el-icon>
            <Download />
          </el-icon>

          <span>
            加减分汇总导出
          </span>

        </el-menu-item>

      </el-menu>

    </el-drawer>


    <!-- =====================================================
         右侧主体
    ====================================================== -->

    <el-container>

      <el-header class="header">

        <div class="header-left">

          <el-button
            class="mobile-menu-button"
            circle
            @click="mobileMenuVisible = true"
            aria-label="打开菜单"
          >

            <el-icon>
              <Menu />
            </el-icon>

          </el-button>


          <span class="system-name">
            学生综合测评系统
          </span>


          <span class="divider">
            /
          </span>


          <span class="current-page">
            {{ currentPageName }}
          </span>

        </div>


        <div class="header-right">

          <div class="admin-info">

            <el-avatar
              :size="38"
              class="avatar"
            >

              {{ adminName.charAt(0) }}

            </el-avatar>


            <div class="admin-text">

              <span class="welcome">
                欢迎
              </span>

              <span class="admin-name">
                {{ adminName }}
              </span>

            </div>

          </div>


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


      <el-main class="main">

        <router-view />

      </el-main>

    </el-container>

  </el-container>

</template>


<script setup>

import {
  computed,
  ref
} from 'vue'


import {
  useRouter,
  useRoute
} from 'vue-router'


import {
  House,
  User,
  UserFilled,
  Tickets,
  EditPen,
  Upload,
  Calendar,
  Download,
  SwitchButton,
  Lock,
  Menu
} from '@element-plus/icons-vue'


/*
 * =========================================================
 * Router
 * =========================================================
 */

const router =
  useRouter()


const route =
  useRoute()


/*
 * =========================================================
 * 获取管理员信息
 * =========================================================
 */

const userStr =
  localStorage.getItem('user')


let user = {}


try {

  user =
    userStr
      ? JSON.parse(userStr)
      : {}

} catch (error) {

  console.error(
    '用户信息解析失败',
    error
  )

}


const adminName =
  user.realName ||
  user.username ||
  '管理员'


/*
 * =========================================================
 * 当前页面名称
 * =========================================================
 */

const currentPageName =
  computed(() => {

    const path =
      route.path


    if (
      path === '/admin'
      ||
      path === '/admin/adminHome'
    ) {

      return '首页'

    }


    if (
      path.startsWith(
        '/admin/student'
      )
    ) {

      return '学生管理'

    }


    if (
      path.startsWith(
        '/admin/teacher'
      )
    ) {

      return '教师管理'

    }


    if (
      path.startsWith(
        '/admin/rule'
      )
    ) {

      return '评分项目管理'

    }


    if (
      path.startsWith(
        '/admin/score-adjustment'
      )
    ) {

      return '加减分调整'

    }


    if (
      path.startsWith(
        '/admin/excel-import'
      )
    ) {

      return 'Excel数据导入'

    }


    /*
     * =====================================================
     * 学期管理
     * =====================================================
     */

    if (
      path.startsWith(
        '/admin/semester'
      )
    ) {

      return '学期管理'

    }


    if (
      path.startsWith(
        '/admin/score-export'
      )
    ) {

      return '加减分汇总导出'

    }


    return '管理后台'

  })


/*
 * =========================================================
 * 移动端菜单
 * =========================================================
 */

const mobileMenuVisible =
  ref(false)


function closeMobileMenu() {

  mobileMenuVisible.value =
    false

}


/*
 * =========================================================
 * 退出登录
 * =========================================================
 */

function goChangePassword() {

  router.push(
    '/change-password'
  )

}


// =========================================================
// 退出登录
// =========================================================

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

</script>


<style scoped>

.layout {

  height:
    100vh;

  background:
    #f5f7fa;

}


.aside {

  background:
    #1f2937;

  color:
    white;

  overflow:
    hidden;

}


.logo {

  height:
    80px;

  padding:
    15px 20px;

  box-sizing:
    border-box;

  border-bottom:
    1px solid
    rgba(255,255,255,.08);

  display:
    flex;

  flex-direction:
    column;

  justify-content:
    center;

}


.logo-title {

  font-size:
    19px;

  font-weight:
    600;

}


.logo-subtitle {

  font-size:
    12px;

  color:
    #9ca3af;

  margin-top:
    5px;

}


.menu {

  border-right:
    none;

  background:
    transparent;

}


.menu :deep(.el-menu-item) {

  height:
    52px;

  line-height:
    52px;

  color:
    #d1d5db;

  font-size:
    15px;

}


.menu :deep(.el-menu-item:hover) {

  background:
    #374151;

  color:
    white;

}


.menu :deep(.el-menu-item.is-active) {

  background:
    #409eff;

  color:
    white;

}


.menu :deep(.el-icon) {

  font-size:
    18px;

  margin-right:
    10px;

}


/* =========================================================
   顶部
========================================================= */

.header {

  height:
    64px;

  background:
    white;

  border-bottom:
    1px solid
    #ebeef5;

  display:
    flex;

  align-items:
    center;

  justify-content:
    space-between;

  padding:
    0 28px;

}


.header-left {

  display:
    flex;

  align-items:
    center;

}


.system-name {

  font-size:
    18px;

  font-weight:
    600;

}


.divider {

  margin:
    0 12px;

  color:
    #c0c4cc;

}


.current-page {

  color:
    #909399;

  font-size:
    14px;

}


.header-right {

  display:
    flex;

  align-items:
    center;

  gap:
    22px;

}


.admin-info {

  display:
    flex;

  align-items:
    center;

  gap:
    10px;

}


.avatar {

  background:
    #409eff;

  color:
    white;

}


.admin-text {

  display:
    flex;

  flex-direction:
    column;

  line-height:
    20px;

}


.welcome {

  color:
    #909399;

  font-size:
    12px;

}


.admin-name {

  color:
    #303133;

  font-size:
    14px;

  font-weight:
    600;

}


/* =========================================================
   主体
========================================================= */

.main {

  padding:
    0;

  background:
    #f5f7fa;

  overflow-y:
    auto;

}


/* =========================================================
   移动端抽屉
========================================================= */

.mobile-drawer-logo {

  height:
    120px;

  padding:
    20px;

  box-sizing:
    border-box;

  display:
    flex;

  flex-direction:
    column;

  align-items:
    center;

  justify-content:
    center;

  background:
    #304156;

  color:
    #fff;

  border-bottom:
    1px solid
    rgba(255,255,255,.08);

}


.mobile-drawer-logo-icon {

  font-size:
    28px;

  margin-bottom:
    5px;

}


.mobile-drawer-logo-title {

  font-size:
    19px;

  font-weight:
    600;

  letter-spacing:
    .5px;

}


.mobile-drawer-logo-subtitle {

  margin-top:
    5px;

  font-size:
    13px;

  color:
    #aeb8c4;

}


:deep(.mobile-drawer .el-drawer__body) {

  padding:
    0;

  background:
    #304156;

}


:deep(.mobile-drawer .el-menu-item) {

  margin:
    4px 10px;

  border-radius:
    6px;

}


.mobile-drawer-menu {

  border-right:
    none;

}


/* =========================================================
   移动端响应式
========================================================= */

.mobile-menu-button {

  display:
    none;

  margin-right:
    8px;

}


@media (max-width: 768px) {

  .layout {

    min-height:
      100vh;

    height:
      100dvh;

    width:
      100%;

  }


  .aside {

    display:
      none;

  }


  .mobile-menu-button {

    display:
      inline-flex;

    align-items:
      center;

    justify-content:
      center;

    width:
      40px;

    height:
      40px;

    padding:
      0;

    flex:
      0 0 auto;

  }


  .header {

    height:
      56px !important;

    min-height:
      56px !important;

    padding:
      0 12px !important;

    gap:
      8px;

  }


  .header-left {

    min-width:
      0;

    flex:
      1;

    gap:
      6px !important;

    overflow:
      hidden;

  }


  .system-name,
  .system-title {

    font-size:
      16px !important;

    white-space:
      nowrap;

    overflow:
      hidden;

    text-overflow:
      ellipsis;

  }


  .system-role {

    flex:
      0 0 auto;

    padding:
      3px 7px !important;

    font-size:
      11px !important;

  }


  .divider,
  .current-page {

    display:
      none;

  }


  .header-right {

    flex:
      0 0 auto;

    gap:
      6px !important;

  }


  .user-text,
  .teacher-name,
  .admin-text {

    display:
      none !important;

  }


  .user-info,
  .admin-info {

    gap:
      0 !important;

  }


  .header-right .el-button {

    width:
      40px;

    height:
      40px;

    padding:
      0;

    margin:
      0;

  }


  .header-right .el-button .el-icon {

    margin:
      0;

  }


  .main {

    padding:
      12px !important;

    min-height:
      calc(100dvh - 56px) !important;

    overflow-x:
      hidden;

  }

}


@media (max-width: 480px) {

  .header {

    padding:
      0 8px !important;

  }


  .system-name,
  .system-title {

    font-size:
      15px !important;

  }


  .main {

    padding:
      10px !important;

  }

}

</style>
