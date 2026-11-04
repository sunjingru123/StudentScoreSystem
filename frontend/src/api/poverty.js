import request from '@/utils/request'

/*
 * =========================================================
 * 贫困生名单
 *
 * 导入权限以后端 /poverty/permission 返回为准，
 * 前端只根据该结果控制按钮是否展示。
 * =========================================================
 */

export function getPovertyPermission() {
  return request.get('/poverty/permission')
}

export function downloadPovertyTemplate() {
  return request.get('/poverty/template', {
    responseType: 'blob',
  })
}

export function importPovertyStudents(formData) {
  return request.post('/poverty/import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}