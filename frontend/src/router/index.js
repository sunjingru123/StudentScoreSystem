import {
  createRouter,
  createWebHistory
} from 'vue-router'

import request from '@/utils/request'

import {
  ElMessage
} from 'element-plus'

import {
  PUBLIC_PATHS,
  decideAccess
} from '@/router/access'


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

          meta: {

            title: '部门加减分申报',

            requiresDepartmentApply: true,

          },

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
// 权限与首页
// =========================================================

const authCache = {

  token: null,

  role: '',

  canDepartmentApply: false,

  departmentLeader: false,

  archiveLeader: false,

  loaded: false,

  pending: null,

  pendingToken: null,

}


// 清空登录态与鉴权缓存
function clearAuth() {

  localStorage.removeItem('user')

  localStorage.removeItem('token')


  authCache.token = null

  authCache.role = ''

  authCache.canDepartmentApply = false

  authCache.departmentLeader = false

  authCache.archiveLeader = false

  authCache.loaded = false

}


// 读取权威鉴权上下文。
//
// 角色来自后端 /user/info，
// 部门 / 档案部权限来自后端 /departmentScoreApply/my-permissions。
//
// 同一个 token 只拉取一次：
//   - 刷新页面后重新拉取；
//   - 重新登录（token 变化）后自动失效重拉。
async function loadAuthContext(storedUser) {

  const token =
    localStorage.getItem('token')


  if (!token) {

    return {

      role: '',

      canDepartmentApply: false,

      departmentLeader: false,

      archiveLeader: false,

    }

  }


  if (authCache.loaded && authCache.token === token) {

    return authCache

  }


  if (authCache.pending && authCache.pendingToken === token) {

    return authCache.pending

  }


  const pending =
    (async () => {

      let role = ''

      let canDepartmentApply = false

      let departmentLeader = false

      let archiveLeader = false


      try {

        const infoRes =
          await request.get('/user/info')


        role =
          infoRes?.data?.role || ''

      } catch (error) {

        console.error(
          '获取当前用户角色失败：',
          error
        )

        role = ''

      }


      try {

        const permissionRes =
          await request.get(
            '/departmentScoreApply/my-permissions'
          )


        const data =
          permissionRes?.data || {}


        canDepartmentApply =
          data.canDepartmentApply === true ||
          Number(data.canDepartmentApply) === 1


        departmentLeader =
          data.canDepartmentAudit === true ||
          Number(data.canDepartmentAudit) === 1


        const departments =
          Array.isArray(data.departments)
            ? data.departments
            : []


        archiveLeader =
          departments.some(
            department =>

              department &&
              department.departmentName === '档案部' &&
              (
                department.position === '干事' ||
                department.position === '副部长' ||
                department.position === '部长'
              )
          )

      } catch (error) {

        console.error(
          '获取部门权限失败：',
          error
        )

        departmentLeader = false

        archiveLeader = false

        canDepartmentApply = false

      }


      // 后端未返回角色时，回退到登录时写入的本地角色。
      // 仅作兜底，敏感权限始终以后端返回为准。
      if (!role) {

        role =
          storedUser?.role || ''

      }


      authCache.token = token

      authCache.role = role

      authCache.canDepartmentApply = canDepartmentApply

      authCache.departmentLeader = departmentLeader

      authCache.archiveLeader = archiveLeader

      authCache.loaded = true


      return authCache

    })()


  authCache.pending = pending

  authCache.pendingToken = token


  try {

    return await pending

  } finally {

    if (authCache.pending === pending) {

      authCache.pending = null

      authCache.pendingToken = null

    }

  }

}


// 把判定结果转换为 vue-router 守卫的返回值
function applyDecision(decision) {

  if (decision.action === 'allow') {

    return true

  }


  if (decision.action === 'logout') {

    clearAuth()

    ElMessage.error('登录状态异常，请重新登录')

    return { path: '/login', replace: true }

  }


  if (decision.warning) {

    ElMessage.warning(decision.warning)

  }


  return {

    path: decision.path,

    replace: true,

  }

}


// =========================================================
// 全局路由守卫（默认拒绝 + 白名单）
// =========================================================

router.beforeEach(async (to) => {

  /* ---------- 白名单：登录页 / 404 / 403 ---------- */

  if (PUBLIC_PATHS.includes(to.path)) {

    return true

  }


  const token =
    localStorage.getItem('token')


  const userStr =
    localStorage.getItem('user')


  /* ---------- 未登录：直接阻断，不调用后端 ---------- */

  if (!token || !userStr) {

    return { path: '/login', replace: true }

  }


  /* ---------- 解析本地用户缓存 ---------- */

  let storedUser


  try {

    storedUser =
      JSON.parse(userStr)

  } catch (error) {

    console.error(
      '用户缓存解析失败：',
      error
    )

    clearAuth()

    return { path: '/login', replace: true }

  }


  const firstLogin =
    storedUser.firstLogin === true ||
    storedUser.firstLogin === 1


  /* ---------- 首次登录优先处理（不调用后端） ---------- */

  if (firstLogin) {

    return applyDecision(
      decideAccess({

        path: to.path,

        role: storedUser.role,

        firstLogin: true,

      })
    )

  }


  /* ---------- 加载权威鉴权上下文（带缓存） ---------- */

  const auth =
    await loadAuthContext(storedUser)


  /* ---------- 收集目标路由的多级敏感标记（to.matched） ---------- */

  const flags = {

    requiresAdminExportPermission:
      to.matched.some(
        record =>
          record.meta?.requiresAdminExportPermission === true
      ),

    requiresArchiveExportPermission:
      to.matched.some(
        record =>
          record.meta?.requiresArchiveExportPermission === true
      ),

    requiresDepartmentLeader:
      to.matched.some(
        record =>
          record.meta?.requiresDepartmentLeader === true
      ),

    requiresDepartmentApply:
      to.matched.some(
        record =>
          record.meta?.requiresDepartmentApply === true
      ),

    requiresArchiveLeader:
      to.matched.some(
        record =>
          record.meta?.requiresArchiveLeader === true
      ),

  }


  const decision =
    decideAccess({

      path: to.path,

      flags,

      role: auth.role,

      permissions: {

        canDepartmentApply: auth.canDepartmentApply,

        departmentLeader: auth.departmentLeader,

        archiveLeader: auth.archiveLeader,

      },

    })


  return applyDecision(decision)

})


export default router
