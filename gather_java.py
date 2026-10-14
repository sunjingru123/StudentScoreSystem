import os

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "javacode.txt")

EXCLUDE_DIRS = {"target", "build", ".git", ".idea", "node_modules"}

java_files = []
for dirpath, dirnames, filenames in os.walk(ROOT):
    dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
    for fn in filenames:
        if fn.endswith(".java"):
            java_files.append(os.path.join(dirpath, fn))

java_files.sort()

sep = "=" * 100
with open(OUT, "w", encoding="utf-8") as out:
    out.write("后端所有 .java 源码汇总\n")
    out.write("文件数量: %d\n" % len(java_files))
    out.write(sep + "\n\n")
    for path in java_files:
        try:
            with open(path, "r", encoding="utf-8") as f:
                code = f.read()
        except UnicodeDecodeError:
            with open(path, "r", encoding="gbk", errors="replace") as f:
                code = f.read()
        out.write(sep + "\n")
        out.write("// FILE: " + path + "\n")
        out.write(sep + "\n")
        out.write(code)
        if not code.endswith("\n"):
            out.write("\n")
        out.write("\n\n")

size = os.path.getsize(OUT)
print("已写入:", OUT)
print("文件数量:", len(java_files))
print("输出大小:", size, "bytes =", round(size / 1024, 1), "KB")
