import json
import os
import re
import urllib.error
import urllib.request

ROOT = os.path.dirname(os.path.abspath(__file__))
URL = "http://127.0.0.1:8787/v1/compress"
EXCLUDE = {"target", "build", ".git", ".idea", "node_modules"}


def post(text):
    body = {
        "model": "claude-3-5-sonnet-20241022",
        "messages": [{"role": "user", "content": text}],
    }
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        URL, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    with urllib.request.urlopen(req, timeout=180) as r:
        return json.loads(r.read().decode("utf-8", "replace"))


comments, seen = [], set()
for dp, dn, fn in os.walk(ROOT):
    dn[:] = [d for d in dn if d not in EXCLUDE]
    for f in fn:
        if not f.endswith(".java"):
            continue
        txt = open(os.path.join(dp, f), "r", encoding="utf-8", errors="replace").read()
        for m in re.findall(r"//\s*(.+)", txt) + re.findall(r"\*\s+([^\n*/]+)", txt):
            s = m.strip()
            if len(s) >= 6 and re.search(r"[\u4e00-\u9fff]", s) and s not in seen:
                seen.add(s)
                comments.append(s)

buf, total = [], 0
for c in comments:
    buf.append(c)
    total += len(c)
    if total >= 5000:
        break
text = "。".join(buf)
sample_path = os.path.join(ROOT, "sample_natural_text.txt")
with open(sample_path, "w", encoding="utf-8") as f:
    f.write(text)

res = post(text)
print("样本已保存:", sample_path, "字符数:", len(text))
print(
    "代理结果:",
    json.dumps(
        {
            k: res.get(k)
            for k in [
                "tokens_before",
                "tokens_after",
                "tokens_saved",
                "compression_ratio",
                "transforms_applied",
                "ccr_hashes",
            ]
        },
        ensure_ascii=False,
    ),
)
