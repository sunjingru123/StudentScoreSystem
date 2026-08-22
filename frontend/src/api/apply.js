import request from '@/utils/request'
// 提交申请

export function addApply(data) {
  return request.post('/scoreApply/add', data)
}
export function getMyApply() {
  return request({
    url: '/scoreApply/my',
    method: 'GET',
  })
}
