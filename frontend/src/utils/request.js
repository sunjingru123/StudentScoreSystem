import axios from 'axios'

const request = axios.create({
  // 弃用环境变量，直接写死为相对路径 '/api'
  // 这样无论是在电脑还是手机，它都会自动拼接当前隧道的域名
  baseURL: '/api',
  timeout: 15000,
  withCredentials: false
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

// ... 后面的响应拦截器保持不变
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default request
