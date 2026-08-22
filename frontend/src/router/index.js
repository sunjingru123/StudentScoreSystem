import { createRouter, createWebHistory } from 'vue-router'

// ==================== 登录 ====================
import Login from '@/views/Login.vue'

// ==================== 学生端布局&页面 ====================
import MainLayout from '@/layout/MainLayout.vue'
import Home from '@/views/Home.vue'
import Score from '@/views/Score.vue'
import Apply from '@/views/Apply.vue'
import Record from '@/views/Record.vue'
import StudentMessage from '@/views/student/Message.vue'

// ==================== 管理员端布局&页面 ====================
import AdminLayout from '@/layout/AdminLayout.vue'
import AdminHome from '@/views/admin/AdminHome.vue'
import ApplyAudit from '@/views/admin/ApplyAudit.vue'
import StudentManage from '@/views/admin/StudentManage.vue'
import RuleManage from '@/views/admin/RuleManage.vue'
import AdminScore from '@/views/admin/AdminScore.vue'
import ExcelImport from '@/views/admin/ExcelImport.vue'
// ==================== 辅导员端布局&页面 ====================
import TeacherLayout from '@/layout/TeacherLayout.vue'
import TeacherHome from '@/views/teacher/TeacherHome.vue'
import TeacherAudit from '@/views/teacher/TeacherAudit.vue'
import TeacherStudent from '@/views/teacher/TeacherStudent.vue'
import TeacherActivity from '@/views/teacher/TeacherActivity.vue'
import TeacherScore from '@/views/teacher/TeacherScore.vue'
import TeacherMessage from '@/views/teacher/TeacherMessage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 根路径重定向登录
    {
      path: '/',
      redirect: '/login',
    },
    // 登录页
    {
      path: '/login',
      name: 'Login',
      component: Login,
    },
    // ==================== 学生端路由 /home ====================
    {
      path: '/home',
      name: 'Student',
      component: MainLayout,
      children: [
        { path: '', name: 'StudentHome', component: Home },
        { path: 'score', name: 'StudentScore', component: Score },
        { path: 'apply', name: 'StudentApply', component: Apply },
        { path: 'record', name: 'StudentRecord', component: Record },
        { path: 'message', name: 'StudentMessage', component: StudentMessage },
        {
          path: 'student/:id',
          name: 'StudentDetail',
          component: () => import('@/views/admin/StudentDetail.vue'),
        },
        {
          path: '/student/score-apply',
          name: 'StudentScoreApply',
          component: () => import('@/views/student/ScoreApply.vue'),
        },
        {
          path: 'department-apply',
          name: 'StudentDepartmentApply',
          component: () => import('@/views/student/DepartmentApply.vue'),
        },
        {
          path: 'department-audit',
          component: () => import('@/views/home/DepartmentAudit.vue'),
        },
        {
          path: 'department-final-audit',
          component: () => import('@/views/home/DepartmentFinalAudit.vue'),
        },
      ],
    },

    // ==================== 管理员路由 /admin ====================
    {
      path: '/admin',
      name: 'Admin',
      component: AdminLayout,
      redirect: '/admin/adminHome',
      children: [
        { path: 'adminHome', name: 'AdminHome', component: AdminHome },
        { path: 'score', name: 'AdminScore', component: AdminScore },
        { path: 'apply', name: 'AdminApplyAudit', component: ApplyAudit },
        { path: 'student', name: 'AdminStudent', component: StudentManage },
        {
          path: 'student/:id',
          name: 'AdminStudentDetail',
          component: () => import('@/views/admin/StudentDetail.vue'),
        },
        { path: 'rule', name: 'AdminRule', component: RuleManage },
        {
          path: 'score-adjustment',
          name: 'AdminScoreAdjustment',
          component: () => import('@/views/admin/ScoreAdjustment.vue'),
        },
        {
          path: 'excel-import',
          name: 'AdminExcelImport',
          component: ExcelImport,
        },
      ],
    },

    // ==================== 辅导员路由 /teacher ====================
    {
      path: '/teacher',
      name: 'Teacher',
      component: TeacherLayout,
      children: [
        { path: '', name: 'TeacherHome', component: TeacherHome },
        // 修复：去掉开头/，使用相对路径
        {
          path: 'department-score-audit',
          name: 'CounselorDepartmentScoreAudit',
          component: () => import('@/views/teacher/DepartmentScoreAudit.vue'),
        },
        { path: 'audit', name: 'TeacherAudit', component: TeacherAudit },
        { path: 'student', name: 'TeacherStudent', component: TeacherStudent },
        { path: 'activity', name: 'TeacherActivity', component: TeacherActivity },
        { path: 'score', name: 'TeacherScore', component: TeacherScore },
        { path: 'message', name: 'TeacherMessage', component: TeacherMessage },
      ],
    },
  ],
})

router.addRoute({
  path: '/:pathMatch(.*)*',
  redirect: '/login',
})

// 全局路由守卫：登录校验 + 角色权限拦截
router.beforeEach((to) => {
  const userStr = localStorage.getItem('user')

  // 1. 登录页直接放行
  if (to.path === '/login') return true

  // 2. 未登录跳登录
  if (!userStr) return '/login'

  let user
  try {
    user = JSON.parse(userStr)
  } catch (err) {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    return '/login'
  }

  const role = user.role
  const rolePrefixMap = {
    学生: '/home',
    管理员: '/admin',
    辅导员: '/teacher',
  }

  // 3. 角色不存在，清缓存重登
  if (!rolePrefixMap[role]) {
    localStorage.removeItem('user')
    localStorage.removeItem('token')
    return '/login'
  }

  // 4. 角色与路由前缀不匹配，跳对应首页
  const targetPrefix = rolePrefixMap[role]

  if (role === '管理员') {
    if (!to.path.startsWith('/admin')) {
      return '/admin/adminHome'
    }
    return true
  }

  if (!to.path.startsWith(targetPrefix)) {
    return targetPrefix
  }

  return true
})

export default router
