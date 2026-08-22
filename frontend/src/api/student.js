import request from '@/utils/request'

// 获取学生列表
export function getStudentList() {
  return request({
    url: '/user/student/list',

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
export function deleteStudent(id) {
  return request({
    url: '/user/student/' + id,

    method: 'DELETE',
  })
}

export function addStudent(data) {
  return request({
    url: '/user/student/add',

    method: 'POST',

    data: data,
  })
}
