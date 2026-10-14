import json
import urllib.error
import urllib.request

URL = "http://127.0.0.1:8787/v1/compress"


def post(body):
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        URL, data=data, headers={"Content-Type": "application/json"}, method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, r.read().decode("utf-8", "replace")[:1500]
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")[:1500]
    except Exception as e:
        return "ERR", repr(e)


candidates = [
    {},
    {"text": "hello world this is a test " * 20},
    {"content": "hello"},
    {"messages": [{"role": "user", "content": "hello world " * 20}]},
]

for b in candidates:
    st, resp = post(b)
    print("BODY KEYS:", list(b.keys()), "->", st)
    print("  ", resp.replace("\n", " ")[:600])
    print("-" * 70)
