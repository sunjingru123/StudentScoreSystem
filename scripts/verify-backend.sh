#!/usr/bin/env sh
#
# =========================================================
# 后端统一验证入口（本地开发 / CI 共用）
#
# 用法：
#   sh scripts/verify-backend.sh           # 完整验证：编译 + 运行测试
#   sh scripts/verify-backend.sh --fast    # 快速验证：编译主源码与测试源码，不运行测试
#   sh scripts/verify-backend.sh --help
#
# 行为：
#   - 使用 POSIX sh 语法，`set -eu`；
#   - 自动定位仓库根目录并进入后端 Maven 项目目录 student-score-system/；
#   - 使用 Maven 执行真实的编译与测试；
#   - 编译或测试失败时以非零退出码结束（CI 据此判定失败）。
#
# 环境变量（完整模式需要）：
#   项目测试包含 @SpringBootTest 上下文加载，需要可用的 PostgreSQL
#   以及 JWT_SECRET（至少 32 字节）：
#     DB_URL / DB_USERNAME / DB_PASSWORD / JWT_SECRET
# =========================================================

set -eu

usage() {
    echo "用法: sh scripts/verify-backend.sh [--fast|--help]"
    echo ""
    echo "  (无参数)   完整验证：mvn clean test（编译 + 运行测试）"
    echo "  --fast     快速验证：mvn test-compile（编译主源码与测试源码，不运行测试）"
    echo "  --help     显示本帮助"
}

MODE="full"

for arg in "$@"; do
    case "$arg" in
        --fast)
            MODE="fast"
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            echo "未知参数: $arg" >&2
            usage >&2
            exit 2
            ;;
    esac
done

# 定位仓库根目录（本脚本位于 <repo>/scripts/ 下）
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
BACKEND_DIR="$REPO_ROOT/student-score-system"

if [ ! -d "$BACKEND_DIR" ]; then
    echo "[verify-backend] 错误：找不到后端项目目录: $BACKEND_DIR" >&2
    exit 1
fi

if [ ! -f "$BACKEND_DIR/pom.xml" ]; then
    echo "[verify-backend] 错误：后端目录缺少 pom.xml: $BACKEND_DIR/pom.xml" >&2
    exit 1
fi

# 选择 Maven 启动方式：
#   1. 优先使用项目自带的 Maven Wrapper（保证与 CI/本地版本一致）；
#      Git 不保证保留可执行位，因此统一用 `sh ./mvnw` 调用，不依赖 chmod +x；
#   2. Windows（Git Bash / MSYS）使用 mvnw.cmd；
#   3. 两者都不存在时回退到系统 mvn。
MVN_KIND="mvn"

case "$(uname -s 2>/dev/null || echo unknown)" in
    MINGW*|MSYS*|CYGWIN*)
        if [ -f "$BACKEND_DIR/mvnw.cmd" ]; then
            MVN_KIND="mvnw.cmd"
        fi
        ;;
    *)
        if [ -f "$BACKEND_DIR/mvnw" ]; then
            MVN_KIND="mvnw"
        fi
        ;;
esac

cd "$BACKEND_DIR"

# 用 exec 调用 Maven：进程被替换，脚本退出码即 Maven 退出码，
# 因此编译/测试失败时脚本一定返回非零。
run_maven() {
    case "$MVN_KIND" in
        mvnw)
            exec sh ./mvnw "$@"
            ;;
        mvnw.cmd)
            exec ./mvnw.cmd "$@"
            ;;
        *)
            exec mvn "$@"
            ;;
    esac
}

if [ "$MODE" = "fast" ]; then
    echo "[verify-backend] 快速模式：编译主源码与测试源码（不运行测试）"
    run_maven -B test-compile
fi

if [ -z "${JWT_SECRET:-}" ]; then
    echo "[verify-backend] 警告：未设置 JWT_SECRET（至少 32 字节），" >&2
    echo "                 Spring 上下文测试可能失败；请参考 README 的验证说明。" >&2
fi

echo "[verify-backend] 完整模式：编译 + 运行测试"
run_maven -B clean test
