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
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


body = {
    "model": "claude-3-5-sonnet-20241022",
    "messages": [{"role": "user", "content": "hello world " * 30}],
}
st, resp = post(body)
print("STATUS:", st)
print(resp[:3000])
