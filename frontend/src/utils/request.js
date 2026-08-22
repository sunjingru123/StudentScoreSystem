import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080',

  timeout: 5000,
})

// 请求拦截器
request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')

  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }

  return config
})

export default request
