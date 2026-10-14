import request from '@/api/request'

export function getDepartmentManageList() {
  return request.get('/department/manage-list')
}

export function addDepartment(data) {
  return request.post('/department', data)
}

export function updateDepartment(id, data) {
  return request.put(`/department/${id}`, data)
}

export function enableDepartment(id) {
  return request.put(`/department/${id}/enable`)
}

export function disableDepartment(id) {
  return request.put(`/department/${id}/disable`)
}
