import request from '@/utils/request'

// =========================================================
// 成绩删除申请
//
// 后端统一返回 Result{ code, message, data }，
// 由 utils/request.js 拦截器解包后返回。
// =========================================================

/**
 * 当前用户成绩删除相关权限
 */
export function getDeletePermission() {
  return request.get('/scoreDeleteApply/permission')
}

/**
 * 提交删除申请（档案部部长 / 副部长）
 */
export function submitDeleteApply(data) {
  return request.post('/scoreDeleteApply/submit', data)
}

/**
 * 我的删除申请
 */
export function getMyDeleteApplies() {
  return request.get('/scoreDeleteApply/mine')
}

/**
 * 待我审核的删除申请
 */
export function getPendingDeleteApplies() {
  return request.get('/scoreDeleteApply/pending')
}

/**
 * 我已处理的删除申请
 */
export function getProcessedDeleteApplies() {
  return request.get('/scoreDeleteApply/processed')
}

/**
 * 审核通过（执行删除）
 */
export function approveDeleteApply(id, remark) {
  return request.put(`/scoreDeleteApply/approve/${id}`, { remark })
}

/**
 * 审核驳回
 */
export function rejectDeleteApply(id, remark) {
  return request.put(`/scoreDeleteApply/reject/${id}`, { remark })
}
