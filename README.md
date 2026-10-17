# Student Score System

综合测评管理系统

技术栈：

- Vue3
- Spring Boot
- PostgreSQL
- MyBatis-Plus

开发者：

- 茹茹宝贝

---

## 开发与验证

### 后端（`student-score-system/`）

统一验证入口（本地与 CI 共用同一个脚本）：

```sh
# 完整验证：编译 + 运行测试
sh scripts/verify-backend.sh

# 快速验证：编译主源码与测试源码，不运行测试（提交前使用）
sh scripts/verify-backend.sh --fast
```

完整验证会运行 `@SpringBootTest` 上下文测试，需要可用的 PostgreSQL 与 `JWT_SECRET`（至少 32 字节），
通过环境变量提供：

```sh
DB_URL="jdbc:postgresql://localhost:5432/student_score_system" \
DB_USERNAME=postgres \
DB_PASSWORD=你的数据库密码 \
JWT_SECRET=至少32字节的随机字符串 \
sh scripts/verify-backend.sh
```

> 不使用 `-DskipTests` 作为唯一验证方式。

### 前端（`frontend/`）

```sh
cd frontend
npm ci
npm run build
```

### 提交前检查（pre-commit）

安装一次（仓库根目录）：

```sh
git config core.hooksPath .githooks

# Linux / macOS 需要赋予执行权限；Windows（Git for Windows）会自动处理
chmod +x .githooks/pre-commit
```

之后，若本次提交涉及后端文件（`student-score-system/` 下任意文件或任意 `.java`），
会自动运行 `sh scripts/verify-backend.sh --fast`（编译主源码与测试源码，不运行测试），编译失败则拒绝提交。

- 临时跳过：`SKIP_BACKEND_CHECK=1 git commit ...`
- 本地钩子只是便利手段，**不是安全边界**，可通过 `--no-verify` 绕过；真正的强制门禁在 CI。

### CI 门禁（`.github/workflows/deploy.yml`）

- `backend-test`：拉起临时 PostgreSQL，编译并运行后端测试；
- `frontend-build`：`npm ci && npm run build`；
- `build-and-push`：依赖上面两个任务，任一失败都不会推送镜像；
- `deploy`：依赖构建任务，使用提交 SHA 作为不可变镜像标签。

任何编译或测试失败都会阻止镜像发布与部署，工作流中不使用 `continue-on-error` 掩盖失败。
