import request from '@/api/request'

/**
 * =========================================================
 * 管理员成绩 API
 * =========================================================
 */

/**
 * 分页查询学生成绩列表
 *
 * GET /user/student/list
 *
 * 参数：
 * page
 * pageSize
 * studentNo
 * realName
 * className
 * status
 */
export function getAdminStudentList(params) {
  return request.get('/user/student/list', {
    params,
  })
}

/**
 * 查询某个学生的成绩明细
 *
 * GET /scoreStatistics/admin/{studentId}/records
 */
export function getAdminStudentScores(
  studentId,
  params = {},
) {
  return request.get(
    `/scoreStatistics/admin/${studentId}/records`,
    {
      params,
    },
  )
}

/**
 * 查询某个学生的总成绩
 *
 * GET /scoreStatistics/admin/{studentId}
 */
export function getAdminStudentTotal(studentId) {
  return request.get(
    `/scoreStatistics/admin/${studentId}`,
  )
}

/**
 * 隐藏成绩
 *
 * 这里根据你原来的接口保留。
 */
export function hideScore(id) {
  return request.put(
    `/scoreRecord/admin/hide/${id}`,
  )
}

/**
 * 恢复成绩
 */
export function showScore(id) {
  return request.put(
    `/scoreRecord/admin/show/${id}`,
  )
}
