import {
  createRouter,
  createWebHistory
} from 'vue-router'


// =========================================================
// 登录
// =========================================================

import Login from '@/views/Login.vue'


// =========================================================
// 学生端布局 & 页面
// =========================================================

import MainLayout from '@/layout/MainLayout.vue'

import Home from '@/views/Home.vue'

import Score from '@/views/Score.vue'

import Apply from '@/views/Apply.vue'

import Record from '@/views/Record.vue'

import StudentMessage from '@/views/student/Message.vue'


// =========================================================
// 管理员端布局 & 页面
// =========================================================

import AdminLayout from '@/layout/AdminLayout.vue'

import AdminHome from '@/views/admin/AdminHome.vue'

import ApplyAudit from '@/views/admin/ApplyAudit.vue'

import StudentManage from '@/views/admin/StudentManage.vue'

import RuleManage from '@/views/admin/RuleManage.vue'

import AdminScore from '@/views/admin/AdminScore.vue'

import ExcelImport from '@/views/admin/ExcelImport.vue'


// =========================================================
// 辅导员端布局 & 页面
// =========================================================

import TeacherLayout from '@/layout/TeacherLayout.vue'

import TeacherHome from '@/views/teacher/TeacherHome.vue'

import TeacherAudit from '@/views/teacher/TeacherAudit.vue'

import TeacherActivity from '@/views/teacher/TeacherActivity.vue'

import TeacherScore from '@/views/teacher/TeacherScore.vue'

import TeacherMessage from '@/views/teacher/TeacherMessage.vue'


// =========================================================
// Router
// =========================================================

const router = createRouter({

  history: createWebHistory(),

  routes: [

    // =======================================================
    // 根路径
    // =======================================================

    {
      path: '/',
      redirect: '/login',
    },


    // =======================================================
    // 登录
    // =======================================================

    {
      path: '/login',

      name: 'Login',

      component: Login,
    },


    // =======================================================
    // 学生端
    //
    // /home
    //
    // 普通学生
    // +
    // 部门干部学生
    //
    // 都属于学生端
    // =======================================================

    {
      path: '/home',

      name: 'Student',

      component: MainLayout,

      children: [

        // ---------------------------------------------------
        // 首页
        // ---------------------------------------------------

        {
          path: '',

          name: 'StudentHome',

          component: Home,
        },


        // ---------------------------------------------------
        // 我的成绩
        // ---------------------------------------------------

        {
          path: 'score',

          name: 'StudentScore',

          component: Score,
        },


        // ---------------------------------------------------
        // 普通加减分申请
        // ---------------------------------------------------

        {
          path: 'apply',

          name: 'StudentApply',

          component: Apply,
        },


        // ---------------------------------------------------
        // 我的申请记录
        // ---------------------------------------------------

        {
          path: 'record',

          name: 'StudentRecord',

          component: Record,
        },


        // ---------------------------------------------------
        // 学生消息
        // ---------------------------------------------------

        {
          path: 'message',

          name: 'StudentMessage',

          component: StudentMessage,
        },


        // ---------------------------------------------------
        // 学生详情
        // ---------------------------------------------------

        {
          path: 'student/:id',

          name: 'StudentDetail',

          component: () =>
            import(
              '@/views/admin/StudentDetail.vue'
              ),
        },


        // ---------------------------------------------------
        // 部门加减分申报
        // ---------------------------------------------------

        {
          path: 'department-apply',

          name: 'StudentDepartmentApply',

          component: () =>
            import(
              '@/views/student/DepartmentApply.vue'
              ),
        },


        // ===================================================
        // ★★★ 档案部加减分汇总导出
        //
        // 地址：
        //
        // /home/score-export
        //
        // 管理员不走这里。
        //
        // 这里主要给：
        //
        // 档案部部长
        // 档案部副部长
        //
        // 使用。
        //
        // 注意：
        //
        // 他们本质上还是“学生”。
        //
        // 所以路由先允许学生进入，
        // 然后 ScoreExport.vue
        // 再调用后端权限接口。
        //
        // ===================================================

        {
          path: 'score-export',

          name: 'ArchiveScoreExport',

          component: () =>
            import(
              '@/views/archive/ScoreExport.vue'
              ),

          meta: {

            title: '加减分汇总导出',

            requiresArchiveExportPermission: true,

          },

        },

      ],

    },


    // =======================================================
    // 兼容旧地址
    //
    // 如果之前你使用：
    //
    // /archive/score-export
    //
    // 现在自动跳到：
    //
    // /home/score-export
    //
    // =======================================================

    {
      path: '/archive/score-export',

      redirect: '/home/score-export',

    },


    // =======================================================
    // 管理员
    //
    // /admin
    // =======================================================

    {
      path: '/admin',

      name: 'Admin',

      component: AdminLayout,

      redirect: '/admin/adminHome',

      children: [

        // ---------------------------------------------------
        // 管理员首页
        // ---------------------------------------------------

        {
          path: 'adminHome',

          name: 'AdminHome',

          component: AdminHome,
        },


        // ---------------------------------------------------
        // 成绩管理
        // ---------------------------------------------------

        {
          path: 'score',

          name: 'AdminScore',

          component: AdminScore,
        },


        // ---------------------------------------------------
        // 普通申请审核
        // ---------------------------------------------------

        {
          path: 'apply',

          name: 'AdminApplyAudit',

          component: ApplyAudit,
        },


        // ---------------------------------------------------
        // 学生管理
        // ---------------------------------------------------

        {
          path: 'student',

          name: 'AdminStudent',

          component: StudentManage,
        },


        // ---------------------------------------------------
        // 学生详情
        // ---------------------------------------------------

        {
          path: 'student/:id',

          name: 'AdminStudentDetail',

          component: () =>
            import(
              '@/views/admin/StudentDetail.vue'
              ),
        },


        // ---------------------------------------------------
        // 规则管理
        // ---------------------------------------------------

        {
          path: 'rule',

          name: 'AdminRule',

          component: RuleManage,
        },


        // ---------------------------------------------------
        // 成绩调整
        // ---------------------------------------------------

        {
          path: 'score-adjustment',

          name: 'AdminScoreAdjustment',

          component: () =>
            import(
              '@/views/admin/ScoreAdjustment.vue'
              ),
        },


        // ---------------------------------------------------
        // Excel 导入
        // ---------------------------------------------------

        {
          path: 'excel-import',

          name: 'AdminExcelImport',

          component: ExcelImport,
        },


        // ===================================================
        // ★★★ 管理员加减分汇总导出
        //
        // 地址：
        //
        // /admin/score-export
        //
        // 只有管理员可以访问。
        //
        // ===================================================

        {
          path: 'score-export',

          name: 'AdminScoreExport',

          component: () =>
            import(
              '@/views/archive/ScoreExport.vue'
              ),

          meta: {

            title: '加减分汇总导出',

            requiresAdminExportPermission: true,

          },

        },

      ],

    },


    // =======================================================
    // 辅导员
    //
    // /teacher
    // =======================================================

    {
      path: '/teacher',

      name: 'Teacher',

      component: TeacherLayout,

      children: [

        // ---------------------------------------------------
        // 辅导员首页
        // ---------------------------------------------------

        {
          path: '',

          name: 'TeacherHome',

          component: TeacherHome,
        },


        // ---------------------------------------------------
        // 部门加减分终审
        // ---------------------------------------------------

        {
          path: 'department-score-audit',

          name: 'CounselorDepartmentScoreAudit',

          component: () =>
            import(
              '@/views/teacher/DepartmentScoreAudit.vue'
              ),
        },


        // ---------------------------------------------------
        // 活动管理
        // ---------------------------------------------------

        {
          path: 'activity',

          name: 'TeacherActivity',

          component: TeacherActivity,
        },


        // ---------------------------------------------------
        // 成绩查看
        // ---------------------------------------------------

        {
          path: 'score',

          name: 'TeacherScore',

          component: TeacherScore,
        },


        // ---------------------------------------------------
        // 消息
        // ---------------------------------------------------

        {
          path: 'message',

          name: 'TeacherMessage',

          component: TeacherMessage,
        },

      ],

    },

  ],

})


// =========================================================
// 404
// =========================================================

router.addRoute({

  path: '/:pathMatch(.*)*',

  redirect: '/login',

})


// =========================================================
// 全局路由守卫
//
// 负责：
//
// 1. 登录校验
// 2. 系统角色校验
// 3. 管理员导出页面基础权限
// 4. 档案部导出页面基础权限
//
// 注意：
//
// 部长 / 副部长 / 干事
//
// 不是系统角色。
//
// 他们仍然是：
//
// role = 学生
//
// 具体部门身份来自：
//
// sys_user_department
//
// =========================================================

router.beforeEach((to) => {

  // =======================================================
  // 获取用户缓存
  // =======================================================

  const userStr =
    localStorage.getItem('user')


  // =======================================================
  // 1. 登录页面
  // =======================================================

  if (to.path === '/login') {

    return true

  }


  // =======================================================
  // 2. 没有用户信息
  // =======================================================

  if (!userStr) {

    return '/login'

  }


  // =======================================================
  // 3. 解析用户
  // =======================================================

  let user

  try {

    user =
      JSON.parse(userStr)

  }

  catch (err) {

    console.error(
      '用户缓存解析失败：',
      err
    )

    localStorage.removeItem('user')

    localStorage.removeItem('token')

    return '/login'

  }


  // =======================================================
  // 4. 系统角色
  //
  // 只有三个：
  //
  // 学生
  // 管理员
  // 辅导员
  // =======================================================

  const role =
    user.role


  const rolePrefixMap = {

    学生: '/home',

    管理员: '/admin',

    辅导员: '/teacher',

  }


  // =======================================================
  // 5. 角色不存在
  // =======================================================

  if (!rolePrefixMap[role]) {

    localStorage.removeItem('user')

    localStorage.removeItem('token')

    return '/login'

  }


  // =======================================================
  // 6. 管理员导出权限
  //
  // /admin/score-export
  //
  // 只有管理员允许。
  // =======================================================

  if (
    to.meta.requiresAdminExportPermission
  ) {

    if (role === '管理员') {

      return true

    }


    // 学生不能进入管理员导出页面

    if (role === '学生') {

      return '/home'

    }


    // 辅导员不能进入管理员导出页面

    if (role === '辅导员') {

      return '/teacher'

    }


    return '/login'

  }


  // =======================================================
  // 7. 档案部导出页面
  //
  // /home/score-export
  //
  // 管理员：
  //     允许
  //
  // 学生：
  //     允许进入页面
  //
  //     之后由 ScoreExport.vue
  //     调用后端：
  //
  //     /scoreExport/permission
  //
  //     判断：
  //
  //     档案部部长      → 允许
  //     档案部副部长    → 允许
  //     档案部干事      → 拒绝
  //     普通学生        → 拒绝
  //     其他部门部长    → 拒绝
  //     其他部门副部长  → 拒绝
  //
  // 辅导员：
  //     不允许
  //
  // =======================================================

  if (
    to.meta.requiresArchiveExportPermission
  ) {

    // ---------------------------------------------------
    // 管理员
    // ---------------------------------------------------

    if (role === '管理员') {

      return true

    }


    // ---------------------------------------------------
    // 学生
    //
    // 档案部部长、副部长本质也是学生。
    //
    // 所以这里允许进入。
    //
    // 真正权限由后端判断。
    // ---------------------------------------------------

    if (role === '学生') {

      return true

    }


    // ---------------------------------------------------
    // 辅导员
    // ---------------------------------------------------

    if (role === '辅导员') {

      return '/teacher'

    }


    return '/login'

  }


  // =======================================================
  // 8. 管理员基础权限
  // =======================================================

  if (role === '管理员') {

    if (
      !to.path.startsWith('/admin')
    ) {

      return '/admin/adminHome'

    }

    return true

  }


  // =======================================================
  // 9. 学生基础权限
  // =======================================================

  if (role === '学生') {

    if (
      !to.path.startsWith('/home')
    ) {

      return '/home'

    }

    return true

  }


  // =======================================================
  // 10. 辅导员基础权限
  // =======================================================

  if (role === '辅导员') {

    if (
      !to.path.startsWith('/teacher')
    ) {

      return '/teacher'

    }

    return true

  }


  // =======================================================
  // 11. 默认
  // =======================================================

  return '/login'

})


export default router
