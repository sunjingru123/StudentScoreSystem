import json
import urllib.error
import urllib.request

URL = "http://127.0.0.1:8787/v1/compress"

LOG = """2025-03-15 10:01:01 ERROR [score-service] c.s.ScoreServiceImpl - 计算学生成绩失败: 课程ID=1
java.lang.NullPointerException: Cannot invoke "com.student...Course.getCredit()" because "course" is null
\tat com.student.studentscoresystem.service.impl.ScoreServiceImpl.calculate(ScoreServiceImpl.java:188)
\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)
\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
Caused by: java.lang.IllegalStateException: 缓存未初始化 (cache not initialized)
\tat com.student.studentscoresystem.service.impl.CacheService.get(CacheService.java:44)

2025-03-15 10:02:02 ERROR [score-service] c.s.ScoreServiceImpl - 计算学生成绩失败: 课程ID=2
java.lang.NullPointerException: Cannot invoke "com.student...Course.getCredit()" because "course" is null
\tat com.student.studentscoresystem.service.impl.ScoreServiceImpl.calculate(ScoreServiceImpl.java:188)
\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)
\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
Caused by: java.lang.IllegalStateException: 缓存未初始化 (cache not initialized)
\tat com.student.studentscoresystem.service.impl.CacheService.get(CacheService.java:44)

2025-03-15 10:03:03 ERROR [score-service] c.s.ScoreServiceImpl - 计算学生成绩失败: 课程ID=3
java.lang.NullPointerException: Cannot invoke "com.student...Course.getCredit()" because "course" is null
\tat com.student.studentscoresystem.service.impl.ScoreServiceImpl.calculate(ScoreServiceImpl.java:188)
\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)
\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
Caused by: java.lang.IllegalStateException: 缓存未初始化 (cache not initialized)
\tat com.student.studentscoresystem.service.impl.CacheService.get(CacheService.java:44)

2025-03-15 10:04:04 ERROR [score-service] c.s.ScoreServiceImpl - 计算学生成绩失败: 课程ID=4
java.lang.NullPointerException: Cannot invoke "com.student...Course.getCredit()" because "course" is null
\tat com.student.studentscoresystem.service.impl.ScoreServiceImpl.calculate(ScoreServiceImpl.java:188)
\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)
\tat org.springframework.aop.framework.ReflectiveMethodInvocation.proceed(ReflectiveMethodInvocation.java:186)
Caused by: java.lang.IllegalStateException: 缓存未初始化 (cache not initialized)
\tat com.student.studentscoresystem.service.impl.CacheService.get(CacheService.java:44)"""

PROSE = "这是一个关于学生成绩管理系统的说明文档。系统采用前后端分离架构，后端使用 Spring Boot 框架，数据库采用 MySQL，缓存采用 Redis。前端使用 Vue3 与 Element Plus 组件库构建管理界面。系统支持学生成绩的录入、审核、统计与导出功能。教师可以为学生录入成绩，管理员可以审核成绩并生成统计报表，学生可以查询自己的成绩明细。系统还提供了活动加分、部门评分、个人证书申请等扩展功能。权限模型采用基于角色的访问控制，不同角色拥有不同的操作权限。所有关键操作都会记录操作日志以便审计追溯。系统采用 JWT 进行身份认证，令牌具有一定的有效期，过期后需要重新登录。为了提升性能，系统对常用查询结果进行了缓存处理，减少了数据库的访问压力。此外，系统还支持批量导入学生数据与成绩数据，提高了数据维护的效率。"


def post(text, role="user"):
    body = {
        "model": "claude-3-5-sonnet-20241022",
        "messages": [{"role": role, "content": text}],
    }
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        URL, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        return {
            "error": "HTTP %s %s" % (e.code, e.read().decode("utf-8", "replace")[:200])
        }


print("完全相同的输入，对比代理 /v1/compress 路径：")
print()
for name, text, mcp_line in [
    (
        "日志(4遍栈, 与MCP同一份)",
        LOG,
        "MCP结果: router:protected:error_output  615->615  0%",
    ),
    ("中文散文(与MCP同一份)", PROSE, "MCP结果: router:noop  223->223  0%"),
]:
    res = post(text)
    print("样本:", name)
    print("  基线(MCP headroom_compress):", mcp_line)
    if "error" in res:
        print("  代理:", res["error"])
    else:
        print(
            "  代理: %s  %s->%s  省 %s (%.1f%%)"
            % (
                json.dumps(res.get("transforms_applied"), ensure_ascii=False),
                res.get("tokens_before"),
                res.get("tokens_after"),
                res.get("tokens_saved"),
                100.0 * (1 - (res.get("compression_ratio") or 1)),
            )
        )
    print("-" * 80)
