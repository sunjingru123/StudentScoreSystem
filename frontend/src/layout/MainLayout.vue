<template>
  <el-container class="layout">

    <!-- ==================== 左侧菜单 ==================== -->
    <el-aside width="220px">

      <h2>综合测评系统</h2>

      <el-menu router>

        <!-- 首页 -->
        <el-menu-item index="/home">
          首页
        </el-menu-item>


        <!-- 我的成绩 -->
        <el-menu-item index="/home/score">
          我的成绩
        </el-menu-item>


        <!-- 加分申请 -->
        <el-menu-item index="/home/apply">
          加分申请
        </el-menu-item>


        <!-- 申请记录 -->
        <el-menu-item index="/home/record">
          申请记录
        </el-menu-item>


        <!-- 消息通知 -->
        <el-menu-item index="/home/message">
          消息通知
        </el-menu-item>


        <!-- =================================================
             档案部部长 / 副部长专属
             加减分汇总导出
             ================================================= -->
        <el-menu-item
          v-if="canScoreExport"
          index="/home/score-export"
        >
          加减分汇总导出
        </el-menu-item>

      </el-menu>

    </el-aside>


    <!-- ==================== 右侧内容 ==================== -->
    <el-container>

      <!-- 顶部 -->
      <el-header class="header">

        <span>
          学生综合测评系统
        </span>


        <el-button
          type="danger"
          @click="logout"
        >
          退出登录
        </el-button>

      </el-header>


      <!-- 页面内容 -->
      <el-main>

        <router-view />

      </el-main>

    </el-container>

  </el-container>
</template>


<script setup>

import {
  onMounted,
  ref
} from 'vue'

import {
  useRouter
} from 'vue-router'

import request from '@/utils/request'


const router = useRouter()


/* =========================================================
   是否拥有加减分汇总导出权限
   ========================================================= */

const canScoreExport = ref(false)


/* =========================================================
   获取当前用户的部门权限
   ========================================================= */

const loadPermissions = async () => {

  try {

    const res =
      await request.get(
        '/departmentScoreApply/my-permissions'
      )


    if (
      res.code !== 200
      && res.code !== 0
    ) {

      return
    }


    const data =
      res.data || {}


    const departments =
      data.departments || []


    /*
     * 只有：
     *
     * 档案部 + 部长
     *
     * 或
     *
     * 档案部 + 副部长
     *
     * 才可以导出。
     *
     * 干事不可以。
     */


    canScoreExport.value =
      departments.some(
        item =>

          item.departmentName === '档案部'
          &&
          (
            item.position === '部长'
            ||
            item.position === '副部长'
          )
      )

  } catch (error) {

    console.error(
      '获取部门权限失败：',
      error
    )

    canScoreExport.value = false

  }

}


/* =========================================================
   退出登录
   ========================================================= */

function logout() {

  localStorage.removeItem('token')

  localStorage.removeItem('user')

  router.replace('/login')

}


/* =========================================================
   页面加载
   ========================================================= */

onMounted(() => {

  loadPermissions()

})

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

  margin: 0;

  padding: 18px 0;

  font-size: 22px;

}


.el-header {

  background: #409eff;

  color: white;

  font-size: 20px;

  line-height: 60px;

  display: flex;

  align-items: center;

  justify-content: space-between;

  padding: 0 24px;

}


</style>
