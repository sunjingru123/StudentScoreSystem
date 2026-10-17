# AGENTS.md — 项目开发与验证规范

本文件对本仓库的所有自动化编码代理（Codex / Zed 等）以及人工贡献者持续生效。

制定背景：本仓库曾多次出现「引用了尚未提交/未实现的类、改完不做完整编译、CI 失败后才发现」的构建事故，例如：

- 部门成员管理功能缺少 `DepartmentMemberAddDTO`、`DepartmentMemberManageService`、`DepartmentMemberDetailVO`；
- `ScoreApplyController` 引用了未提交的 `BatchFinalAuditDTO`、`BatchPreliminaryAuditDTO`、`CertificateBatchAuditService`、`BatchAuditResultVO`。

下面的规则就是为了避免同类问题再次发生。

## 1. 项目结构（重要）

- 仓库根目录 **不是** 后端工程根目录。
- 后端：`student-score-system/`（Spring Boot 3.4.4 + Java 21 + MyBatis-Plus + PostgreSQL）。
- 前端：`frontend/`（Vue3 + Vite）。
- 数据库脚本：`sql/`（`sql/02_create_table.sql` 由部署流程幂等执行）。
- 统一验证入口：`scripts/verify-backend.sh`（本地与 CI 共用）。

> 注意：`dto/`、`vo/`、`services` 等目录的物理位置与 `package` 声明并不总是一一对应，
> 查找文件时以 **类名** 为准，不要只按包路径推断物理路径。

## 2. 修改前必须阅读

改动任何代码前，先阅读相关链路，而不是只看报错行：

- Controller；
- Service / ServiceImpl；
- Mapper；
- Entity；
- DTO / VO；
- 以及所有调用方。

## 3. 新增接口时必须同步完成

一个「接口」是一个整体，缺一不可：

1. 请求对象（DTO）；
2. 业务实现（Service 层，写操作加 `@Transactional(rollbackFor = Exception.class)`）；
3. 返回对象（VO / Result 结构）；
4. 权限校验（后端为准，不能只依赖前端路由）；
5. 异常处理（不吞异常，不静默失败）；
6. 必要测试。

**禁止只写 Controller 而遗漏 DTO / VO / Service。**
**禁止「先提交 Controller、DTO 类以后再补」这种会破坏 CI 的拆分提交。**

## 4. 修改或删除类时

- 必须全局搜索该类（含 `import`、构造器注入、静态调用）并同步更新所有调用方。
- 提交前确认：被引用的类确实存在于源码中，且已加入本次提交。

## 5. 禁止行为

- 不得用空类、空方法、返回固定值来掩盖编译错误。
- 不得为了让编译通过而删除接口、删除调用、注释业务代码。
- 不得只修当前报错行，必须检查本次改动涉及的完整调用链。
- 不得在源码中硬编码数据库密码、JWT 密钥等敏感信息（从环境变量/配置读取）。
- 不得修改 `JWT` 密钥机制、默认密码行为、现有角色名称与既有业务规则，除非任务明确要求。
- 不得删除数据库数据，不得执行 `DROP TABLE` / `TRUNCATE` / 全表 `DELETE`。

## 6. 修改后的验证（必须执行）

后端：

```sh
sh scripts/verify-backend.sh          # 编译 + 测试（完整）
sh scripts/verify-backend.sh --fast   # 编译主源码与测试源码，不运行测试（快速）
```

- 完整验证会运行 `@SpringBootTest` 上下文测试，需要可用的 PostgreSQL 与 `JWT_SECRET`（≥32 字节），
  通过环境变量提供：`DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET`。
- 不允许把 `-DskipTests` 当作唯一验证方式。
- 编译或测试失败时先定位根因，禁止提交未验证的代码。

前端（涉及 `frontend/` 改动时）：

```sh
cd frontend && npm ci && npm run build
```

**只有实际执行并看到通过结果，才允许声称「测试通过」。**

## 7. 敏感领域优先保护

涉及 **审核流程、成绩生成/删除、权限与角色、用户信息、数据库** 的改动：

- 优先保护原有业务规则与安全约束；
- 权限校验以后端为准；
- 保持原有状态流转与数据隔离，不擅自改变流程。

## 8. 提交前检查（pre-commit）

安装一次（仓库根目录）：

```sh
git config core.hooksPath .githooks

# Linux / macOS 需要赋予执行权限；Windows（Git for Windows）会自动处理
chmod +x .githooks/pre-commit
```

之后提交时，若涉及后端文件，会自动运行 `scripts/verify-backend.sh --fast`（编译主源码与测试源码，不运行测试），失败则拒绝提交。

- 临时跳过：`SKIP_BACKEND_CHECK=1 git commit ...`
- 本地钩子只是便利手段，**不是安全边界**，可以通过 `--no-verify` 绕过；真正的强制门禁在 CI。

## 9. CI 门禁

`.github/workflows/deploy.yml` 中：

- `backend-test`：编译并运行后端测试；
- `frontend-build`：前端构建；
- `build-and-push`：依赖上述两个任务，失败则不会推送镜像；
- `deploy`：依赖 `build-and-push`，且使用提交 SHA 作为不可变镜像标签。

任何编译或测试失败都会阻止镜像发布与部署，工作流中不允许使用 `continue-on-error` 掩盖失败。
