import json
import os
import re
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.abspath(__file__))
URL = "http://127.0.0.1:8787/v1/compress"


def post(text):
    body = {
        "model": "claude-3-5-sonnet-20241022",
        "messages": [{"role": "user", "content": text}],
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


ap = os.path.join(ROOT, "audit.md")
audit = open(ap, "r", encoding="utf-8", errors="replace").read()

# 去 Markdown 结构，只保留自然段落的文字
plain_lines = []
for ln in audit.splitlines():
    s = ln.strip()
    if not s:
        continue
    if s.startswith(("#", "|", "-", "*", ">", "```", "!")) or re.match(r"^\d+\.", s):
        continue
    if s.startswith("---"):
        continue
    plain_lines.append(s)
plain = "\n".join(plain_lines)
print("audit 原始字符数:", len(audit), " 去结构后字符数:", len(plain))

cases = [
    ("audit[:4000字符] (含md结构)", audit[:4000]),
    ("audit[:12000字符] (含md结构)", audit[:12000]),
    ("去结构散文[:4000]", plain[:4000]),
    ("去结构散文[:16000]", plain[:16000]),
]

print()
print("%-34s %8s %8s %8s  %s" % ("样本", "前", "后", "省", "策略"))
print("-" * 88)
for label, text in cases:
    res = post(text)
    if "error" in res:
        print("%-34s ERROR %s" % (label, res["error"]))
        continue
    print(
        "%-34s %8s %8s %8s  %s"
        % (
            label,
            res.get("tokens_before"),
            res.get("tokens_after"),
            res.get("tokens_saved"),
            json.dumps(res.get("transforms_applied"), ensure_ascii=False),
        )
    )
