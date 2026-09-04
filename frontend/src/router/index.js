import {
  createRouter,
  createWebHistory
} from 'vue-router'


// =========================================================
// 登录
// =========================================================

import Login from '@/views/Login.vue'
import ChangePassword from '@/views/ChangePassword.vue'


// =========================================================
// 学生端
// =========================================================

import MainLayout from '@/layout/MainLayout.vue'

import Home from '@/views/Home.vue'
import Score from '@/views/Score.vue'
import Apply from '@/views/Apply.vue'
import Record from '@/views/Record.vue'

import StudentMessage from '@/views/student/Message.vue'


// =========================================================
// 管理员端
// =========================================================

import AdminLayout from '@/layout/AdminLayout.vue'

import TeacherManage from '@/views/admin/TeacherManage.vue'
import AdminHome from '@/views/admin/AdminHome.vue'
import StudentManage from '@/views/admin/StudentManage.vue'
import RuleManage from '@/views/admin/RuleManage.vue'
import ExcelImport from '@/views/admin/ExcelImport.vue'
import SemesterManage from '@/views/admin/SemesterManage.vue'


// =========================================================
// 辅导员端
// =========================================================

import TeacherLayout from '@/layout/TeacherLayout.vue'

import TeacherHome from '@/views/teacher/TeacherHome.vue'
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
    // 修改密码
    // =======================================================

    {
      path: '/change-password',
      name: 'ChangePassword',
      component: ChangePassword,
    },


    // =======================================================
    // 学生端
    //
    // 普通学生
    // 部门干事
    // 部门负责人
    // 部门副负责人
    //
    // 本质上都属于学生账号
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
        // 个人加分申请
        //
        // 证书、奖状等材料
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


        // ===================================================
        // 部门加减分申报
        //
        // 部门干事使用
        // ===================================================

        {
          path: 'department-apply',

          name: 'StudentDepartmentApply',

          component: () =>
            import(
              '@/views/student/DepartmentApply.vue'
              ),
        },


        // ===================================================
        // 部门负责人审核
        //
        // 部门负责人 / 副负责人
        //
        // 审核本部门干事提交的加减分
        // ===================================================

        {
          path: 'department-audit',

          name: 'DepartmentScoreAudit',

          component: () =>
            import(
              '@/views/student/DepartmentScoreAudit.vue'
              ),

          meta: {

            title: '部门申报审核',

            requiresDepartmentLeader: true,

          },

        },


        // ===================================================
        // 个人证书审核
        //
        // 仅档案部负责人 / 副负责人
        //
        // 学生个人上传证书之后，
        // 在这里审核。
        // ===================================================

        {
          path: 'certificate-audit',

          name: 'CertificateAudit',

          component: () =>
            import(
              '@/views/student/CertificateAudit.vue'
              ),

          meta: {

            title: '个人证书审核',

            requiresArchiveLeader: true,

          },

        },


        // ===================================================
        // 加减分汇总导出
        //
        // 档案部负责人 / 副负责人
        // 管理员
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
    // 旧地址兼容
    // =======================================================

    {
      path: '/archive/score-export',

      redirect: '/home/score-export',
    },


    // =======================================================
    // 管理员
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
        // 学生管理
        // ---------------------------------------------------

        {
          path: 'student',

          name: 'AdminStudent',

          component: StudentManage,
        },


        // ---------------------------------------------------
        // 学期管理
        // ---------------------------------------------------

        {
          path: 'semester',

          name: 'AdminSemester',

          component: SemesterManage,
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
        // 教师管理
        // ---------------------------------------------------

        {
          path: 'teacher',

          name: 'AdminTeacher',

          component: TeacherManage,

          meta: {

            title: '教师管理',

          },

        },


        // ---------------------------------------------------
        // 加分规则
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


        // ---------------------------------------------------
        // 管理员汇总导出
        // ---------------------------------------------------

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
        // 部门申报终审
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
// =========================================================

router.beforeEach(async (to) => {

  const userStr =
    localStorage.getItem('user')


  // =======================================================
  // 登录页
  // =======================================================

  if (to.path === '/login') {

    return true

  }


  // =======================================================
  // 未登录
  // =======================================================

  if (!userStr) {

    return '/login'

  }


  // =======================================================
  // 解析用户
  // =======================================================

  let user


  try {

    user =
      JSON.parse(userStr)

  } catch (error) {

    console.error(
      '用户缓存解析失败：',
      error
    )

    localStorage.removeItem(
      'user'
    )

    localStorage.removeItem(
      'token'
    )

    return '/login'

  }


  // =======================================================
  // 首次登录强制修改密码
  // =======================================================

  if (
    user.firstLogin === true
    ||
    user.firstLogin === 1
  ) {

    if (
      to.path === '/change-password'
    ) {

      return true

    }

    return '/change-password'

  }


  // =======================================================
  // 已完成首次改密的用户
  //
  // 允许管理员、辅导员、学生主动进入修改密码页面。
  // 否则后面的角色路由判断会把 /change-password
  // 重定向回各自首页。
  // =======================================================

  if (
    to.path === '/change-password'
  ) {

    return true

  }


  // =======================================================
  // 系统角色
  // =======================================================

  const role =
    user.role


  const rolePrefixMap = {

    学生:
      '/home',

    管理员:
      '/admin',

    辅导员:
      '/teacher',

  }


  // =======================================================
  // 无效角色
  // =======================================================

  if (
    !rolePrefixMap[role]
  ) {

    localStorage.removeItem(
      'user'
    )

    localStorage.removeItem(
      'token'
    )

    return '/login'

  }


  // =======================================================
  // 管理员导出
  // =======================================================

  if (
    to.meta.requiresAdminExportPermission
  ) {

    if (
      role === '管理员'
    ) {

      return true

    }


    if (
      role === '学生'
    ) {

      return '/home'

    }


    if (
      role === '辅导员'
    ) {

      return '/teacher'

    }


    return '/login'

  }


  // =======================================================
  // 档案部汇总导出
  // =======================================================

  if (
    to.meta.requiresArchiveExportPermission
  ) {

    if (
      role === '管理员'
    ) {

      return true

    }


    if (
      role === '学生'
    ) {

      return true

    }


    if (
      role === '辅导员'
    ) {

      return '/teacher'

    }


    return '/login'

  }


  // =======================================================
  // 部门负责人审核
  // =======================================================

  if (
    to.meta.requiresDepartmentLeader
  ) {

    if (
      role === '学生'
    ) {

      return true

    }


    return '/home'

  }


  // =======================================================
  // 档案部负责人审核
  // =======================================================

  if (
    to.meta.requiresArchiveLeader
  ) {

    if (
      role === '学生'
    ) {

      return true

    }


    return '/home'

  }


  // =======================================================
  // 管理员
  // =======================================================

  if (
    role === '管理员'
  ) {

    if (
      !to.path.startsWith(
        '/admin'
      )
    ) {

      return '/admin/adminHome'

    }


    return true

  }


  // =======================================================
  // 学生
  // =======================================================

  if (
    role === '学生'
  ) {

    if (
      !to.path.startsWith(
        '/home'
      )
    ) {

      return '/home'

    }


    return true

  }


  // =======================================================
  // 辅导员
  // =======================================================

  if (
    role === '辅导员'
  ) {

    if (
      !to.path.startsWith(
        '/teacher'
      )
    ) {

      return '/teacher'

    }


    return true

  }


  return '/login'

})


export default router
