# 星职 · AI 职业导航与终身学习伙伴

面向大学生的职业探索、能力评估、职业规划和职场训练 Web 应用，使用 Vue 3、Spring Boot、MySQL 与蚂蚁百宝箱，由国赛 A13 项目持续演进到省赛 A02。

> 本文依据当前代码和 2026-09-24 本地复测更新。三类职场训练已经实现，本轮发现的跨账号访问、匿名岗位写入、图片校验和错误处理问题已修复。**当前百宝箱网关返回 HTTP 503 `Agent not found`，真实 AI 完整流程尚未通过本轮验收**。最新排查见[百宝箱 503 原因与重试记录](docs/百宝箱503排查-20260924.md)，功能复测见[全面复测与修复记录](docs/全面复测与修复-20260924.md)，[此前验收报告](docs/项目完整验收-20260924.md)保留为历史记录。演示页面和预留接口不计作完整功能。

> 2026-09-24 后续补测：使用隔离数据库和本地 HTTP/WebSocket 模拟平台，69 项业务流程检查及 60 项回归测试全部通过，外部 SMTP 1 项跳过；修复了 WS 断流误存回答和问卷能力记录缺少画像关联的问题。详见[离线模拟验收及复现步骤](docs/离线模拟验收-20260924.md)。该结果验证本地业务链路，真实平台连通性及模型质量仍需组内验收。

## 1. 页面与功能

推荐顺序：**登录 → 个人中心建档和问卷 → AI 能力分析 → 多智能体报告 → 成长计划 → 职场训练 → 查看反馈与能力变化**。

| 页面 / 入口 | 实际功能 | 条件与边界 |
|---|---|---|
| 登录 `/login` | 账号、邮箱或昵称登录；注册、邮箱验证码、密码找回入口 | 登录已验收；邮件依赖 SMTP，本轮未发送；微信、QQ、钉钉按钮不代表完成第三方登录接入 |
| 智能体对话 `/` | 新建会话、流式问答、历史、重命名、结束会话；咨询职业方向和最新报告 | 需要登录及百宝箱配置；普通聊天与六段报告不同；图片分支需单独验收 |
| 个人中心 `/profile` | 基础档案、职业意向、硬实力、六维软能力分数、报告列表、PDF 下载、密码修改入口 | 先建档再测评；简历上传解析尚未接入，入口已禁用并说明 |
| AI 能力测评 `/ai-score` | 基于档案和能力资料调用 AI，展示十维分数、雷达图、评语并保存 | 与个人中心的随机情境问卷不同；资料不足时先完善画像 |
| 职业星图 `/graph?reportId=<联合测评报告ID>` | 中心岗位 + 目标/进阶/迁移方向分支，每个分支含测评原文依据、待补能力与行动建议 | 来自**完整联合测评报告**（六段齐全）；无可用测评时只显示**明确标注「视觉示意」的预览**，不代表个人测评结果 |
| 岗位详情 `/job-detail` | 岗位信息、能力要求、市场和路径资料 | 由岗位页面带参数进入；`job_info.id` 与岗位画像 `job_id` 不是同一 ID |
| 岗位对比 `/compare` | 选择岗位，调用 AI 与已有画像、路径比较 | 依赖平台输出格式；本轮未逐一测试所有岗位组合 |
| 多智能体中枢 `/multi-agent` | 补充诉求、六段输出、展开内容、停止生成、恢复最近报告 | 平台执行、前端轮询；切页回来可继续；完成后保存报告并提取成长目标 |
| 个人成长 `/growth` | 1／3／5 年计划；新增计划和任务、修改状态、进度、完成记录、删除 | 来自报告或手动创建；训练建议可加入计划；仅能读写本人数据 |
| 职场训练 `/training`、`/training/:sessionId` | 三类场景、分阶段对话、作品版本、恢复、证据评分、能力变化和成长任务 | 已实现闭环，详见下一节 |
| 岗位管理 `/admin/job-info` | 岗位分页、筛选、增删改、要求维护、AI 分析入口 | 页面与后端写接口均要求管理员；岗位资料读取公开，AI 对比要求登录 |
| 导师大屏 `/tutor-dashboard` | ECharts 图表和布局演示 | 导师或管理员可进入；静态示例数据，已标识，不是实际学生统计或风险判断 |

侧栏“推荐专属路线”属于展示入口，不代表个性化推荐。旧 `/match` 的部分匹配、收藏、置顶，以及旧报告分享、通用导出仍含 TODO，接口存在不等于功能完成。Word 简历导出是后端独立能力，不等于上传解析。

## 2. A02 职场场景模拟训练

| 场景 | 用户任务 | 反馈 |
|---|---|---|
| 模拟面试 `interview_backend_intern.v1` | 6 阶段：经历、技术选择、问题分析、协作、压力应对、复盘 | 专业、沟通、问题解决、抗压四维反馈与回答证据 |
| 跨岗位沟通 `communication_release.v1` | 5 轮产品 / 开发 / 测试交流，提交范围、负责人、期限、验收、风险、待确认项 | 引用对话与共识清单，评价约束、取舍和协作表达 |
| AI 辅助办公 `office_review.v1` | 3 阶段：指令、独立核验 AI 初稿、修订活动复盘和待办 | 校验给定事实：报名 54、转化率 18%、社群最高、投放未决；事实错误触发评分上限 |

- 入门 / 标准难度；会话冻结题目和评分规则。
- 服务端保存回答草稿、作品草稿、版本和消息；切页 / 刷新可恢复；旧版本提交返回冲突。
- 支持跳过、提前结束、取消、失败重试；不完整作答标为部分完成。
- 评分必须含合法维度和可回查证据，不合规则进入待复核，不伪造分数。
- 选择用于画像更新后，还须满足完整有效评分、原有十维基线有效、画像版本未变化等条件。只更新本场景覆盖的四维，保留其余维度并写历史；条件不足则显示跳过原因。
- 有效评分的改进建议可加入自己的成长计划，重复提交不会重复创建同一训练建议任务。

详见[职场训练完整使用说明](docs/职场训练-完整实现与使用说明.md)。旧设计文档中的远期设想不代表当前交付能力。

## 3. 多智能体与系统分工

六段报告：**画像分析 → 职业探索 → 目标设定 → 路径规划 → 行动计划 → 报告整合**。

```text
Vue 浏览器：页面、图表、流式展示、训练工作台
    │ 同源 /api
Spring Boot：业务接口、数据保存、训练编排、平台适配
    ├── MySQL：档案 / 能力 / 对话 / 报告 / 计划 / 训练 / 岗位
    └── 蚂蚁百宝箱应用
          ├── WebSocket 对话（后端转换为 SSE 或训练结果）
          └── 六智能体异步报告任务（后端查询并落库）
```

百宝箱应用、平台知识库和模型配置在平台侧维护，**clone 本仓库不会复制或部署这些平台资源**。本仓库提供调用适配、画像上下文、结果处理，以及训练状态和评分规则。

报告第六段的结构化目标可转为 1／3／5 年计划。异常或超时可能保存“部分完成”报告，页面会提示；不能把它算作全部完成。部分任务缓存仍在进程内，重启、多实例与长期运行需要专项验收。

## 4. 目录与技术栈

```text
前端/src/
  views/                     对话、星图、个人中心、报告、成长、训练等页面
  components/                公共组件、问卷、训练作品和结果
  utils/                     加密、SSE 解析、训练 API
  router/index.js            页面路由
后端/
  .env.example               配置模板；真实 .env 不提交
  src/main/java/com/xingzhi/  合并应用启动入口
  src/main/java/org/example/web/
    controller/              用户、画像、对话、报告、成长、训练接口
    service/training/        模板、执行、证据校验、画像更新
    mapper/                  数据访问
  src/main/java/wwy/example/springboot/
                             岗位知识库、分析、对比、图谱模块
  src/main/resources/
    application.yml          环境和平台配置
    mapper/、mappers/         两个历史模块的 MyBatis XML
    training/、data/          训练模板、能力问卷题库
  src/test/                  流式、数据库、训练和分页回归
数据库/
  数据库结构.sql              全量建表，含 DROP，不能覆盖已有库
  数据库数据.sql              岗位种子数据，不包含业务用户数据
  migrations/                增量迁移，含 010 / 011 训练表、012 报告任务归属
百宝箱/                      平台接口说明、提示词参考
nginx/                       静态站点和 /api 代理
scripts/                     启动安全测试、后端测试入口、真实平台验收
docs/                        使用说明、训练设计、验收记录
manage.py / manage_gui.py     命令行 / Tkinter 服务管理
```

- 前端：Vue 3.5、Vue Router 4、Vite 7、Axios 1.20、ECharts 6.1、CryptoJS、JSEncrypt。
- 后端：JDK 17、Spring Boot 3.5.7、MyBatis-Plus 3.5.14、Web / WebFlux、MySQL 8、JWT、RSA / AES。
- 当前运行链路不要求 Redis；历史架构介绍不等于实际依赖。
- 本次本地库应用迁移后为 **41 张表、9,958 条岗位记录**，是种子快照，不是实时招聘统计，也不保证每个岗位均有完整画像。星图必须由**完整联合测评**生成：无法从报告中提取有效依据时明确失败，不渲染孤立中心节点充数。

## 5. 配置和启动

### 环境

| 工具 | 要求 |
|---|---|
| Java | JDK 17，设置 `JAVA_HOME` 和 PATH |
| Node.js | `^20.19.0` 或 `>=22.12.0`，以 package.json 为准 |
| MySQL | 8.x，准备 `youthpath` 库和访问权限 |
| Python | 3.10+；管理脚本用标准库，GUI 需要 Tkinter |
| Maven | 已有 Maven Wrapper，无需另装 |
| Nginx | 可选；本地开发用 Vite |

### 配置文件

**后端 `后端/.env`；前端公钥 `前端/.env.local`。** 当前电脑路径为 `D:\my\star-career-agent-zhejiang2026\后端\.env` 和 `D:\my\star-career-agent-zhejiang2026\前端\.env.local`。

首次复制 `后端/.env.example` 为 `.env` 后填写，已有文件不要覆盖。

1. **智能体对话**：职业规划智能问答，SSE 流式输出 + 打字机体验（A02 改造后对话能力由百宝箱开放 API 提供）
2. **职业星图**：以测评目标岗位为中心，展开目标方向 / 进阶路径 / 迁移方向三类分支（含测评原文依据、待补能力、行动建议），星座式可视化
3. **AI 能力测评**：多维能力量化评估（能力测评/评分历史）
4. **人岗匹配**：10 维双向画像匹配，输出匹配度与差距分析
5. **生涯报告**：职业探索→目标设定→路径规划→行动计划，支持润色/编辑/导出
6. **职场场景模拟训练（新增，A02 要求）**：模拟面试 / 跨岗位沟通 / AI 辅助办公，训练结果反哺学生能力画像
7. **学生就业大盘（B 端）**：班级/院系就业数据看板（当前为静态展示）
8. **岗位管理（B 端）**：岗位数据维护、AI 自动分析入库

## 六、快速启动

### 环境准备

- JDK 17+、Maven Wrapper（已内置）、Node 20+、MySQL（导入 `数据库/数据库结构.sql` 到 `youthpath` 库）、Redis（可选）
- Python 3（服务管理器用）

### 配置环境变量

```bash
cd 后端
cp .env.example .env     # 填写数据库/邮件/RSA/AES 等真实值
```

### 一键启动（推荐）

```bash
python manage.py start all     # 启动 后端 + 前端 + nginx
python manage.py status        # 查看状态（含前端构建时间）
python manage.py build         # 构建前端（npm run build → 前端/dist）
python manage.py logs backend  # 查看日志（也可 logs build 看构建日志）
python manage.py gui           # 可视化管理器（Tkinter，含「🔨 构建前端」按钮）
```

### 手动启动

| 模块 | 端口 | 启动方式 | 依赖 |
|---|---|---|---|
| 后端 | 8080 | `cd 后端 && mvnw spring-boot:run` | MySQL（youthpath）、`.env` |
| 前端 | 5173 | `cd 前端 && npm install && npm run dev` | — |
| 前端构建 | — | `python manage.py build`（或 `cd 前端 && npm run build`） | Node.js；产物 `前端/dist` |
| Nginx（生产） | 80 | `python manage.py start nginx` | `前端/dist` 构建产物；Windows 实例在 `C:\xingzhi-nginx` |

> ⚠️ 生产部署：先在 Windows 侧执行 `npm run build` 生成 `前端/dist`，再由 Nginx 托管静态文件并反代 `/api/*` 到后端（详见 `nginx/README.md`）。
> ⚠️ Windows 版 nginx **不能用中文 `-p` 前缀**（报 1113），因此本机实例放在 `C:\xingzhi-nginx`（ASCII），中文路径只出现在 conf 的 `root` 里；`manage.py` 已自动适配。

> 🗄️ **数据库自动灌库**：`manage.py` 启动后端前会检查数据库；空库初始化，已有库执行幂等增量迁移。已有但不完整的库不会自动覆盖，请先备份核查。
> 也可手动执行：`python manage.py db`（灌库）/ `python manage.py db status`（查看）。
> 数据说明见 [`数据库/README.md`](./数据库/README.md)（岗位 9958 条 + 画像/能力/用户，向量数据见 `数据库/向量数据/`）。

### 简历 PDF 解析与画像更新策略

个人中心「📄 简历附件库」可上传**文字版 PDF 简历**：后端用 PDFBox 提取文本（**扫描件无法识别，会明确提示**）→ AI 只抽取**五件套**（学历 / 专业 / 技能 / 证书 / 实习与项目经历）→ 并入个人资料并给**硬实力四项**加分。

**合并规则**：不覆盖你已填写的非空值；技能 / 证书 / 经历类字段**去重追加**。

**画像更新幅度策略（所有写入方统一，代码见 `service/profile/ProfileScorePolicy.java`）**：

| 阶段 | 规则 |
|---|---|
| **首次评估**（当前画像尚未被任何评估更新过）| 相对基线 60 大幅调整：**±35 → 25~95** |
| **再次评估** | 单维单次变化 **≤10**（升也受限）；**降低必须带理由**，无理由则保持原值 |
| **简历解析加分** | **只加不减**，单项 **≤+15**，且仅在首次评估或该维度此前为空时生效 |

总分口径不变：**硬实力四项均值 × 30% + 软实力六维均值 × 70%**。

**自动更新与提示**：下列操作完成后都会自动写入画像并记录 `change_source` / `change_detail`（最近一次变更的来源、理由、各维度 before→after）——
能力补充测评、职场训练（仿真）、简历 PDF 解析、多智能体联合测评。

- 联合测评报告生成完成后，前端自动调 `POST /api/profile/score/report/{reportId}` 同步画像（**幂等**：同一份报告只计分一次，标记写在报告 JSON 的 `profileScoredAt`）；
- 前端统一调 `GET /api/profile/change/latest` 弹一条「画像已更新 · 来源 + 各维度变化」轻提示（同一变更只提示一次），并提供「查看画像 →」；
- 个人中心的「我的核心能力模型」也直接展示最近更新时间、来源与变化。

**职业星图不参与评分**（它是多智能体报告的可视化衍生视图，避免同一份测评被重复计分）。
提示文案只描述结果，不向前端暴露评分策略内部细节。

**已有数据库需执行迁移**：`python manage.py db migrate`（或直接 `python manage.py start backend`，启动前会自动跑幂等迁移）。

### 个人成长：任务提交 → 难度自评 → 下一周期计划

个人成长页（`/growth`）的任务支持**提交文件**（PDF / 图片 / Word / PPT / Excel / 压缩包，单个 ≤10MB）：
**提交即视为完成**（写入 `grow_task.grow_recourse`，任务标记已完成并重算计划进度）。

任务完成后，任务行右侧出现**本次完成难度**选择：**太简单 / 中等 / 困难**
（写入 `grow_task.effect_score`：太简单=5、中等=3、困难=1，并记入 `adjustment_reason`）。

选好难度后可点「**生成下一周期计划**」：后端把「上一周期计划 + 每个任务的完成情况与难度自评」交给
百宝箱，生成**具体可验收**的下一周期任务清单（任务名/类型/说明/预期成果/目标能力/资源/起止日期），
并**新建一条 `grow_plan`**（`parent_plan_id` 指回上一周期、`cycle_round` +1），**上一周期整体保留为历史**。

> 幂等：同一计划已生成过下一周期时不再重复调用模型（提示后可手动「重新生成」）。
> 难度含义会一并交给模型：太简单→加量/提速/提高目标；中等→保持节奏小幅加难；困难→拆小步/适当延长周期。
> 需执行迁移：`python manage.py db migrate`（迁移 016 为 `grow_plan` 增加 `parent_plan_id` / `cycle_round`）。

### 职场模拟训练与评分

登录后从侧栏进入「职场训练」或打开 `/training`，可选择目标岗位，完成模拟面试、跨岗位沟通或 AI 辅助办公练习。训练结束后由百宝箱生成分维度评分、回答证据和改进建议；只有通过证据校验的完整训练，且用户主动勾选并已有能力基线时，才会更新个人画像。训练分与能力画像总分是不同指标。

首次启动后端时，`manage.py` 会增量执行 `数据库/migrations/010` 至 `013`；已有数据不会被清空。平台已部署独立训练接口时，在 `后端/.env` 配置 `TBOX_TRAINING_PATH_MOCK_INTERVIEW`、`TBOX_TRAINING_PATH_CROSS_ROLE`、`TBOX_TRAINING_PATH_AI_OFFICE`，如需专用令牌再配置 `TRAINING_API_TOKEN`。未配置训练接口路径时回退现有 WebSocket 对话链路，真实 AI 评分仍取决于百宝箱侧训练场景与评分提示词是否就绪。

本地验证：在 `前端` 执行 `npm test && npm run build`，在 `后端` 执行 `mvnw test`。真实 MySQL 集成测试需额外设置 `TRAINING_DB_TEST=true` 和 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`；测试只创建并清理带专用前缀的临时用户。

## 七、百宝箱应用接入（已实测 ✅）

省赛 A02 要求依托**蚂蚁百宝箱企业版**开发，本项目的**知识库与多智能体能力部署在百宝箱侧**，自研后端通过接口对接（链路：前端 → 后端 → 百宝箱）。

**应用基址**（来自平台注入环境变量 `APP_API_URL`，预览态域名会变，勿硬编码）：
```
https://202609APqqd122487260-coding.tboxpro.cn
```

### 已实测通过的通道

| # | 通道 | 地址 | 结果 |
|---|---|---|---|
| 1 | 健康检查 | `GET /api/health` | ✅ `{"status":"ok"}` |
| 2 | 对话页 H5 | `GET /` | ✅ HTTP 200 |
| 3 | 创建会话 | `POST /api/conversation/create` | ✅ 返回 `conversationId` |
| 4 | 历史导出 | `GET /api/conversation/messages?format=raw` | ✅ 分页结构正常 |
| 5 | 平台会话 | `GET /api/tbox/session` | ✅ 返回 `sessionId / appId` |
| 6 | **对话通道** | `WSS /ws` | ✅ 握手 + HELLO + SEND_MESSAGE + `RUN_STARTED` 全通 |
| 7 | 知识库 OpenAPI | `POST api.tbox.cn/api/datasets/retrieve` | ✅ 鉴权通过（待补 datasetId） |

### 对话协议（AG-UI）

```
客户端：HELLO{sessionId} → SEND_MESSAGE{content, sessionId, conversationId?}
        （可选 CANCEL_RUN / UI_ACTION）
服务端：RUN_STARTED
        → TOOL_CALL*（searchJobs 双库 RAG：岗位记录 | 能力画像）
        → TEXT_MESSAGE_CONTENT(delta)*   ← Markdown 正文
        → CUSTOM('tbox:card')            ← 结构化卡片
        → RUN_FINISHED{requestId}
```

**结构化卡片契约**：
- `career_pathway`：`{title, phases[{phase, goal, key_actions[], timeline}]}`（3-5 年路径）
- `scenario_score`：`{scenario, dimensions{professional/communication/teamwork/problem_solving/learning/innovation/pressure}, total, comment, suggestions[]}`（与库表 `student_ability_score` 一一对应）
- `weather`：`{city, temp, weather, humidity}`

**模型输出协议**：`{"response":...}` / `{"career_pathway":{...}}` / `{"scenario_score":{...}}`

### 环境变量（`后端/.env`）

| 变量 | 说明 |
|---|---|
| `SERVER_PORT` | 默认 8080 |
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | MySQL 地址和凭据；端口按本机实际配置填写，保留 `allowMultiQueries=true` |
| `RSA_PRIVATE_KEY`、`RSA_PUBLIC_KEY` | 匹配的 RSA 密钥对，PKCS#8 私钥 / X.509 公钥，Base64 DER 单行 |
| `AES_KEY`、`AES_IV` | 按模板填写 16 个 ASCII 字符；已有数据时不要随意换密钥 |
| `JWT_SECRET` | 必填，至少 32 字节的随机签名密钥，禁止使用示例常量；只写入未提交的后端 `.env`。更换后旧登录失效，需重新登录 |
| `MAIL_HOST`、`MAIL_PORT`、`MAIL_USERNAME`、`MAIL_PASSWORD` | 验证码、密码找回；通常填写 SMTP 授权码 |
| `TBOX_API_URL`、`TBOX_API_KEY`、`TBOX_AGENT_ID` | 小组百宝箱应用地址、密钥和应用 ID |
| `TBOX_REPORT_TOKEN` | 平台启用报告专用令牌时必填，否则可留空 |
| `TBOX_CHAT_CHANNEL` | 当前用 `ws`，其它通道依赖平台实现 |
| 其它 `TBOX_*` | 超时、轮询、握手间隔，先沿用模板 |

前端 `.env.local` 填与后端 `RSA_PUBLIC_KEY` 相同的公钥：

```dotenv
VITE_RSA_PUBLIC_KEY=填写与后端私钥匹配的公钥
```

修改后重启 Vite。私钥、数据库密码、平台密钥不能放入 `VITE_*`，这些变量会进入浏览器产物。

`manage.py` 读取 `.env` 并启动服务；`manage_gui.py` 提供启停、状态、日志，**没有自动获取平台密钥或生成全部配置的向导**。`scripts/run_backend_tests.py` 加载 `.env` 供数据库测试使用。

### 在仓库根目录启动

```powershell
python manage.py db status
python manage.py db migrate
python manage.py start backend
python manage.py start frontend
python manage.py status
```

访问 **http://localhost:5173**；后端前缀为 `http://localhost:8080/api`。`python manage.py gui` 打开管理界面；`python manage.py stop backend` / `stop frontend` 停止服务。

空库首次初始化可执行 `python manage.py db seed`，需要 MySQL 客户端及建表 / 导入权限。已有但不完整的库会停止自动初始化，避免结构脚本覆盖数据。**已有数据先备份，再做增量迁移，不要运行 `db seed --force`。** 种子 SQL 使用固定库名 `youthpath`，其它库名需先调整脚本。

前端首次启动会安装依赖；按锁文件安装请在 `前端` 执行 `npm ci`；构建执行 `npm run build`。Nginx 见[部署说明](nginx/README.md)。修改后端端口时，同时核对 Vite 和 Nginx 代理目标。

## 6. 主要接口

路径统一加 `/api`。这是主要页面接口摘要，不代表全部历史接口已验收。

| 功能 | 示例 |
|---|---|
| 账号 | `POST /user/login`、`POST /user/register`、`GET /user/getUserInfo` |
| 画像 | `POST /student/condition`、`POST /student/insert`、`PUT /student/update` |
| 能力 | `GET /ability/quiz`、`POST /ability/quiz/submit`、`GET /ability/score/user/{userId}`、`POST /ai/analysis/ability/score` |
| 对话 | `POST /ai-conversation/create`、`POST /ai-conversation/send-stream`、`GET /ai-conversation/history/{id}`、`PUT /ai-conversation/update-title` |
| 报告任务 | `POST /career-report/start`、`GET /career-report/jobs/{jobId}`、`POST /career-report/jobs/{jobId}/cancel` |
| 报告和 PDF | `GET /career-report/user/{userId}`、`POST /career-report/batch-delete`、`GET /career-report/{id}/export/pdf`，`mode=report` 或 `full` |
| 成长 | `GET/POST /grow/plans`、`POST /grow/tasks`、`PATCH /grow/tasks/{id}`、`POST /grow/tasks/{id}/records` |
| 岗位 / 图谱 | `GET /job-info/page`、`GET /job-detail/by-job-info/{jobInfoId}`、`GET /analysis/graph/{profileId}`、`POST /job-compare/analyze-new-job` |
| Word 简历 | `GET /resume/export/{userId}` |
| 训练 | `GET /training/templates`、`GET/POST /training/sessions`、`GET /training/sessions/{id}`、`POST /training/sessions/{id}/turns` |
| 作品 / 评分 | `PUT /training/sessions/{id}/artifact-draft`、`POST /training/sessions/{id}/artifacts`、`POST /training/sessions/{id}/finish`、`GET /training/sessions/{id}/evaluation` |
| 训练后续动作 | `POST /training/sessions/{id}/profile/retry`、`GET /training/growth/plans`、`POST /training/sessions/{id}/growth-task` |

历史模块成功码有 `10001`、`200` 等格式，前端按模块处理。Long ID 以字符串返回，避免 JavaScript 丢精度，不要转成 Number。训练用 HTTP 201 / 202 / 404 / 409 表达创建、排队、归属和版本冲突。

## 7. 测试和验收

```powershell
# 标准库测试，不连接数据库
python scripts/test_manage.py

# 数据库测试：先停后端，避免恢复任务干扰测试
python manage.py stop backend
python scripts/run_backend_tests.py
python manage.py start backend

# 前端
cd 前端
npm test
npm run build
```

后端入口读取 `.env` 并启用 `TRAINING_DB_TEST=true`。CI 使用独立 MySQL；本地仅操作新建临时记录，不能对真实用户库运行重置脚本。

真实平台验收需先启动后端，在测试 Python 环境安装 `requests pymysql cryptography`，从仓库根目录执行：

```powershell
python scripts/project_acceptance.py --keep-fixtures
python scripts/project_extended_acceptance.py
python scripts/project_acceptance.py --cleanup
python scripts/training_smoke.py
```

脚本使用百宝箱配置产生模型调用，创建虚构账号并清理本轮记录，不发送邮件；结果写入忽略的 `logs/`。扩展脚本复用上一条命令保留的两个账号，验证权限、图片、边界参数、三场景草稿及 SMTP TLS/认证；它不验证邮件投递。任一失败会非零退出，平台报错也保留 FAIL。即使前序失败，也应执行 `--cleanup` 清理本轮账号；浏览器验收须在清理前完成。

本轮结果：后端 **46/46**、前端 **9/9**、启动脚本 **5/5** 通过；前端构建通过，依赖审计为 **0 个已知漏洞**。真实接口主流程 **9/12**、扩展 **26/27** 通过，其余 AI 项因平台 503 失败；训练完整流程在首轮 AI 调用处中断。不能据此宣称完整 AI 验收通过。

平台连通诊断用 `python scripts/tbox_diagnose.py`（不发密钥）；专门重试未通过的 AI 功能用 `python scripts/retry_failed_ai.py`。21:44 定向重试的 7 项仍全部失败，网关找不到预览地址对应的应用，需要先在百宝箱控制台确认应用状态和最新访问地址；详见上方排查记录。

22:00 补查模板中的 `https://<appid>.tboxpro.cn`：用实际 ID 替换后返回 401；带现有 API Key 仍提示 `Invalid token`。这与实际 `.env` 中带 `-coding` 地址的 503 是两个入口问题。正式入口的 HTTP 鉴权尚待确认，因此未直接替换实际配置。

GitHub Actions 已配置后端、前端测试 / 构建与汇总门禁。本轮只做本地验证，**没有推送，没有宣称远端 CI 已运行**。

## 8. 当前边界和下一步

1. **先恢复百宝箱，再验收 AI 闭环**：由小组核实应用运行状态及 `.env` 中的平台地址，恢复后重跑普通对话、能力分析、岗位对比、六段报告 / PDF、三场景训练 / 画像回写 / 成长任务。
2. **权限已加强，仍需生产专项检查**：档案、能力、图片、对话、报告、简历和成长按账号校验；岗位写入要求管理员；JWT 已配置化并校验数据库中的角色及账号状态。报告任务归属持久化到 `career_report_job`；升级前未记录归属的旧任务需要重新发起。报告完成去重的部分状态仍在内存，不能宣称多实例或中途重启完全可靠。
3. **补齐 A02 对接与交付**：学习 / 招聘平台 API 演示、真实导师端、简历解析、报告分享等尚未全部实现；小程序不在本 Web 仓库交付范围内。
4. **补做专项验收**：真实验证码投递及找回闭环、图片对话、全部后台写接口、生产 Nginx、压力与长期运行、多实例、健康 AI 生成中切页和重启恢复。本轮 20 次 / 5 并发只算轻量读取检查。
5. **改善体验**：部分 Markdown 表格仍以文本显示；前端共享包约 1.54 MB（gzip 526 KB），构建有体积告警；普通聊天切页行为仍需在 AI 恢复后专项回归。

当前可以使用已通过的本地档案、问卷、知识库、成长和简历流程；依赖 AI 的流程等待平台恢复。尚不能宣称“全站无缺陷”或“A02 全部完成”。

## 9. 小组 Git 协作

按任务说明：操作前检查本地和远端变动，在功能分支开发；上传前再次同步检查，避免覆盖队友代码；PR 目标为 `develop`，通过 CI 后评审合并。不要直接推送 `main` / `develop`，不要删除他人分支。

提交前确认 `.env`、`.env.local`、密钥、日志、导出文件和临时账号不在暂存区。当前本地实现及文档尚未上传 GitHub。
