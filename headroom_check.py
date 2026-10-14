"""
Headroom 压缩无损性校验：
把 headroom_retrieve 还原出的「原文」与 headroom_compress 输出的「压缩表示」逐字段严格比对。
数据：低重复 JSON-C（hash = b3612f63cc778c92642ac9c9）
"""

import json
import re
import sys

# ---- 1) headroom_retrieve 还原出的原文（JSON-C）----
ORIGINAL = r"""{"schemaVersion":"v7","exportedBy":"zhao.liu","exportedAt":"2025-09-07T23:41:08+08:00","code":200,"msg":"success","data":{"total":40,"page":3,"pageSize":40,"records":[
{"id":8841201,"studentNo":"S2021-0473-a9f","name":"欧阳梓萱","gender":"女","className":"电子信息科学与技术2103","department":"电子信息与集成电路学院","semester":"2024-2025-2","status":1,"statusText":"正常在读","totalScore":68.42,"bonusScore":3.17,"deductScore":14.75,"createTime":"2025-03-11 07:42:19","updateTime":"2025-06-28 21:09:53","remark":"参加全国大学生电子设计竞赛省赛获二等奖，评审编号 EC-2025-4482，凭证已归档"},
{"id":3779016,"studentNo":"S2019-8821-b3c","name":"司马承宇","gender":"男","className":"机械设计制造及其自动化1907","department":"机械与能源动力工程学院","semester":"2022-2023-1","status":0,"statusText":"冻结","totalScore":41.09,"bonusScore":0.84,"deductScore":29.91,"createTime":"2022-09-14 13:07:55","updateTime":"2023-01-19 04:26:31","remark":"因欠缴学费账户冻结，教务处通知编号 FIN-2023-1174，待财务确认"},
{"id":5023347,"studentNo":"S2022-1130-c7d","name":"上官婉清","gender":"女","className":"汉语言文学2209","department":"文学与传媒学院","semester":"2023-2024-2","status":1,"statusText":"正常在读","totalScore":93.77,"bonusScore":26.63,"deductScore":12.86,"createTime":"2024-02-27 19:33:41","updateTime":"2024-07-02 08:15:09","remark":"校刊发表散文三篇，稿费凭证 GR-2024-2210，另有诗歌获奖记录"},
{"id":1198453,"studentNo":"S2020-6642-d0e","name":"独孤鸿飞","gender":"男","className":"应用物理学2002","department":"物理科学与技术学院","semester":"2023-2024-1","status":2,"statusText":"休学","totalScore":57.18,"bonusScore":8.42,"deductScore":11.24,"createTime":"2023-08-30 05:11:27","updateTime":"2024-01-05 16:58:44","remark":"因病申请休学一学期，医院证明 MED-2024-0093，预计秋季复学"},
{"id":6630271,"studentNo":"S2023-2298-e1f","name":"慕容雪见","gender":"女","className":"临床医学2301","department":"基础医学院","semester":"2024-2025-1","status":1,"statusText":"正常在读","totalScore":88.53,"bonusScore":19.08,"deductScore":20.55,"createTime":"2024-09-02 11:24:36","updateTime":"2025-01-11 03:47:22","remark":"参与解剖学标本制作志愿工作 62 小时，志愿编号 VOL-2025-3381"},
{"id":2287604,"studentNo":"S2019-3310-f2a","name":"皇甫墨言","gender":"男","className":"金融工程1905","department":"经济与金融学院","semester":"2022-2023-2","status":4,"statusText":"毕业","totalScore":96.31,"bonusScore":31.46,"deductScore":15.15,"createTime":"2023-01-21 22:09:03","updateTime":"2023-06-30 09:52:17","remark":"已通过毕业论文答辩，答辩编号 DEF-2023-0917，学分齐全"},
{"id":7761490,"studentNo":"S2021-7756-a3b","name":"长孙无极","gender":"男","className":"软件工程2108","department":"计算机与人工智能学院","semester":"2024-2025-1","status":1,"statusText":"正常在读","totalScore":74.66,"bonusScore":12.35,"deductScore":17.69,"createTime":"2024-08-19 14:36:12","updateTime":"2025-02-03 20:41:58","remark":"参与开源社区提交 PR 编号 OSS-4417，被合并两次，含代码评审纪要"},
{"id":3145829,"studentNo":"S2020-5521-b8c","name":"东方清野","gender":"女","className":"环境设计2006","department":"艺术与设计学院","semester":"2023-2024-1","status":1,"statusText":"正常在读","totalScore":82.07,"bonusScore":15.92,"deductScore":13.85,"createTime":"2023-09-05 08:18:47","updateTime":"2024-03-22 12:03:39","remark":"毕业设计展作品《潮汐》获校级优秀奖，展览编号 ART-2024-0762"},
{"id":9952367,"studentNo":"S2022-9963-c4d","name":"南宫问天","gender":"男","className":"通信工程2204","department":"信息与通信工程学院","semester":"2024-2025-2","status":0,"statusText":"冻结","totalScore":49.28,"bonusScore":2.71,"deductScore":23.43,"createTime":"2024-02-14 17:55:21","updateTime":"2024-11-08 06:29:14","remark":"学分账户冻结，涉及跨校转学审核编号 TRA-2024-5530，待对方院校回函"},
{"id":4417830,"studentNo":"S2023-4472-d9e","name":"轩辕若冰","gender":"女","className":"生物技术2305","department":"生命科学学院","semester":"2024-2025-1","status":1,"statusText":"正常在读","totalScore":91.14,"bonusScore":24.87,"deductScore":13.73,"createTime":"2024-09-16 03:29:58","updateTime":"2025-04-01 18:12:45","remark":"参与基因编辑课题实验，实验记录编号 LAB-2025-1693，导师签字确认"},
{"id":5829074,"studentNo":"S2019-7718-e5f","name":"令狐冲虚","gender":"男","className":"数学与应用数学1903","department":"数学与统计学院","semester":"2022-2023-1","status":4,"statusText":"毕业","totalScore":89.36,"bonusScore":22.51,"deductScore":11.15,"createTime":"2022-09-08 20:44:33","updateTime":"2023-06-28 15:07:52","remark":"保研至本校研究生，推荐信编号 REC-2023-2284，最终成绩位列前三"},
{"id":6073821,"studentNo":"S2021-3309-f3a","name":"司徒远航","gender":"男","className":"车辆工程2102","department":"汽车与交通工程学院","semester":"2024-2025-1","status":1,"statusText":"正常在读","totalScore":66.83,"bonusScore":7.44,"deductScore":20.61,"createTime":"2024-08-27 09:13:06","updateTime":"2025-02-19 23:50:28","remark":"参与大学生方程式赛车设计，团队排名第 11，赛事编号 FSAE-2025-0088"},
{"id":1594726,"studentNo":"S2020-1187-a1b","name":"夏侯安宁","gender":"女","className":"会计学2001","department":"管理学与商学院","semester":"2023-2024-2","status":1,"statusText":"正常在读","totalScore":87.29,"bonusScore":17.66,"deductScore":10.37,"createTime":"2024-02-05 12:48:19","updateTime":"2024-08-14 05:36:07","remark":"通过注册会计师两门考试，成绩单编号 CPA-2024-3390，附件已上传"},
{"id":7306418,"studentNo":"S2022-6634-b6c","name":"万俟清欢","gender":"女","className":"旅游管理2203","department":"地理与旅游学院","semester":"2023-2024-1","status":3,"statusText":"结业","totalScore":52.94,"bonusScore":5.28,"deductScore":18.34,"createTime":"2023-08-22 16:02:55","updateTime":"2024-06-11 11:27:43","remark":"按结业处理，学籍编号 REG-2024-7708，可申请补考换取毕业证"},
{"id":2840517,"studentNo":"S2023-8890-c2d","name":"钟离陌离","gender":"男","className":"数字媒体艺术2306","department":"艺术与设计学院","semester":"2024-2025-2","status":1,"statusText":"正常在读","totalScore":79.51,"bonusScore":14.09,"deductScore":16.58,"createTime":"2024-02-21 07:27:31","updateTime":"2025-05-03 13:44:09","remark":"短视频作品获校赛金奖，作品编号 VID-2025-2247，平台播放量破十万"},
{"id":3938265,"studentNo":"S2019-4455-d8e","name":"端木青云","gender":"男","className":"土木工程1904","department":"土木与建筑工程学院","semester":"2022-2023-2","status":1,"statusText":"正常在读","totalScore":73.16,"bonusScore":9.73,"deductScore":19.57,"createTime":"2023-01-11 04:19:26","updateTime":"2023-09-27 19:38:51","remark":"参与桥梁结构检测实习，实习编号 INT-2023-6612，单位反馈良好"},
{"id":8471039,"studentNo":"S2021-2213-e9f","name":"赫连明轩","gender":"女","className":"药学2107","department":"药学院","semester":"2024-2025-1","status":0,"statusText":"冻结","totalScore":44.07,"bonusScore":1.83,"deductScore":26.91,"createTime":"2024-08-31 23:05:14","updateTime":"2025-03-08 02:16:37","remark":"因实验安全事故调查冻结，案件编号 SAF-2025-0041，听证会待安排"},
{"id":5207394,"studentNo":"S2020-9931-f0a","name":"澹台明月","gender":"女","className":"法学2008","department":"法学院","semester":"2023-2024-1","status":1,"statusText":"正常在读","totalScore":90.68,"bonusScore":21.34,"deductScore":9.66,"createTime":"2023-09-12 18:41:02","updateTime":"2024-04-30 10:55:26","remark":"模拟法庭大赛最佳辩手，赛事编号 MOOT-2024-1130，奖状已归档"},
{"id":6814952,"studentNo":"S2022-5587-a4b","name":"公孙止戈","gender":"男","className":"材料成型及控制工程2201","department":"材料科学与工程学院","semester":"2024-2025-2","status":1,"statusText":"正常在读","totalScore":71.33,"bonusScore":11.06,"deductScore":18.73,"createTime":"2024-02-09 10:52:44","updateTime":"2024-12-17 15:31:08","remark":"参与新型合金疲劳测试项目，数据编号 MAT-2024-9982，论文在投"},
{"id":9652180,"studentNo":"S2023-1124-b5c","name":"百里守约","gender":"男","className":"英语2304","department":"外国语学院","semester":"2023-2024-1","status":1,"statusText":"正常在读","totalScore":85.94,"bonusScore":16.47,"deductScore":11.53,"createTime":"2023-08-14 21:28:39","updateTime":"2024-05-19 07:03:57","remark":"通过 CATTI 三级笔译，证书编号 CATTI-2024-2871，成绩单随附"},
{"id":3071645,"studentNo":"S2019-6678-c6d","name":"太史慈恩","gender":"女","className":"食品科学与工程1909","department":"食品科学学院","semester":"2022-2023-1","status":4,"statusText":"毕业","totalScore":81.47,"bonusScore":13.82,"deductScore":12.35,"createTime":"2022-09-19 15:37:11","updateTime":"2023-06-25 22:49:33","remark":"已入职食品检测机构，就业协议编号 EMP-2023-4471，档案已转出"},
{"id":7482936,"studentNo":"S2021-8899-d7e","name":"呼延觉罗","gender":"男","className":"机器人工程2105","department":"自动化与机器人学院","semester":"2024-2025-1","status":1,"statusText":"正常在读","totalScore":77.62,"bonusScore":13.19,"deductScore":15.57,"createTime":"2024-08-23 06:11:28","updateTime":"2025-01-30 14:22:41","remark":"参与机器人格斗赛获省级三等奖，队伍编号 BOT-2025-0726"},
{"id":1935704,"studentNo":"S2020-3345-e8f","name":"宇文长风","gender":"男","className":"测绘工程2007","department":"测绘与地理信息学院","semester":"2023-2024-2","status":1,"statusText":"正常在读","totalScore":69.85,"bonusScore":6.91,"deductScore":17.06,"createTime":"2024-02-18 03:44:52","updateTime":"2024-09-06 16:10:19","remark":"完成无人机航测外业，作业编号 SUR-2024-5518，含坐标系转换报告"},
{"id":6218473,"studentNo":"S2022-1102-f9a","name":"尉迟恭行","gender":"女","className":"公共事业管理2202","department":"公共管理学院","semester":"2023-2024-1","status":2,"statusText":"休学","totalScore":58.73,"bonusScore":7.36,"deductScore":18.63,"createTime":"2023-08-26 12:23:47","updateTime":"2024-02-01 09:41:33","remark":"参军入伍申请休学，入伍编号 MIL-2024-0035，退役后复学"},
{"id":4569012,"studentNo":"S2023-7754-a0b","name":"欧阳听风","gender":"男","className":"物流管理2308","department":"管理学与商学院","semester":"2024-2025-2","status":1,"statusText":"正常在读","totalScore":83.28,"bonusScore":15.64,"deductScore":12.36,"createTime":"2024-02-25 20:17:04","updateTime":"2025-06-05 04:58:22","remark":"参与供应链优化课题，数据模型编号 SCM-2025-3392，企业合作方盖章"},
{"id":1730486,"studentNo":"S2019-2289-b1c","name":"东方既白","gender":"女","className":"化学工程与工艺1906","department":"化学与分子工程学院","semester":"2022-2023-2","status":1,"statusText":"正常在读","totalScore":86.11,"bonusScore":18.29,"deductScore":12.18,"createTime":"2023-01-16 09:07:38","updateTime":"2023-08-12 21:34:50","remark":"参与催化剂合成实验，危险品操作证编号 HAZ-2023-1147，已备案"},
{"id":5306714,"studentNo":"S2021-5560-c3d","name":"慕容听雪","gender":"女","className":"水利水电工程2101","department":"水利与土木工程学院","semester":"2024-2025-1","status":1,"statusText":"正常在读","totalScore":75.49,"bonusScore":12.07,"deductScore":16.58,"createTime":"2024-08-11 17:41:26","updateTime":"2025-02-22 11:15:03","remark":"参与水工模型试验，试验编号 HYD-2025-2208，数据处理脚本已提交"},
{"id":9182460,"studentNo":"S2020-4478-d4e","name":"长孙无咎","gender":"男","className":"工程管理2009","department":"土木与建筑工程学院","semester":"2023-2024-1","status":1,"statusText":"正常在读","totalScore":80.02,"bonusScore":14.55,"deductScore":14.53,"createTime":"2023-09-01 05:56:14","updateTime":"2024-03-15 18:29:47","remark":"考取一级建造师法规科目，成绩编号 CON-2024-8830，备考记录完整"},
{"id":2647193,"studentNo":"S2022-3341-e5f","name":"南宫惊鸿","gender":"男","className":"物联网工程2206","department":"计算机与人工智能学院","semester":"2024-2025-2","status":0,"statusText":"冻结","totalScore":47.36,"bonusScore":3.06,"deductScore":25.58,"createTime":"2024-02-16 14:52:31","updateTime":"2024-10-25 07:44:16","remark":"因严重违纪调查冻结，纪检编号 DIS-2024-0912，处分决定待公示"},
{"id":8053927,"studentNo":"S2023-6690-f6a","name":"皇甫静姝","gender":"女","className":"新闻学2302","department":"文学与传媒学院","semester":"2023-2024-1","status":1,"statusText":"正常在读","totalScore":89.81,"bonusScore":20.43,"deductScore":10.62,"createTime":"2023-08-18 11:31:09","updateTime":"2024-06-20 13:27:55","remark":"校报记者站站长，采编稿件编号 PRS-2024-6673，深度报道获省级奖"},
{"id":3561820,"studentNo":"S2019-9934-a7b","name":"令狐未央","gender":"女","className":"农业资源与环境1902","department":"资源与环境学院","semester":"2022-2023-1","status":4,"statusText":"毕业","totalScore":78.64,"bonusScore":11.38,"deductScore":12.74,"createTime":"2022-09-04 19:22:47","updateTime":"2023-06-29 05:18:36","remark":"考取选调生，录用编号 GOV-2023-7756，报到手续已办理完成"},
{"id":4702381,"studentNo":"S2021-2267-b8c","name":"轩辕孤鸿","gender":"男","className":"核工程与核技术2103","department":"核科学与技术学院","semester":"2024-2025-1","status":1,"statusText":"正常在读","totalScore":72.95,"bonusScore":10.28,"deductScore":17.33,"createTime":"2024-08-25 08:49:15","updateTime":"2025-03-12 22:06:40","remark":"参与辐射防护培训，培训编号 RAD-2025-4409，实操考核合格"},
{"id":6924158,"studentNo":"S2020-7701-c9d","name":"司徒望舒","gender":"女","className":"工业设计2005","department":"艺术与设计学院","semester":"2023-2024-2","status":1,"statusText":"正常在读","totalScore":84.16,"bonusScore":16.02,"deductScore":11.86,"createTime":"2024-02-12 23:37:58","updateTime":"2024-08-29 09:51:24","remark":"设计作品入围红点概念奖，投稿编号 RDT-2024-5581，模型图纸已存档"},
{"id":1378654,"studentNo":"S2022-8830-d0e","name":"万俟孤云","gender":"男","className":"能源与动力工程2207","department":"机械与能源动力工程学院","semester":"2024-2025-2","status":1,"statusText":"正常在读","totalScore":70.48,"bonusScore":9.15,"deductScore":18.67,"createTime":"2024-02-24 15:08:43","updateTime":"2025-05-27 03:39:12","remark":"参与锅炉效率测试项目，测试编号 THM-2025-6624，能耗报告已提交"},
{"id":5863709,"studentNo":"S2023-1146-e1f","name":"钟离若梦","gender":"女","className":"哲学2301","department":"哲学与社会发展学院","semester":"2023-2024-1","status":1,"statusText":"正常在读","totalScore":92.53,"bonusScore":25.71,"deductScore":13.18,"createTime":"2023-08-20 04:26:31","updateTime":"2024-07-15 17:43:08","remark":"在核心期刊发表论文一篇，期刊编号 JRN-2024-9925，含外审意见回复"},
{"id":2097316,"studentNo":"S2021-4408-f2a","name":"端木长歌","gender":"男","className":"智能制造工程2104","department":"自动化与机器人学院","semester":"2024-2025-1","status":0,"statusText":"冻结","totalScore":50.81,"bonusScore":4.53,"deductScore":23.72,"createTime":"2024-08-17 21:14:26","updateTime":"2025-01-08 12:37:49","remark":"学籍信息核验异常冻结，核验编号 VER-2025-1173，需补充身份材料"}
]}}"""

# ---- 2) headroom_compress 输出的压缩表示（同一份数据）----
COMPRESSED = r"""{"schemaVersion":"v7","exportedBy":"zhao.liu","exportedAt":"2025-09-07T23:41:08+08:00","code":200,"msg":"success","data":{"total":40,"page":3,"pageSize":40,"records":"[36]{bonusScore:float,className:string,createTime:string,deductScore:float,department:string,gender:string,id:int,name:string,remark:string,semester:string,status:int,statusText:string,studentNo:string,totalScore:float,updateTime:string}\n3.17,电子信息科学与技术2103,2025-03-11 07:42:19,14.75,电子信息与集成电路学院,女,8841201,欧阳梓萱,参加全国大学生电子设计竞赛省赛获二等奖，评审编号 EC-2025-4482，凭证已归档,2024-2025-2,1,正常在读,S2021-0473-a9f,68.42,2025-06-28 21:09:53\n0.84,机械设计制造及其自动化1907,2022-09-14 13:07:55,29.91,机械与能源动力工程学院,男,3779016,司马承宇,因欠缴学费账户冻结，教务处通知编号 FIN-2023-1174，待财务确认,2022-2023-1,0,冻结,S2019-8821-b3c,41.09,2023-01-19 04:26:31\n26.63,汉语言文学2209,2024-02-27 19:33:41,12.86,文学与传媒学院,女,5023347,上官婉清,校刊发表散文三篇，稿费凭证 GR-2024-2210，另有诗歌获奖记录,2023-2024-2,1,正常在读,S2022-1130-c7d,93.77,2024-07-02 08:15:09\n8.42,应用物理学2002,2023-08-30 05:11:27,11.24,物理科学与技术学院,男,1198453,独孤鸿飞,因病申请休学一学期，医院证明 MED-2024-0093，预计秋季复学,2023-2024-1,2,休学,S2020-6642-d0e,57.18,2024-01-05 16:58:44\n19.08,临床医学2301,2024-09-02 11:24:36,20.55,基础医学院,女,6630271,慕容雪见,参与解剖学标本制作志愿工作 62 小时，志愿编号 VOL-2025-3381,2024-2025-1,1,正常在读,S2023-2298-e1f,88.53,2025-01-11 03:47:22\n31.46,金融工程1905,2023-01-21 22:09:03,15.15,经济与金融学院,男,2287604,皇甫墨言,已通过毕业论文答辩，答辩编号 DEF-2023-0917，学分齐全,2022-2023-2,4,毕业,S2019-3310-f2a,96.31,2023-06-30 09:52:17\n12.35,软件工程2108,2024-08-19 14:36:12,17.69,计算机与人工智能学院,男,7761490,长孙无极,参与开源社区提交 PR 编号 OSS-4417，被合并两次，含代码评审纪要,2024-2025-1,1,正常在读,S2021-7756-a3b,74.66,2025-02-03 20:41:58\n15.92,环境设计2006,2023-09-05 08:18:47,13.85,艺术与设计学院,女,3145829,东方清野,毕业设计展作品《潮汐》获校级优秀奖，展览编号 ART-2024-0762,2023-2024-1,1,正常在读,S2020-5521-b8c,82.07,2024-03-22 12:03:39\n2.71,通信工程2204,2024-02-14 17:55:21,23.43,信息与通信工程学院,男,9952367,南宫问天,学分账户冻结，涉及跨校转学审核编号 TRA-2024-5530，待对方院校回函,2024-2025-2,0,冻结,S2022-9963-c4d,49.28,2024-11-08 06:29:14\n24.87,生物技术2305,2024-09-16 03:29:58,13.73,生命科学学院,女,4417830,轩辕若冰,参与基因编辑课题实验，实验记录编号 LAB-2025-1693，导师签字确认,2024-2025-1,1,正常在读,S2023-4472-d9e,91.14,2025-04-01 18:12:45\n22.51,数学与应用数学1903,2022-09-08 20:44:33,11.15,数学与统计学院,男,5829074,令狐冲虚,保研至本校研究生，推荐信编号 REC-2023-2284，最终成绩位列前三,2022-2023-1,4,毕业,S2019-7718-e5f,89.36,2023-06-28 15:07:52\n7.44,车辆工程2102,2024-08-27 09:13:06,20.61,汽车与交通工程学院,男,6073821,司徒远航,参与大学生方程式赛车设计，团队排名第 11，赛事编号 FSAE-2025-0088,2024-2025-1,1,正常在读,S2021-3309-f3a,66.83,2025-02-19 23:50:28\n17.66,会计学2001,2024-02-05 12:48:19,10.37,管理学与商学院,女,1594726,夏侯安宁,通过注册会计师两门考试，成绩单编号 CPA-2024-3390，附件已上传,2023-2024-2,1,正常在读,S2020-1187-a1b,87.29,2024-08-14 05:36:07\n5.28,旅游管理2203,2023-08-22 16:02:55,18.34,地理与旅游学院,女,7306418,万俟清欢,按结业处理，学籍编号 REG-2024-7708，可申请补考换取毕业证,2023-2024-1,3,结业,S2022-6634-b6c,52.94,2024-06-11 11:27:43\n14.09,数字媒体艺术2306,2024-02-21 07:27:31,16.58,艺术与设计学院,男,2840517,钟离陌离,短视频作品获校赛金奖，作品编号 VID-2025-2247，平台播放量破十万,2024-2025-2,1,正常在读,S2023-8890-c2d,79.51,2025-05-03 13:44:09\n9.73,土木工程1904,2023-01-11 04:19:26,19.57,土木与建筑工程学院,男,3938265,端木青云,参与桥梁结构检测实习，实习编号 INT-2023-6612，单位反馈良好,2022-2023-2,1,正常在读,S2019-4455-d8e,73.16,2023-09-27 19:38:51\n1.83,药学2107,2024-08-31 23:05:14,26.91,药学院,女,8471039,赫连明轩,因实验安全事故调查冻结，案件编号 SAF-2025-0041，听证会待安排,2024-2025-1,0,冻结,S2021-2213-e9f,44.07,2025-03-08 02:16:37\n21.34,法学2008,2023-09-12 18:41:02,9.66,法学院,女,5207394,澹台明月,模拟法庭大赛最佳辩手，赛事编号 MOOT-2024-1130，奖状已归档,2023-2024-1,1,正常在读,S2020-9931-f0a,90.68,2024-04-30 10:55:26\n11.06,材料成型及控制工程2201,2024-02-09 10:52:44,18.73,材料科学与工程学院,男,6814952,公孙止戈,参与新型合金疲劳测试项目，数据编号 MAT-2024-9982，论文在投,2024-2025-2,1,正常在读,S2022-5587-a4b,71.33,2024-12-17 15:31:08\n16.47,英语2304,2023-08-14 21:28:39,11.53,外国语学院,男,9652180,百里守约,通过 CATTI 三级笔译，证书编号 CATTI-2024-2871，成绩单随附,2023-2024-1,1,正常在读,S2023-1124-b5c,85.94,2024-05-19 07:03:57\n13.82,食品科学与工程1909,2022-09-19 15:37:11,12.35,食品科学学院,女,3071645,太史慈恩,已入职食品检测机构，就业协议编号 EMP-2023-4471，档案已转出,2022-2023-1,4,毕业,S2019-6678-c6d,81.47,2023-06-25 22:49:33\n13.19,机器人工程2105,2024-08-23 06:11:28,15.57,自动化与机器人学院,男,7482936,呼延觉罗,参与机器人格斗赛获省级三等奖，队伍编号 BOT-2025-0726,2024-2025-1,1,正常在读,S2021-8899-d7e,77.62,2025-01-30 14:22:41\n6.91,测绘工程2007,2024-02-18 03:44:52,17.06,测绘与地理信息学院,男,1935704,宇文长风,完成无人机航测外业，作业编号 SUR-2024-5518，含坐标系转换报告,2023-2024-2,1,正常在读,S2020-3345-e8f,69.85,2024-09-06 16:10:19\n7.36,公共事业管理2202,2023-08-26 12:23:47,18.63,公共管理学院,女,6218473,尉迟恭行,参军入伍申请休学，入伍编号 MIL-2024-0035，退役后复学,2023-2024-1,2,休学,S2022-1102-f9a,58.73,2024-02-01 09:41:33\n15.64,物流管理2308,2024-02-25 20:17:04,12.36,管理学与商学院,男,4569012,欧阳听风,参与供应链优化课题，数据模型编号 SCM-2025-3392，企业合作方盖章,2024-2025-2,1,正常在读,S2023-7754-a0b,83.28,2025-06-05 04:58:22\n18.29,化学工程与工艺1906,2023-01-16 09:07:38,12.18,化学与分子工程学院,女,1730486,东方既白,参与催化剂合成实验，危险品操作证编号 HAZ-2023-1147，已备案,2022-2023-2,1,正常在读,S2019-2289-b1c,86.11,2023-08-12 21:34:50\n12.07,水利水电工程2101,2024-08-11 17:41:26,16.58,水利与土木工程学院,女,5306714,慕容听雪,参与水工模型试验，试验编号 HYD-2025-2208，数据处理脚本已提交,2024-2025-1,1,正常在读,S2021-5560-c3d,75.49,2025-02-22 11:15:03\n14.55,工程管理2009,2023-09-01 05:56:14,14.53,土木与建筑工程学院,男,9182460,长孙无咎,考取一级建造师法规科目，成绩编号 CON-2024-8830，备考记录完整,2023-2024-1,1,正常在读,S2020-4478-d4e,80.02,2024-03-15 18:29:47\n3.06,物联网工程2206,2024-02-16 14:52:31,25.58,计算机与人工智能学院,男,2647193,南宫惊鸿,因严重违纪调查冻结，纪检编号 DIS-2024-0912，处分决定待公示,2024-2025-2,0,冻结,S2022-3341-e5f,47.36,2024-10-25 07:44:16\n20.43,新闻学2302,2023-08-18 11:31:09,10.62,文学与传媒学院,女,8053927,皇甫静姝,校报记者站站长，采编稿件编号 PRS-2024-6673，深度报道获省级奖,2023-2024-1,1,正常在读,S2023-6690-f6a,89.81,2024-06-20 13:27:55\n11.38,农业资源与环境1902,2022-09-04 19:22:47,12.74,资源与环境学院,女,3561820,令狐未央,考取选调生，录用编号 GOV-2023-7756，报到手续已办理完成,2022-2023-1,4,毕业,S2019-9934-a7b,78.64,2023-06-29 05:18:36\n10.28,核工程与核技术2103,2024-08-25 08:49:15,17.33,核科学与技术学院,男,4702381,轩辕孤鸿,参与辐射防护培训，培训编号 RAD-2025-4409，实操考核合格,2024-2025-1,1,正常在读,S2021-2267-b8c,72.95,2025-03-12 22:06:40\n16.02,工业设计2005,2024-02-12 23:37:58,11.86,艺术与设计学院,女,6924158,司徒望舒,设计作品入围红点概念奖，投稿编号 RDT-2024-5581，模型图纸已存档,2023-2024-2,1,正常在读,S2020-7701-c9d,84.16,2024-08-29 09:51:24\n9.15,能源与动力工程2207,2024-02-24 15:08:43,18.67,机械与能源动力工程学院,男,1378654,万俟孤云,参与锅炉效率测试项目，测试编号 THM-2025-6624，能耗报告已提交,2024-2025-2,1,正常在读,S2022-8830-d0e,70.48,2025-05-27 03:39:12\n25.71,哲学2301,2023-08-20 04:26:31,13.18,哲学与社会发展学院,女,5863709,钟离若梦,在核心期刊发表论文一篇，期刊编号 JRN-2024-9925，含外审意见回复,2023-2024-1,1,正常在读,S2023-1146-e1f,92.53,2024-07-15 17:43:08\n4.53,智能制造工程2104,2024-08-17 21:14:26,23.72,自动化与机器人学院,男,2097316,端木长歌,学籍信息核验异常冻结，核验编号 VER-2025-1173，需补充身份材料,2024-2025-1,0,冻结,S2021-4408-f2a,50.81,2025-01-08 12:37:49\n"}}"""


def norm(v):
    """把数值统一成可比字符串：68.42 与 '68.42' 视为相等，85.0 与 '85' 视为相等。"""
    if isinstance(v, bool):
        return str(v)
    if isinstance(v, (int, float)):
        f = float(v)
        return str(int(f)) if f == int(f) else repr(f)
    return str(v)


def main():
    orig = json.loads(ORIGINAL)
    comp = json.loads(COMPRESSED)

    orecs = orig["data"]["records"]
    print("=" * 60)
    print("【原文】声明的 total      :", orig["data"].get("total"))
    print("【原文】声明的 pageSize   :", orig["data"].get("pageSize"))
    print("【原文】records 实际条数  :", len(orecs))

    rec_str = comp["data"]["records"]
    lines = [ln for ln in rec_str.split("\n") if ln.strip() != ""]
    m = re.match(r"^\[(\d+)\]\{(.*)\}$", lines[0])
    declared = int(m.group(1))
    fields = [p.split(":")[0] for p in m.group(2).split(",")]
    rows = lines[1:]

    print("-" * 60)
    print("【压缩】头部声明条数      :", declared)
    print("【压缩】实际数据行数      :", len(rows))
    print("【压缩】Schema 字段数     :", len(fields))

    # 字段集合是否一致
    orig_fields = set(orecs[0].keys())
    print("【校验】字段集合一致      :", orig_fields == set(fields))
    if orig_fields != set(fields):
        print("   原文独有:", orig_fields - set(fields))
        print("   压缩独有:", set(fields) - orig_fields)

    # 逐字段比对
    mismatches = []
    cdicts = []
    for i, r in enumerate(rows):
        vals = r.split(",")
        if len(vals) != len(fields):
            mismatches.append("行%d 字段数不符: %d != %d" % (i, len(vals), len(fields)))
            continue
        cdicts.append(dict(zip(fields, vals)))

    for i, (o, c) in enumerate(zip(orecs, cdicts)):
        for k, ov in o.items():
            cv = c.get(k)
            if cv is None:
                mismatches.append("行%d 字段 %s 缺失" % (i, k))
            elif norm(ov) != norm(cv):
                mismatches.append("行%d 字段 %s: 原文=%r 压缩=%r" % (i, k, ov, cv))

    print("-" * 60)
    print(
        "【结果】原文条数 == 压缩行数 :",
        len(orecs) == len(cdicts),
        "(%d vs %d)" % (len(orecs), len(cdicts)),
    )
    print("【结果】逐字段不一致项数     :", len(mismatches))
    for d in mismatches[:30]:
        print("   -", d)
    print("=" * 60)
    ok = len(orecs) == len(cdicts) == declared and not mismatches
    print("最终结论:", "无损（逐字段 100% 严格一致）" if ok else "存在差异/丢失")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
