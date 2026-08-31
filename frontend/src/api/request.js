import axios from 'axios'

const
  request = axios.create({
    // 只需要这一行，注意不要多写
    baseURL: '/api'
  })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')

  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }

  return config
})

export default request
