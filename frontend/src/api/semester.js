import request from '@/api/request'


/*
 * =========================================================
 * 获取全部学期
 * =========================================================
 */
export function getSemesterList() {

  return request.get(
    '/sysSemester/list'
  )
}


/*
 * =========================================================
 * 获取当前学期
 * =========================================================
 */
export function getCurrentSemester() {

  return request.get(
    '/sysSemester/current'
  )
}


/*
 * =========================================================
 * 新增学期
 * =========================================================
 */
export function addSemester(data) {

  return request.post(
    '/sysSemester/add',
    data
  )
}


/*
 * =========================================================
 * 修改学期
 * =========================================================
 */
export function updateSemester(
  id,
  data
) {

  return request.put(
    `/sysSemester/update/${id}`,
    data
  )
}


/*
 * =========================================================
 * 设置当前学期
 * =========================================================
 */
export function setCurrentSemester(
  id
) {

  return request.put(
    `/sysSemester/set-current/${id}`
  )
}
