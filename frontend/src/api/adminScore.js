import request from '@/api/request'


/**
 * =========================================================
 * 管理员学生成绩 API
 * ========================================================= */


/**
 * =========================================================
 * 1. 获取学生列表
 *
 * GET /user/student/list
 * =========================================================
 */

export function getAdminStudentList(params) {

  return request.get(
    '/user/student/list',
    {
      params,
    },
  )

}


/**
 * =========================================================
 * 2. 获取某个学生的全部成绩明细
 *
 * 管理员专用
 *
 * GET /admin/score/student/{studentId}
 *
 * 注意：
 * 这里后端返回的是：
 *
 * Result<List<AdminScoreDetailVO>>
 *
 * 不是 Page
 * =========================================================
 */

export function getAdminStudentScores(studentId) {

  return request.get(
    `/admin/score/student/${studentId}`,
  )

}


/**
 * =========================================================
 * 3. 获取某个学生当前综合评分
 *
 * GET /scoreStatistics/admin/{studentId}
 *
 * 管理员统计接口
 * =========================================================
 */

export function getAdminStudentTotal(studentId) {

  return request.get(
    `/scoreStatistics/admin/${studentId}`,
  )

}


/**
 * =========================================================
 * 4. 隐藏成绩
 *
 * PUT /admin/score/hide/{id}
 * =========================================================
 */

export function hideScore(id) {

  return request.put(
    `/admin/score/hide/${id}`,
  )

}


/**
 * =========================================================
 * 5. 恢复成绩
 *
 * PUT /admin/score/show/{id}
 * =========================================================
 */

export function showScore(id) {

  return request.put(
    `/admin/score/show/${id}`,
  )

}
