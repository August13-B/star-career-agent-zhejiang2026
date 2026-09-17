# 星职 · AI 职业导航与终身学习伙伴系统

> 基于《基于 AI 的大学生职业规划智能体》（国赛 A13，全国三等奖）迁移至省赛 A02 赛题《面向未来工作的 AI 职业导航或终身学习伙伴系统设计》的参赛作品。
> 更新时间：2026-08-15

## 一、项目简介

星职（StarCareer）是一套面向未来工作的 **AI 职业导航与终身学习伙伴系统**。系统以职业知识库为底座、以大模型智能体为核心，围绕"感知 → 认知 → 决策 → 生成 → 执行 → 反馈"全技术闭环，为高校学生及职场新人提供个性化职业规划、人岗精准匹配、成长路径拆解与长期陪伴服务。

系统前身《基于 AI 的大学生职业规划智能体》（国赛 A13，获全国三等奖），现按省赛 A02 赛题要求（蚂蚁百宝箱企业版平台、MCP 协议、多端发布、AI 伦理设计）进行适配演进。

### 核心价值

- **懂学生**：10 维能力画像 + 动态职业画像，量化自我认知
- **懂岗位**：10000+ 真实岗位数据构建职业认知知识底座
- **懂成长**：垂直晋升图谱 + 横向换岗图谱 + 3-5 年职业路径
- **懂陪伴**：情绪感知、目标监督激励、计划动态调整（开发中）

## 二、系统架构

```
┌─────────────────────────────────────────────────┐
│  表现交互层  Vue3 + Vite + ECharts（玻璃拟态 UI）  │
│  对话流式（SSE 打字机）· 职业星图 · 能力雷达 · 大盘   │
└──────────────┬──────────────────────────────────┘
               │ REST / SSE / 加密传输
┌──────────────▼──────────────────────────────────┐
│  后端（单一 Spring Boot 工程，已合并）             │
│  业务服务：用户/画像/测评/匹配/报告/对话/场景模拟     │
│  岗位知识库：岗位数据/图谱/AI 分析                  │
└──────────────┬──────────────────────────────────┘
┌──────────────▼──────────────────────────────────┐
│  AI 能力  蚂蚁百宝箱开放 API（A02 改造后）          │
│  知识库（岗位/职业知识）· 多智能体工作流编排          │
└──────────────┬──────────────────────────────────┘
┌──────────────▼──────────────────────────────────┐
│  数据层  MySQL · Redis                            │
└─────────────────────────────────────────────────┘
```

> 🔄 A02 改造方向：原自研 AI 服务（LangChain4j + PGVector）已退役，知识库与多智能体能力迁移至蚂蚁百宝箱企业版（开放 API 对接，链路：前端 → 后端 → 百宝箱）。

## 三、技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3、Vite、Axios、ECharts、Glassmorphism 设计 |
| 后端 | Spring Boot 3.x、MyBatis-Plus、JWT、RSA 加密 |
| AI（A02 改造后） | 蚂蚁百宝箱企业版：知识库 + 多智能体工作流编排（开放 API 对接） |
| 数据 | MySQL（业务）、Redis（缓存）、Excel 知识库 |
| 部署 | Nginx（反向代理）、Maven Wrapper、Node 20+（Vite 7） |

## 四、代码仓库结构

```
星职/
├── 前端/                        # Vue3 前端
├── 后端/                        # 统一后端（单一 Spring Boot 工程，已合并）
│   ├── src/main/java/           #   com.xingzhi（主类）+ org.example.web + wwy.example.springboot
│   ├── src/main/resources/      #   application.yml（${ENV} 占位） + mapper XML
│   ├── .env / .env.example      #   环境配置（含密钥，.env 不入库）
│   └── sql/                     #   邀请码 SQL
├── 数据库/数据库结构.sql         # MySQL youthpath 库（31 张表）
├── nginx/conf/nginx.conf        # 生产反代配置（前端静态 + /api 反代）
├── manage.py                    # 命令行服务管理器
├── manage_gui.py                # Tkinter 可视化服务管理器
├── 国赛A13至省赛A02差异分析与待办清单.md   # 差异分析与开发待办（开发 agent 用）
├── 作品材料/                     # 项目介绍材料（概要/PPT/详细方案/知识库/原始知识库材料）
└── README.md
```

## 五、主要功能

1. **智能体对话**：职业规划智能问答，SSE 流式输出 + 打字机体验（A02 改造后对话能力由百宝箱开放 API 提供）
2. **职业星图**：垂直晋升链路 + 横向换岗路径的可视化宇宙星图
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
python manage.py status        # 查看状态
python manage.py logs backend  # 查看日志
python manage.py gui           # 可视化管理器（Tkinter）
```

### 手动启动

| 模块 | 端口 | 启动方式 | 依赖 |
|---|---|---|---|
| 后端 | 8080 | `cd 后端 && mvnw spring-boot:run` | MySQL（youthpath）、`.env` |
| 前端 | 5173 | `cd 前端 && npm install && npm run dev` | — |
| Nginx（生产） | 80 | `python manage.py start nginx` | `前端/dist` 构建产物 |

> ⚠️ 生产部署：先在 Windows 侧执行 `npm run build` 生成 `前端/dist`，再由 Nginx 托管静态文件并反代 `/api/*` 到后端（详见 `nginx/README.md`）。

> 🗄️ **数据库自动灌库**：`manage.py` 启动后端前会自动检查并灌库（缺表建表、无数据导入，已有数据则跳过）。
> 也可手动执行：`python manage.py db`（灌库）/ `python manage.py db status`（查看）。
> 数据说明见 [`数据库/README.md`](./数据库/README.md)（岗位 9958 条 + 画像/能力/用户，向量数据见 `数据库/向量数据/`）。

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
| `TBOX_API_URL` | 应用基址（来自平台 `APP_API_URL`） |
| `TBOX_API_KEY` | 报名所得 `inc-ak...`（**不入库**） |
| `TBOX_AGENT_ID` | `202609APqqd122487260` |

### 🔴 当前阻塞

**模型网关未开通**：WS 调用返回 `RUN_ERROR: "Not Open"`，需在百宝箱侧开通模型后对话才能完整跑通。

> 详细接口清单、实测抓包与后端改造方案见 [`百宝箱/接口清单与接入说明.md`](./百宝箱/接口清单与接入说明.md)；
> 智能体创建提示词见 [`百宝箱/提示词-智能体创建.md`](./百宝箱/提示词-智能体创建.md)。

> ⚠️ **后端改造要点**：实测确认百宝箱对话是 **WebSocket (AG-UI)** 通道（非 HTTP 转发），后端需新增 WS 客户端并把事件流桥接为对前端的 SSE（对应 issue #15）。

## 八、当前状态与演进方向

**已完成**：用户体系、学生画像、能力测评、人岗匹配、生涯报告、岗位知识库、SSE 流式基础链路、前端全页面、后端合并、nginx/启动脚本/环境变量配置。

**开发中（对应省赛 A02 要求）**：
- 百宝箱侧：知识库迁移（向量库/岗位知识上传）、多智能体工作流编排（意图路由 / 教练 / 规划 / 情绪）、场景模拟评分节点
- 后端改造：对话转发目标切换为百宝箱开放 API、MCP 双数据源对接（招聘→职业画像，学习资源→学生画像）、评分落库
- 动态调整方案（计划重构 / 动态排期 / 情绪联动）——待实现
- 职场场景模拟训练（模拟面试 / 跨岗位沟通 / AI 辅助办公 + 画像提升闭环）——新增
- 流式输出完整化（心跳、重连、JSON 清洗、Markdown、流式图表、落库）
- AI 伦理设计、用户测试记录、多端发布（可选）

> 详细开发任务见根目录《国赛A13至省赛A02差异分析与待办清单.md》与 GitHub Issues（按阶段里程碑划分）。

