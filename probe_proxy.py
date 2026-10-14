import json
import urllib.request


def get(path):
    req = urllib.request.Request("http://127.0.0.1:8787" + path)
    with urllib.request.urlopen(req, timeout=15) as r:
        return r.read().decode("utf-8", "replace")


try:
    doc = json.loads(get("/openapi.json"))
    paths = doc.get("paths", {})
    print("OpenAPI paths (%d):" % len(paths))
    for p, methods in sorted(paths.items()):
        ms = ",".join(sorted(m.upper() for m in methods.keys()))
        print("  %-45s %s" % (p, ms))
except Exception as e:
    print("openapi error:", repr(e))
