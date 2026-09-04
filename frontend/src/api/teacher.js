import request from '@/api/request'

// =========================================================
// 教师管理
// =========================================================

// 获取教师列表
export function getTeacherList() {
  return request.get('/admin/teacher/list')
}

// 获取部门列表
export function getTeacherDepartments() {
  return request.get('/admin/teacher/departments')
}

// 新增教师
export function addTeacher(data) {
  return request.post('/admin/teacher/add', data)
}

// 修改教师
export function updateTeacher(id, data) {
  return request.put(`/admin/teacher/update/${id}`, data)
}

// 停用教师
export function disableTeacher(id) {
  return request.put(`/admin/teacher/disable/${id}`)
}

// 启用教师
export function enableTeacher(id) {
  return request.put(`/admin/teacher/enable/${id}`)
}

// 重置密码
export function resetTeacherPassword(id) {
  return request.put(`/admin/teacher/reset-password/${id}`)
}
