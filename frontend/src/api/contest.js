import request from '@/utils/request'

/*
 * =========================================================
 * 辅导员端：比赛（个人证书）筛选
 *
 * 筛选参数：
 *   keyword       学号 / 姓名 / 班级
 *   awardLevel    国家级 / 省级 / 校级 / 院级
 *   awardCategory A / B / C / OTHER
 *   povertyStatus 1 = 贫困生，0 = 非贫困生
 * =========================================================
 */

export function getContestList(params) {
  return request.get('/scoreApply/contest/list', {
    params,
  })
}