import request from './request'

/**
 * 查询某个学生全部成绩
 */
export function getAdminStudentScores(studentId) {
  return request.get(`/admin/score/student/${studentId}`)
}

/**
 * 查询学生总成绩
 */
export function getAdminStudentTotal(studentId) {
  return request.get(`/admin/score/student/${studentId}/total`)
}

/**
 * 隐藏成绩
 */
export function hideScore(id) {
  return request.put(`/admin/score/hide/${id}`)
}

/**
 * 恢复成绩
 */
export function showScore(id) {
  return request.put(`/admin/score/show/${id}`)
}
