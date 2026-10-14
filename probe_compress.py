import json
import urllib.request


def get(path):
    req = urllib.request.Request("http://127.0.0.1:8787" + path)
    with urllib.request.urlopen(req, timeout=15) as r:
        return r.read().decode("utf-8", "replace")


doc = json.loads(get("/openapi.json"))


def show(path):
    print("=" * 70)
    print("PATH:", path)
    p = doc["paths"].get(path)
    if not p:
        print("  (not found)")
        return
    for method, spec in p.items():
        print("  method:", method.upper())
        print("  summary:", spec.get("summary"))
        print("  description:", (spec.get("description") or "")[:500])
        rb = spec.get("requestBody")
        if rb:
            print("  requestBody:", json.dumps(rb, ensure_ascii=False)[:2000])
        resp = spec.get("responses", {})
        for code, r in resp.items():
            sch = r.get("content", {}).get("application/json", {}).get("schema")
            print(
                "  response %s schema:" % code,
                json.dumps(sch, ensure_ascii=False)[:1500],
            )


for path in [
    "/v1/compress",
    "/v1/compress/response",
    "/transformations/feed",
    "/settings",
]:
    show(path)

# also dump component schemas referenced by /v1/compress
print("=" * 70)
print("COMPONENT SCHEMAS (names):")
for name in sorted(doc.get("components", {}).get("schemas", {}).keys()):
    print("  ", name)
