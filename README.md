# 星职 · AI Career Navigator & Lifelong Learning Companion

> A provincial competition (A02) entry adapted from the national competition (A13, 3rd prize) project *AI Career Planning Agent for College Students*.
> Updated: 2026-08-15

## 1. Introduction

**StarCareer (星职)** is an AI-powered **career navigation & lifelong learning companion system** for the future of work. Built on a career knowledge base with LLM agents at its core, it implements the full closed loop of *Sense → Recognize → Decide → Generate → Execute → Feedback* to deliver personalized career planning, precise job-person matching, growth path decomposition, and long-term companionship for college students and early-career professionals.

For the provincial A02 competition, the system is being adapted to the **Ant Tbox (蚂蚁百宝箱) Enterprise platform**: knowledge base + multi-agent workflow orchestration, MCP protocol integration, multi-platform publishing, and AI ethics design.

### Core Value

- **Knows the student**: 10-dimension ability profile + dynamic career profile
- **Knows the job**: 10,000+ real job postings forming the career knowledge base
- **Knows growth**: vertical promotion graph + lateral transfer graph + 3–5 year career path
- **Knows companionship**: emotion awareness, goal supervision & motivation, dynamic plan adjustment (in development)

## 2. Architecture

```
┌─────────────────────────────────────────────────┐
│  Presentation Layer  Vue3 + Vite + ECharts       │
│  Streaming chat (SSE) · Career Star Map · Radar  │
└──────────────┬──────────────────────────────────┘
               │ REST / SSE / encrypted transport
┌──────────────▼──────────────────────────────────┐
│  Backend (single Spring Boot app, merged)        │
│  Business: user/profile/assessment/match/report  │
│  Job KB: job data/graphs/AI analysis             │
└──────────────┬──────────────────────────────────┘
┌──────────────▼──────────────────────────────────┐
│  AI Capability  Ant Tbox Open API (A02)          │
│  Knowledge base · Multi-agent workflow           │
└──────────────┬──────────────────────────────────┘
┌──────────────▼──────────────────────────────────┐
│  Data Layer  MySQL · Redis                       │
└─────────────────────────────────────────────────┘
```

> 🔄 A02 migration: the former in-house AI service (LangChain4j + PGVector) is retired; knowledge base & multi-agent capabilities move to Ant Tbox Enterprise (API integration: frontend → backend → Tbox).

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Vue 3, Vite, Axios, ECharts, Glassmorphism UI |
| Backend | Spring Boot 3.x, MyBatis-Plus, JWT, RSA encryption |
| AI (A02) | Ant Tbox Enterprise: knowledge base + multi-agent workflow (Open API) |
| Data | MySQL (business), Redis (cache), Excel knowledge base |
| Deploy | Nginx (reverse proxy), Maven Wrapper, Node 20+ (Vite 7) |

## 4. Repository Structure

```
├── 前端/                        # Vue 3 frontend
├── 后端/                        # Merged Spring Boot backend (single project)
│   ├── src/main/java/           #   com.xingzhi + org.example.web + wwy.example.springboot
│   ├── src/main/resources/      #   application.yml (${ENV} placeholders) + mapper XMLs
│   ├── .env / .env.example      #   environment config (secrets, gitignored)
│   └── sql/                     #   invitation-code SQL scripts
├── 数据库/数据库结构.sql         # MySQL youthpath schema (31 tables)
├── nginx/conf/nginx.conf        # reverse proxy config (prod)
├── manage.py                    # CLI service manager
├── manage_gui.py                # Tkinter visual service manager
├── 作品材料/                     # competition materials (PPT, docs, knowledge base)
├── docs/                        # project docs (A13→A02 diff & backlog)
└── README.md
```

## 5. Features

1. **Agent Chat**: career Q&A with SSE streaming + typewriter effect (AI via Tbox Open API after A02 migration)
2. **Career Star Map**: visual vertical promotion & horizontal transfer path graph
3. **AI Ability Assessment**: multi-dimension quantitative evaluation
4. **Job-Person Matching**: 10-dimension bidirectional profile matching with gap analysis
5. **Career Report**: exploration → goal → path → action plan, editable & exportable
6. **Workplace Simulation Training** (new, A02): mock interview / cross-role communication / AI-assisted office, results feed back into the student profile
7. **Tutor Dashboard (B-side)**: class/department employment data dashboard
8. **Job Admin (B-side)**: job data maintenance & AI analysis

## 6. Quick Start

### Prerequisites

- JDK 17+, Maven wrapper (bundled), Node 20+, MySQL (import `数据库/数据库结构.sql` into a `youthpath` schema), Redis (optional)
- Python 3 for the service manager

### Configure environment

```bash
cd 后端
cp .env.example .env     # fill in DB / mail / RSA / AES values
```

### Start services (recommended)

```bash
python manage.py start all     # start backend + frontend + nginx
python manage.py status        # check status
python manage.py logs backend  # view logs
python manage.py gui           # visual manager (Tkinter)
```

### Start manually

| Module | Port | Command | Depends on |
|---|---|---|---|
| Backend | 8080 | `cd 后端 && mvnw spring-boot:run` | MySQL (`youthpath`), `.env` |
| Frontend | 5173 | `cd 前端 && npm install && npm run dev` | — |
| Nginx (prod) | 80 | `python manage.py start nginx` | `前端/dist` build |

> ⚠️ Production: build the frontend first (`npm run build` → `前端/dist`), then Nginx serves it and proxies `/api/*` to the backend. See `nginx/README.md`.

## 7. Status & Roadmap

**Done**: user system, student profile, ability assessment, job-person matching, career report, job knowledge base, SSE streaming base chain, full frontend pages.

**In progress (A02 requirements)**:
- Tbox side: knowledge base migration, multi-agent workflow (Router / Coach / Planner / Emotion), scenario scoring nodes
- Backend: switch chat forwarding to Tbox Open API, MCP dual data sources (jobs → career profile; learning resources → student profile), scoring persistence
- Dynamic adjustment (plan rebuild / dynamic scheduling / emotion-triggered fallback)
- Workplace simulation training (mock interview / cross-role / AI-assisted office + profile improvement loop)
- Streaming completeness (heartbeat, reconnect, JSON cleaning, Markdown, streaming charts, persistence)
- AI ethics design, user testing records, multi-platform publishing (optional)

## 8. Disclaimer

- Competition entry; API keys and secrets are redacted — never commit real credentials (use `.env`, which is gitignored)
- Job data is provided by the competition organizer for competition/learning use only
- The project follows the competition's open-source code conventions and AI ethics requirements
