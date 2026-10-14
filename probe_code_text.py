import json
import os
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8787"
ROOT = os.path.dirname(os.path.abspath(__file__))


def get(path):
    req = urllib.request.Request(BASE + path)
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8", "replace")


def post(path, body):
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        BASE + path,
        data=data,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return r.status, json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:400]


print("### transformations feed (limit=5, no messages) ###")
try:
    feed = json.loads(get("/transformations/feed?limit=5&include_messages=0"))
    if isinstance(feed, dict):
        items = (
            feed.get("items") or feed.get("transformations") or feed.get("feed") or feed
        )
    else:
        items = feed
    print("keys:", list(feed.keys()) if isinstance(feed, dict) else "list")
    print(json.dumps(feed, ensure_ascii=False)[:2500])
except Exception as e:
    print("err", repr(e))

# gather java code
best, sz = None, 0
for dp, dn, fn in os.walk(ROOT):
    dn[:] = [
        d for d in dn if d not in {"target", "build", ".git", ".idea", "node_modules"}
    ]
    for f in fn:
        if f.endswith(".java"):
            p = os.path.join(dp, f)
            s = os.path.getsize(p)
            if s > sz:
                best, sz = p, s
code = open(best, "r", encoding="utf-8", errors="replace").read()[:40000]
audit = ""
ap = os.path.join(ROOT, "audit.md")
if os.path.exists(ap):
    audit = open(ap, "r", encoding="utf-8", errors="replace").read()[:40000]

zh_prose = (
    "这是一个关于学生成绩管理系统的说明文档。系统采用前后端分离架构，后端使用 Spring Boot 框架，"
    * 30
)

cases = [
    ("代码-裸文件", code),
    ("代码-```围栏包裹", "这里是代码：\n```java\n" + code + "\n```\n请解释。"),
    ("audit.md-散文", audit),
    ("中文散文", zh_prose),
    (
        "英文散文",
        "The quick brown fox jumps over the lazy dog and then explains the architecture. "
        * 60,
    ),
]

print()
print("%-24s %8s %8s %8s  %s" % ("样本", "前", "后", "省", "策略"))
print("-" * 80)
for label, text in cases:
    if not text:
        print("%-24s (空)" % label)
        continue
    st, res = post(
        "/v1/compress",
        {
            "model": "claude-3-5-sonnet-20241022",
            "messages": [{"role": "user", "content": text}],
        },
    )
    if isinstance(res, dict) and "tokens_before" in res:
        print(
            "%-24s %8s %8s %8s  %s"
            % (
                label,
                res.get("tokens_before"),
                res.get("tokens_after"),
                res.get("tokens_saved"),
                json.dumps(res.get("transforms_applied"), ensure_ascii=False),
            )
        )
    else:
        print("%-24s -> %s %s" % (label, st, str(res)[:200]))
