# 星职 · 项目说明与已知坑（Agent 用）

> 上次更新：2026-09-19 ｜ 更新时间阈值：7 天（每会话必读的基础内容）

## 1. 项目定位

- 赛题：省赛 A02《面向未来工作的 AI 职业导航或终身学习伙伴系统设计》
- 前身：国赛 A13《基于 AI 的大学生职业规划智能体》（全国三等奖）
- 代码仓库名：星职（StarCareer）

## 2. 架构与技术栈

- 前端：Vue3 + Vite + ECharts（`前端/`），接口一律走相对路径 `/api`，开发用 Vite 代理 → `http://127.0.0.1:8080`
- 后端：Spring Boot 3.5 + MyBatis-Plus + JWT + RSA/AES（`后端/`，两个包合并：`org.example.web` 业务、`wwy.example.springboot` 岗位库）
- AI：**蚂蚁百宝箱企业版**（WebSocket / AG-UI 事件流），链路 前端 → 后端 `TboxAgentServiceImpl` → 百宝箱
- 数据：MySQL `youthpath`（31 张表）+ Redis（可选）
- AI 参数来自 `后端/.env`（不入库）：`TBOX_API_URL` / `TBOX_API_KEY` / `TBOX_AGENT_ID`

## 3. 启动方式

```bash
python manage.py start all     # 后端(8080)+前端(5173)+nginx
python manage.py db            # 幂等灌库（仅职业数据；缺表建表/无数据导入，之后自动跑幂等迁移）
python manage.py db --force    # 重建表并重新导入（会清空现有数据）
python manage.py db migrate    # 对已有库执行幂等迁移（加宽加密列 / 修 is_deleted）
python manage.py db status     # 查看客户端/连接/表数/岗位数
python manage.py free-port backend
```

> 🔒 **灌库只导入职业数据**：`job_info` / `job_requirement_profile` / `job_*` / `invitation_code`；
> **不导入** `user` 账号、`student_profile` 画像、`student_ability(_score)`、`ai_*` 对话、`match_*`、`career_report*` 等用户数据。
> 含用户数据的完整备份见 `数据库/数据库数据-全量备份.sql`（已 gitignore，不参与灌库）。

## 4. 关键约定 / 已知坑（务必先看）

1. **token 前缀归一在登录端**：`JwtUtil.genToken()` 自带 `Bearer `；前端登录后必须 `replace(/^Bearer\s+/i,'')` 再存，其它处只判断是否已带前缀。（历史上 `Bearer Bearer` → 401 → 旧 userId 残留 → 外键失败）
1.5 **64 位雪花 ID 一律以字符串传输**：后端 `JacksonConfig` 已把 `Long/long` 序列化为字符串；前端**切勿 `Number(id)`/`parseInt(id)`**（会静默丢精度 → 外键失败）。请求体里 ID 传字符串，Jackson 会自动转 Long。
2. **画像敏感字段用 RSA 写入**（`rsaEncrypt`），读取优先 `rsaDecrypt`、兜底 `decryptFromDB`（早期 AES 数据）。用错方法会得到 Base64 乱码。
3. **加密列宽 ≥ varchar(1000)**：RSA-1024 密文 ≈172 字符；`expected_salary` 曾只有 varchar(50)。
4. **逻辑删除 `is_deleted` 必须显式写 0**：查询条件是 `is_deleted = 0`，NULL 会导致「保存成功却查不到」。
5. **多语句 SQL 依赖 `allowMultiQueries=true`**：`StudentProfileMapper`/`StudentAbilityMapper` 的 `insert/update` 是 `insert...; select...`。
6. **`/api/user/getUserInfo` 的 IV/AES 为可选**：只取 id/nickname/userRole 的调用点不必传；否则会 400 被静默吞掉。
7. **导入 SQL 必带 `--default-character-set=utf8mb4`**（否则 `Unknown command '\"'`）。
8. **端口/IPv4**：Vite 代理与后端检查一律用 `127.0.0.1`，避免 localhost 解析到 IPv6 `::1`。
9. **`数据库结构.sql` 与 `migrations/` 必须同步**：历史上出现「结构漂移」——建表脚本仍是旧列宽，只有迁移修过。
10. **Mapper 必须「有实现」**：`@Mapper` 接口若无 XML、无注解，运行期报 `Invalid bound statement`（曾漏 `MatchDetailMapper`、`CareerReportMapper`）。
11. **职业报告 = 平台异步任务 + 前端 offsets 增量轮询**（网关缓冲长响应，SSE 公网不可用）：
    - 后端 `POST /api/career-report/start` → 平台 `POST /api/report` → `202 {jobId}`
    - 后端 `GET /api/career-report/jobs/{jobId}?offsets={...}` → 平台同名接口（透传 offsets）；
      running 返回 `deltas:[{agent,data}]`（按 offsets 切片，不重不漏）+ `currentAgent/agentsDone/segmentChars/progressChars`
    - 前端每 1.5s 轮询：`deltas` 追加到卡片缓冲 → 本地 30ms 均匀打字机实时呈现；done 用 `content.agents[]` 校准
    - `jobId` 存 localStorage，刷新/切页可续（任务在平台侧独立运行，与浏览器连接无关）
    - 后端首次 done 时按 `content.agents` 幂等落库 MySQL；前端详情/PDF 用我们的 `reportId`
    - 注意：平台 done 帧顶层 `agents` 为 `null`，6 段在 **`content.agents[]`**（key/name/content）
12. **纯文本对话默认走平台 SSE**：`POST {TBOX_API_URL}/api/chat/stream`（`TBOX_CHAT_CHANNEL=http`，待平台提供）；
    平台未就绪时设 `TBOX_CHAT_CHANNEL=ws` 回退 WS。带图片对话始终走 `WS /ws`。
    - 对话上下文：后端用 `StudentProfileContextService.build()` 注入账号画像 + 本轮问题；多轮历史由平台按 `conversationId` 注入。
    - 帧：`{"delta":...}`* + `{"type":"tool",...}` + `{"done":...}` + `{"error":...}`；我们 `done` 时另存 MySQL。

## 5. 当前阻塞（平台侧）

- 百宝箱**模型网关未开通**：WS 返回 `RUN_ERROR: Not Open`，需在平台开通并重新发布。
- 应用**系统提示词**需写入《系统提示词·变更指令》，6 个智能体段才会完整输出。

## 6. 参考文档

- `README.md`、`问题汇总与修复记录.md`、`国赛A13至省赛A02差异分析与待办清单.md`
- `百宝箱/接口清单与接入说明.md`、`百宝箱/提示词-*.md`
- `数据库/README.md`、`多智能体报告-完整流程指引.md`

## 7. 协作约定（本仓库）

- **「自动化推送」= 管理员 + 合并后删除分支**（用户已设为默认，无需再问身份与删除意向）。
  流程：`git pull origin develop` → `git push -u origin <分支>` → `gh pr create --base develop`
  → `gh pr checks`（无 CI 则跳过）→ `gh pr merge --merge --delete-branch`。
- 其它改动遵循「小步提交」；后端 Java 改动需用户重启后端，前端改动 Vite 热更新。
- Agent 不启动/构建项目（`npm run build`、`mvnw spring-boot:run` 等由用户在 Windows 侧执行）。
