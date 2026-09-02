import request from './request'

/**
 * 上传文件
 */
export function uploadFile(file) {

  const formData = new FormData()

  formData.append(
    'file',
    file
  )

  return request({
    url: '/fileInfo/upload',
    method: 'post',
    data: formData
  })
}
