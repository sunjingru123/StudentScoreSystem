import request from '@/utils/request'

// 获取学生列表
export function getStudentList() {
  return request({
    url: '/user/student/list',

    method: 'GET',
  })
}

// 获取全部学生（部门成员选择等场景使用）
export function getAllStudents() {
  return request({
    url: '/user/student/all',

    method: 'GET',
  })
}

// 禁用学生
export function disableStudent(id) {
  return request({
    url: '/user/student/disable/' + id,

    method: 'PUT',
  })
}

// 启用学生
export function enableStudent(id) {
  return request({
    url: '/user/student/enable/' + id,

    method: 'PUT',
  })
}

// 删除学生
//
// 注意：管理员端「学生管理」的删除走软删除接口
// （PUT /user/student/delete/{id}，带删除原因），
// 这个旧的硬删除接口保留仅为向后兼容，业务已不再使用。
export function deleteStudent(id) {
  return request({
    url: '/user/student/' + id,

    method: 'DELETE',
  })
}

// 管理员软删除学生（进入回收站）
export function deleteStudentWithReason(id, reason) {
  return request({
    url: '/user/student/delete/' + id,

    method: 'PUT',

    data: {
      reason,
    },
  })
}

// 管理员从回收站恢复学生
export function restoreStudent(id) {
  return request({
    url: '/user/student/restore/' + id,

    method: 'PUT',
  })
}

// 管理员：回收站学生列表
export function getRecycleStudents(params) {
  return request({
    url: '/user/student/recycle',

    method: 'GET',

    params,
  })
}

export function addStudent(data) {
  return request({
    url: '/user/student/add',

    method: 'POST',

    data: data,
  })
}

// 管理员重置学生密码
export function resetStudentPassword(id) {
  return request({
    url: `/user/student/reset-password/${id}`,
    method: 'PUT',
  })
}
