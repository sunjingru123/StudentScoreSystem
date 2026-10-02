/*
 * =========================================================
 * 路由鉴权决策（纯函数，便于单测）
 *
 * 设计原则：
 *   1. 默认拒绝（Default Deny）：
 *      所有非白名单页面都必须显式命中规则才放行，
 *      没有任何“兜底无条件放行”。
 *   2. 区域隔离：
 *      管理员 -> /admin，辅导员 -> /teacher，
 *      学生（含部长 / 副部长 / 干事等学生干部）-> /home。
 *   3. 敏感页二次校验：
 *      在区域匹配的基础上，再校验后端返回的部门 / 档案部权限。
 * =========================================================
 */


/*
 * 角色 -> 所属区域
 *
 * 注意：
 *   “部长 / 副部长 / 干事”在后端 sys_position 里也是岗位名称，
 *   登录接口会把它作为 role 返回，但它们本质仍是学生端账号，
 *   所以统一归入 /home 区域。
 */

export const ROLE_AREA = {

  管理员: '/admin',

  辅导员: '/teacher',

  学生: '/home',

  部长: '/home',

  副部长: '/home',

  干事: '/home',

}


/*
 * 无需鉴权（或单独处理）的公开路径白名单。
 */

export const PUBLIC_PATHS = [

  '/login',

  '/404',

  '/403',

]


/*
 * 角色是否有效（能映射到区域）
 */

export function areaForRole(role) {

  return ROLE_AREA[role] || null

}


/*
 * 路径所属区域
 */

export function areaOfPath(path) {

  if (!path) {

    return null

  }


  if (path === '/admin' || path.startsWith('/admin/')) {

    return '/admin'

  }


  if (path === '/teacher' || path.startsWith('/teacher/')) {

    return '/teacher'

  }


  if (path === '/home' || path.startsWith('/home/')) {

    return '/home'

  }


  return null

}


/*
 * 核心判定。
 *
 * 入参：
 *   path        - 目标路径（to.path）
 *   flags       - 目标路由敏感标记（来自 to.matched 多级 meta 继承）
 *   role        - 权威角色
 *   permissions - 后端权限 { canDepartmentApply, departmentLeader, archiveLeader }
 *   firstLogin  - 是否首次登录需强制改密
 *
 * 返回：
 *   { action: 'allow' }
 *   { action: 'redirect', path, warning }
 *   { action: 'logout' }
 */

export function decideAccess({
  path,
  flags = {},
  role,
  permissions = {},
  firstLogin = false,
}) {

  /* ---------- 1. 白名单：登录页 ---------- */

  if (PUBLIC_PATHS.includes(path)) {

    return { action: 'allow' }

  }


  /* ---------- 2. 首次登录强制修改密码 ---------- */

  if (firstLogin) {

    if (path === '/change-password') {

      return { action: 'allow' }

    }


    return { action: 'redirect', path: '/change-password' }

  }


  /* ---------- 3. 已登录：允许主动进入修改密码页 ---------- */

  if (path === '/change-password') {

    return { action: 'allow' }

  }


  /* ---------- 4. 角色必须有效，否则视为登录态异常 ---------- */

  const area = areaForRole(role)

  if (!area) {

    return { action: 'logout' }

  }


  /* ---------- 5. 目标路径必须属于系统内已知区域 ---------- */

  const targetArea = areaOfPath(path)

  if (!targetArea) {

    return {

      action: 'redirect',

      path: area,

      warning: '页面不存在或没有访问权限',

    }

  }


  /* ---------- 6. 区域隔离：角色与区域必须一致（默认拒绝） ---------- */

  if (targetArea !== area) {

    return {

      action: 'redirect',

      path: area,

      warning: '你没有权限访问该页面',

    }

  }


  /* ---------- 7. 敏感页二次校验 ---------- */

  if (flags.requiresAdminExportPermission) {

    if (role !== '管理员') {

      return {

        action: 'redirect',

        path: area,

        warning: '你没有导出权限，无法访问该页面',

      }

    }

  }


  if (
    flags.requiresArchiveExportPermission ||
    flags.requiresArchiveLeader
  ) {

    if (role !== '管理员' && !permissions.archiveLeader) {

      return {

        action: 'redirect',

        path: area,

        warning: '你没有档案部权限，无法访问该页面',

      }

    }

  }


  if (flags.requiresDepartmentLeader) {

    if (!permissions.departmentLeader) {

      return {

        action: 'redirect',

        path: area,

        warning: '你不是部门负责人，无法访问部门审核页面',

      }

    }

  }


  if (flags.requiresDepartmentApply) {

    if (!permissions.canDepartmentApply) {

      return {

        action: 'redirect',

        path: area,

        warning: '你不是部门成员，无法进行部门申报',

      }

    }

  }


  /* ---------- 8. 放行 ---------- */

  return { action: 'allow' }

}
