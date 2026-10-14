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
    try:
        with urllib.request.urlopen(req, timeout=180) as r:
            return json.loads(r.read().decode("utf-8", "replace"))
    except urllib.error.HTTPError as e:
        return {
            "error": "HTTP %s %s" % (e.code, e.read().decode("utf-8", "replace")[:200])
        }


# collect Chinese comment text from java sources
comments = []
seen = set()
for dp, dn, fn in os.walk(ROOT):
    dn[:] = [d for d in dn if d not in EXCLUDE]
    for f in fn:
        if not f.endswith(".java"):
            continue
        p = os.path.join(dp, f)
        txt = open(p, "r", encoding="utf-8", errors="replace").read()
        for m in re.findall(r"//\s*(.+)", txt):
            s = m.strip()
            if len(s) >= 6 and re.search(r"[\u4e00-\u9fff]", s) and s not in seen:
                seen.add(s)
                comments.append(s)
        for m in re.findall(r"\*\s+([^\n*/]+)", txt):
            s = m.strip()
            if len(s) >= 6 and re.search(r"[\u4e00-\u9fff]", s) and s not in seen:
                seen.add(s)
                comments.append(s)

print("收集到中文注释行数:", len(comments))

# build samples of increasing size
sizes = [200, 600, 1500]
for target_chars in [1500, 5000, 12000]:
    buf, total = [], 0
    for c in comments:
        buf.append(c)
        total += len(c)
        if total >= target_chars:
            break
    text = "。".join(buf)
    res = post(text)
    if "error" in res:
        print("~%d字符 -> ERROR %s" % (target_chars, res["error"]))
        continue
    print(
        "~%d字符(%d行) -> before=%s after=%s saved=%s strat=%s"
        % (
            target_chars,
            len(buf),
            res.get("tokens_before"),
            res.get("tokens_after"),
            res.get("tokens_saved"),
            json.dumps(res.get("transforms_applied"), ensure_ascii=False),
        )
    )

# also: natural English prose built from varied sentences (docstring-like)
en = (
    "The system records every score modification in an audit table so that administrators can trace who changed what and when. "
    "Authentication relies on short lived JSON Web Tokens, which are validated on each request by a filter that runs before the controllers. "
    "Because the scoring rules differ between departments, each department owns a template that maps activity categories onto score types. "
    "Importing student rosters from spreadsheets is a common operation, so the importer validates every row before committing the batch. "
    "When a teacher submits a score, the workflow engine routes it to the appropriate approver and notifies the applicant on completion. "
)
en_long = ("".join(en.split(". ")[:-1] + [x]) for x in [])
# just repeat with variation markers
en_text = "\n\n".join(
    [
        "Paragraph %d. " % i + en.replace("The system", "The subsystem%d" % i)
        for i in range(1, 12)
    ]
)
res = post(en_text)
print(
    "英文自然段(11段变体) -> before=%s after=%s saved=%s strat=%s"
    % (
        res.get("tokens_before"),
        res.get("tokens_after"),
        res.get("tokens_saved"),
        json.dumps(res.get("transforms_applied"), ensure_ascii=False),
    )
)
