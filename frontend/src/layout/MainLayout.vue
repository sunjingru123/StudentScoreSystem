<template>

  <el-container class="layout">


    <!-- =====================================================
         左侧菜单
    ====================================================== -->

    <el-aside
      width="230px"
      class="aside"
    >

      <!-- Logo -->

      <div class="logo">

        <div class="logo-icon">
          🎓
        </div>

        <div class="logo-title">
          综合测评系统
        </div>

        <div class="logo-subtitle">
          学生端
        </div>

      </div>


      <!-- =================================================
           菜单
      ================================================= -->

      <el-menu
        :default-active="route.path"
        router
        class="menu"
      >


        <!-- 首页 -->

        <el-menu-item
          index="/home"
        >

          <el-icon>
            <House />
          </el-icon>

          <span>
            首页
          </span>

        </el-menu-item>


        <!-- 我的成绩 -->

        <el-menu-item
          index="/home/score"
        >

          <el-icon>
            <Trophy />
          </el-icon>

          <span>
            我的成绩
          </span>

        </el-menu-item>


        <!-- 个人加分申请 -->

        <el-menu-item
          index="/home/apply"
        >

          <el-icon>
            <DocumentAdd />
          </el-icon>

          <span>
            个人加分申报
          </span>

        </el-menu-item>


        <!-- 我的申请 -->

        <el-menu-item
          index="/home/record"
        >

          <el-icon>
            <Document />
          </el-icon>

          <span>
            我的申请记录
          </span>

        </el-menu-item>


        <!-- =================================================
             部门申报
             部门干事 / 部门成员
        ================================================== -->

        <el-menu-item
          index="/home/department-apply"
        >

          <el-icon>
            <EditPen />
          </el-icon>

          <span>
            部门加减分申报
          </span>

        </el-menu-item>


        <!-- =================================================
             ★ 部门负责人审核
        ================================================== -->

        <el-menu-item
          v-if="departmentLeader"
          index="/home/department-audit"
        >

          <el-icon>
            <Checked />
          </el-icon>

          <span>
            部门申报审核
          </span>

        </el-menu-item>


        <!-- =================================================
             ★ 档案部个人证书审核
        ================================================== -->

        <el-menu-item
          v-if="archiveLeader"
          index="/home/certificate-audit"
        >

          <el-icon>
            <FolderChecked />
          </el-icon>

          <span>
            个人证书审核
          </span>

        </el-menu-item>


        <!-- =================================================
             ★ 档案部汇总导出
        ================================================== -->

        <el-menu-item
          v-if="archiveLeader"
          index="/home/score-export"
        >

          <el-icon>
            <Download />
          </el-icon>

          <span>
            加减分汇总导出
          </span>

        </el-menu-item>


        <!-- =================================================
             消息
        ================================================== -->

        <el-menu-item
          index="/home/message"
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


    <!-- =====================================================
         右侧
    ====================================================== -->

    <el-container>


      <!-- 顶部 -->

      <el-header class="header">


        <div class="header-left">

          <span class="system-name">
            学生综合测评系统
          </span>

          <span class="system-role">
            学生端
          </span>

        </div>


        <div class="header-right">


          <div class="user-info">

            <el-avatar
              :size="38"
            >

              {{ userName.charAt(0) }}

            </el-avatar>


            <div class="user-text">

              <span class="welcome">
                欢迎
              </span>

              <span class="user-name">
                {{ userName }}
              </span>

            </div>

          </div>


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


      <!-- =================================================
           页面主体
      ================================================== -->

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
  useRouter,
  useRoute
} from 'vue-router'


import {
  ElMessage
} from 'element-plus'


import {
  House,
  Trophy,
  DocumentAdd,
  Document,
  EditPen,
  Checked,
  FolderChecked,
  Download,
  Bell,
  SwitchButton
} from '@element-plus/icons-vue'


import request from '@/utils/request'


const router =
  useRouter()


const route =
  useRoute()


// =========================================================
// 用户
// =========================================================

const userName =
  ref('学生')


const departmentLeader =
  ref(false)


const archiveLeader =
  ref(false)


// =========================================================
// 获取当前用户
// =========================================================

function loadUser() {

  const userStr =
    localStorage.getItem('user')


  if (!userStr) {

    router.replace('/login')

    return

  }


  try {

    const user =
      JSON.parse(userStr)


    console.log(
      '当前学生用户：',
      user
    )


    userName.value =
      user.realName ||
      user.username ||
      '学生'


  } catch (error) {

    console.error(
      '读取用户信息失败：',
      error
    )

  }

}


// =========================================================
// 查询部门身份
//
// 这里不要直接相信 localStorage。
// 最终以数据库权限为准。
// =========================================================

async function loadPermission() {

  try {

    const res =
      await request.get(
        '/user/department/permission'
      )


    console.log(
      '部门权限：',
      res
    )


    const result =
      res?.data?.code !== undefined
        ? res.data
        : res


    if (
      result?.code !== 200
      &&
      result?.code !== 0
    ) {

      return

    }


    const data =
      result?.data || {}


    // =====================================================
    // 部门负责人 / 副负责人
    //
    // 只要是负责人或者副负责人，
    // 就能看到部门审核。
    // =====================================================

    departmentLeader.value =
      data.departmentLeader === true
      ||
      data.isDepartmentLeader === true
      ||
      data.position === '部长'
      ||
      data.position === '副部长'
      ||
      data.position === '负责人'
      ||
      data.position === '副负责人'


    // =====================================================
    // 档案部负责人 / 副负责人
    // =====================================================

    archiveLeader.value =
      data.archiveLeader === true
      ||
      data.isArchiveLeader === true
      ||
      data.archiveDepartmentLeader === true
      ||
      data.archivePosition === '部长'
      ||
      data.archivePosition === '副部长'


  } catch (error) {

    console.error(
      '获取部门权限失败：',
      error
    )

  }

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


// =========================================================
// 初始化
// =========================================================

onMounted(async () => {

  loadUser()

  await loadPermission()

})

</script>


<style scoped>

.layout {

  height: 100vh;

  min-height: 600px;

  background:
    #f5f7fa;

}


/* =========================================================
   左侧
========================================================= */

.aside {

  background:
    #304156;

  color:
    white;

  overflow:
    hidden;

}


/* =========================================================
   Logo
========================================================= */

.logo {

  height:
    120px;

  display:
    flex;

  flex-direction:
    column;

  align-items:
    center;

  justify-content:
    center;

  border-bottom:
    1px solid
    rgba(255,255,255,.08);

}


.logo-icon {

  font-size:
    28px;

  margin-bottom:
    5px;

}


.logo-title {

  font-size:
    19px;

  font-weight:
    bold;

}


.logo-subtitle {

  margin-top:
    5px;

  font-size:
    13px;

  color:
    #aeb8c4;

}


/* =========================================================
   菜单
========================================================= */

.menu {

  border-right:
    none;

  padding-top:
    10px;

  background:
    transparent;

}


.menu :deep(.el-menu-item) {

  height:
    52px;

  line-height:
    52px;

  margin:
    4px 10px;

  border-radius:
    6px;

  color:
    #bfcbd9;

  font-size:
    15px;

}


.menu :deep(.el-menu-item:hover) {

  background:
    #263445;

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

  padding:
    0 25px;

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

}


.header-left {

  display:
    flex;

  align-items:
    center;

  gap:
    12px;

}


.system-name {

  font-size:
    20px;

  font-weight:
    bold;

  color:
    #303133;

}


.system-role {

  padding:
    4px 10px;

  font-size:
    12px;

  color:
    #409eff;

  background:
    #ecf5ff;

  border-radius:
    12px;

}


.header-right {

  display:
    flex;

  align-items:
    center;

  gap:
    20px;

}


.user-info {

  display:
    flex;

  align-items:
    center;

  gap:
    10px;

}


.user-text {

  display:
    flex;

  flex-direction:
    column;

}


.welcome {

  font-size:
    12px;

  color:
    #909399;

}


.user-name {

  font-size:
    14px;

  font-weight:
    600;

  color:
    #303133;

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

</style>
