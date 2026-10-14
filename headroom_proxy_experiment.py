import json
import os
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.abspath(__file__))
URL = "http://127.0.0.1:8787/v1/compress"
OUT_JSON = os.path.join(ROOT, "headroom_proxy_experiment.json")

EXCLUDE = {"target", "build", ".git", ".idea", "node_modules"}


def call_proxy(text, label, role="user"):
    body = {
        "model": "claude-3-5-sonnet-20241022",
        "messages": [{"role": role, "content": text}],
    }
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        URL, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=300) as r:
            return json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        return {
            "error": "HTTP %s: %s" % (e.code, e.read().decode("utf-8", "replace")[:300])
        }
    except Exception as e:
        return {"error": repr(e)}


# ---------- sample builders ----------
def gen_json_repeat(n=200):
    recs = []
    for i in range(n):
        recs.append(
            {
                "studentId": "S%05d" % i,
                "name": "Student%d" % i,
                "courseId": "C%03d" % (i % 12),
                "courseName": "CourseName",  # repeated value
                "category": "MORAL",  # repeated value
                "score": round(60 + (i % 40) * 0.75, 2),
                "credit": 2.0,
                "semester": "2024-2025-1",
                "scoreType": "department_template",
                "status": "APPROVED",
                "remark": None,
                "createdAt": "2025-01-15T08:30:00",
                "updatedAt": "2025-01-15T08:30:00",
                "operator": "admin",
                "deleted": 0,
            }
        )
    return json.dumps(
        {"code": 0, "msg": "ok", "total": n, "pageSize": n, "records": recs},
        ensure_ascii=False,
        indent=2,
    )


def gen_json_varied(n=60):
    recs = []
    for i in range(n):
        recs.append(
            {
                "id": i + 1,
                "studentId": "S%05d" % (i * 7 % 9973),
                "studentName": "姓名_%d" % i,
                "department": ["信息学院", "机械学院", "外国语学院", "艺术学院"][i % 4],
                "activityTitle": "第%d届校园活动_%d" % (i, i * 3),
                "score": round(50 + (i * 17 % 50) * 0.97, 2),
                "unit": "分",
                "approver": "teacher_%d" % (i % 23),
                "approveTime": "2025-03-%02dT11:%02d:00" % ((i % 28) + 1, i % 60),
                "reason": "该生在第%d项活动中表现%s，故给予加分"
                % (i, ["优秀", "良好", "突出", "积极"][i % 4]),
            }
        )
    return json.dumps(
        {"code": 0, "data": {"list": recs, "pageNo": 1, "pageSize": n}},
        ensure_ascii=False,
        indent=2,
    )


def gen_text_prose():
    p = os.path.join(ROOT, "audit.md")
    if os.path.exists(p):
        with open(p, "r", encoding="utf-8", errors="replace") as f:
            return f.read()[:40000]
    return "The quick brown fox jumps over the lazy dog. " * 800


def gen_code():
    best, best_size = None, 0
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE]
        for fn in filenames:
            if fn.endswith(".java"):
                fp = os.path.join(dirpath, fn)
                sz = os.path.getsize(fp)
                if sz > best_size:
                    best, best_size = fp, sz
    if best:
        with open(best, "r", encoding="utf-8", errors="replace") as f:
            return f.read()[:40000], best
    return "", None


def gen_log():
    frames = [
        "\tat org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:600)",
        "\tat org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:542)",
        "\tat org.springframework.beans.factory.support.AbstractBeanFactory$1.getObject(AbstractBeanFactory.java:326)",
        "\tat org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:234)",
        "\tat com.student.studentscoresystem.service.impl.ScoreServiceImpl.calculate(ScoreServiceImpl.java:188)",
        "\tat org.springframework.cglib.proxy.MethodProxy.invoke(MethodProxy.java:218)",
    ]
    lines = []
    for i in range(10):
        lines.append(
            "2025-03-15 10:%02d:%02d ERROR [score-service] c.s.ScoreServiceImpl - 计算学生成绩失败: 课程ID=%s"
            % (i % 60, i, i)
        )
        lines.append(
            'java.lang.NullPointerException: Cannot invoke "com.student...Course.getCredit()" because "course" is null'
        )
        for f in frames:
            lines.append(f)
        lines.append(
            "Caused by: java.lang.IllegalStateException: 缓存未初始化 (cache not initialized)"
        )
        for f in frames[:3]:
            lines.append(f)
        lines.append("")
    return "\n".join(lines)


# ---------- run ----------
code_text, code_path = gen_code()
samples = [
    ("JSON-高重复 (200条同构记录)", gen_json_repeat()),
    ("JSON-低重复 (60条异构记录)", gen_json_varied()),
    ("散文文本 (audit.md 前40KB)", gen_text_prose()),
    ("Java代码 (最大文件)", code_text),
    ("日志/堆栈 (10遍重复栈)", gen_log()),
]

results = []
print("样本大小: 代码来源 =", code_path)
print("=" * 96)
print(
    "%-32s %10s %10s %10s %9s  %s"
    % ("样本", "tokens前", "tokens后", "节省tokens", "压缩率", "策略")
)
print("-" * 96)
for label, text in samples:
    if not text:
        print("%-32s  (样本为空)" % label)
        continue
    res = call_proxy(text, label)
    if "error" in res:
        print("%-32s  ERROR: %s" % (label, res["error"]))
        results.append({"label": label, "error": res["error"], "chars": len(text)})
        continue
    tb = res.get("tokens_before")
    ta = res.get("tokens_after")
    ts = res.get("tokens_saved")
    ratio = res.get("compression_ratio")
    strat = res.get("transforms_summary") or res.get("transforms_applied")
    pct = (100.0 * (1 - ratio)) if isinstance(ratio, (int, float)) else None
    print(
        "%-32s %10s %10s %10s %9s  %s"
        % (
            label,
            tb,
            ta,
            ts,
            ("%.1f%%" % pct) if pct is not None else "-",
            json.dumps(strat, ensure_ascii=False),
        )
    )
    results.append(
        {
            "label": label,
            "chars": len(text),
            "tokens_before": tb,
            "tokens_after": ta,
            "tokens_saved": ts,
            "compression_ratio": ratio,
            "transforms_applied": res.get("transforms_applied"),
            "transforms_summary": res.get("transforms_summary"),
            "ccr_hashes": res.get("ccr_hashes"),
        }
    )
print("=" * 96)

with open(OUT_JSON, "w", encoding="utf-8") as f:
    json.dump(results, f, ensure_ascii=False, indent=2)
print("详细结果已保存:", OUT_JSON)
