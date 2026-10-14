import json
import urllib.error
import urllib.request

BASE = "http://127.0.0.1:8787"


def get(path):
    req = urllib.request.Request(BASE + path)
    with urllib.request.urlopen(req, timeout=20) as r:
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
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


print("### /settings/schema ###")
try:
    sch = json.loads(get("/settings/schema"))
    print(json.dumps(sch, ensure_ascii=False, indent=2)[:3000])
except Exception as e:
    print("err", repr(e))

print()
print("### /settings (current) ###")
try:
    cur = json.loads(get("/settings"))
    print(json.dumps(cur, ensure_ascii=False, indent=2)[:2000])
except Exception as e:
    print("err", repr(e))

print()
print("### /admin/upstream ###")
try:
    print(get("/admin/upstream")[:800])
except Exception as e:
    print("err", repr(e))

# Test per-request overrides on prose
prose = ("Headroom is a context optimization layer. " * 5) + (
    "This paragraph explains the design goals and tradeoffs of the system in plain natural language without any repetition of data structures. "
    * 40
)

print()
print("### override probes (prose) ###")
for extra in [
    {},
    {"target_ratio": 0.4},
    {"force_kompress": True},
    {"mode": "token"},
    {"compress_system_messages": True},
]:
    body = {
        "model": "claude-3-5-sonnet-20241022",
        "messages": [{"role": "user", "content": prose}],
    }
    body.update(extra)
    st, resp = post("/v1/compress", body)
    try:
        j = json.loads(resp)
        print(
            "extra=%-28s -> %s  before=%s after=%s strat=%s"
            % (
                json.dumps(extra),
                st,
                j.get("tokens_before"),
                j.get("tokens_after"),
                json.dumps(j.get("transforms_applied"), ensure_ascii=False),
            )
        )
    except Exception:
        print("extra=%-28s -> %s  %s" % (json.dumps(extra), st, resp[:200]))
