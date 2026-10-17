/*
 * =========================================================
 * 受保护文件（申报材料 / 获奖凭证）的访问工具
 *
 * 背景：
 *   浏览器通过 <img>、<iframe>、window.open 直接访问
 *   /api/file/view/{id} 时无法携带 Authorization 头，
 *   后端会返回 401「请先登录」。
 *
 *   因此这里统一改用带鉴权头的 axios 请求
 *   （responseType: 'blob'）拉取文件字节，
 *   再通过 Object URL 在页面内展示或新窗口打开。
 * =========================================================
 */

import { ElMessage } from 'element-plus'

import request from '@/utils/request'


/*
 * 已生成的对象地址缓存：
 *   key   = 相对请求路径（不含 /api 前缀）
 *   value = { objectUrl, blob }
 *
 * 同一个文件重复查看时复用，避免重复下载。
 */

const objectUrlCache = new Map()


/*
 * 把后端存储的文件地址转换成 request 的相对路径。
 *
 * 后端返回的地址形如：
 *
 *   /api/file/view/10
 *
 * 而 request 的 baseURL 已经是 /api，
 * 所以需要去掉 /api 前缀，避免拼成 /api/api/...。
 */

function toRequestPath(url) {

  if (!url) {

    return ''

  }


  if (/^https?:\/\//i.test(url)) {

    return url

  }


  if (url.startsWith('/api/')) {

    return url.slice(4)

  }


  return url

}


/*
 * 拉取受保护文件，返回 { objectUrl, blob }。
 */

export async function loadProtectedFile(url) {

  const path = toRequestPath(url)

  if (!path) {

    throw new Error('缺少文件地址')

  }


  const cached = objectUrlCache.get(path)

  if (cached) {

    return cached

  }


  const response = await request.get(path, {
    responseType: 'blob'
  })


  const blob =
    response instanceof Blob
      ? response
      : new Blob([response])


  const result = {

    objectUrl: URL.createObjectURL(blob),

    blob

  }


  objectUrlCache.set(path, result)


  return result

}


/*
 * 只取对象地址。
 */

export async function loadProtectedFileUrl(url) {

  const { objectUrl } = await loadProtectedFile(url)

  return objectUrl

}


/*
 * 释放所有缓存的对象地址。
 * 建议组件卸载（onBeforeUnmount）时调用。
 */

export function releaseAllProtectedFileUrls() {

  objectUrlCache.forEach((cached) => {

    URL.revokeObjectURL(cached.objectUrl)

  })

  objectUrlCache.clear()

}


/*
 * 在新窗口打开受保护文件。
 *
 * 先同步打开空白窗口（避免被浏览器拦截），
 * 拉取成功后再把窗口地址指向对象地址。
 */

export async function openProtectedFile(url) {

  if (!url) {

    ElMessage.warning('没有可查看的材料')

    return

  }


  /*
   * 绝对地址（外链）直接打开。
   */

  if (/^https?:\/\//i.test(url)) {

    window.open(url, '_blank')

    return

  }


  /*
   * 历史数据可能只存了文件名，
   * 无法定位到后端文件，保持原有打开行为。
   */

  if (!url.startsWith('/')) {

    window.open(url, '_blank')

    return

  }


  const targetWindow = window.open('', '_blank')


  try {

    const objectUrl = await loadProtectedFileUrl(url)


    if (targetWindow && !targetWindow.closed) {

      targetWindow.location.href = objectUrl

    } else {

      window.open(objectUrl, '_blank')

    }

  } catch (error) {

    if (targetWindow && !targetWindow.closed) {

      targetWindow.close()

    }


    ElMessage.error('查看材料失败，请检查登录状态后重试')

  }

}
