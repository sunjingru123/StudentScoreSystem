import request from './request'

// 查询学生成绩

export function getScore(studentId) {
  return request.get(`/scoreStatistics/${studentId}`)
}
