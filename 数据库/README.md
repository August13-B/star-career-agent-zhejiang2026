# 数据库说明（灌库 / 数据 / 向量）

## 目录内容

| 路径 | 内容 | 大小 |
|---|---|---|
| `数据库结构.sql` | **31 张表结构**（含 `CREATE DATABASE` + `USE`，一键建库） | 69 KB |
| `数据库数据.sql` | **仅职业相关数据**（岗位 9958 条 + 岗位画像 181 条 + 岗位要求/图谱 + 邀请码）<br>**不含**用户账号、学生画像/能力、对话、匹配、报告等用户数据 | 22 MB |
| `数据库数据-全量备份.sql` | 完整版导出（含用户/画像/对话等用户数据）<br>⚠️ **不入 Git、不参与灌库**，仅本地保留 | 24 MB |
| `向量数据/` | **岗位知识库向量**（10139 条 × 1024 维，gzip 约 60MB） | 60 MB |
| `../后端/sql/` | 邀请码相关脚本 | — |

## 连接信息（与 `后端/.env` 对应）

| 项 | 值 |
|---|---|
| 主机 | `localhost:3306` |
| 库名 | `youthpath` |
| 字符集 | `utf8mb4` |
| 账号 | 见 `后端/.env` 的 `DB_USERNAME` / `DB_PASSWORD` |

## 方式 1：自动灌库（推荐，随项目启动）

`manage.py` 已内置**幂等自动灌库**：启动后端前自动检查——缺表则建表、无业务数据则导入；已有数据则跳过。

```bash
python manage.py start backend     # 启动时自动灌库
python manage.py db                # 手动灌库（同样幂等）
python manage.py db --force        # 强制重建表并重新导入（会清空现有数据）
python manage.py db migrate        # 对已有库执行幂等迁移
python manage.py db status         # 查看库/表/岗位数据量
```

> 🔒 **灌库内容**：只导入**职业相关数据**（`job_info` / `job_requirement_profile` / `job_*` / `invitation_code`）。
> `user` / `student_profile` / `student_ability(_score)` / `ai_*` / `match_*` / `career_report*` 等用户数据**不会**被导入，
> 灌库后用户表为空，需重新注册。完整数据备份见 `数据库数据-全量备份.sql`（不参与灌库）。

> 灌库逻辑读取 `后端/.env` 的 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`，自动定位 mysql 客户端（PATH 或常见安装目录）。

## 方式 2：手动命令行

```bash
# 建库建表（脚本内含 CREATE DATABASE + USE，无需手动选库）
mysql -u root -p --default-character-set=utf8mb4 < 数据库/数据库结构.sql

# 导入业务数据
mysql -u root -p --default-character-set=utf8mb4 < 数据库/数据库数据.sql

# （可选）邀请码种子数据
mysql -u root -p --default-character-set=utf8mb4 youthpath < 后端/sql/invitation_code_init.sql
```

Windows（MySQL 装在 D 盘时）：
```bat
"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 < "数据库\数据库结构.sql"
"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 < "数据库\数据库数据.sql"
```

> ⚠️ **`--default-character-set=utf8mb4` 必须带**：数据含中文与 `\"` 转义，缺失该参数会导致 `ERROR: Unknown command '\"'` 导入失败。

## ⚠️ 重要提醒

1. **两个脚本都含 `DROP TABLE IF EXISTS`** —— 执行会**清空并重建**表，现有数据会丢失。
   仅在首次建库或确认可清空时执行；生产/演示库请先备份。
2. 自动灌库是**幂等**的：检测到 `job_info` 已有数据就跳过，故日常启动不会重复导入。

## 迁移脚本

| 脚本 | 说明 |
|---|---|
| `migrations/001_add_tbox_ids.sql` | 百宝箱对接：`ai_conversation` / `ai_message` 增加 tbox 映射字段 |
| `migrations/002_clean_user_data.sql` | **清理旧密钥用户数据**（保留 `job_info` / `job_requirement_profile` / `invitation_code` / `user` 账号）<br>背景：早期画像由另一套密钥加密，无法解密；清理后重新录入即为当前密钥 |
| `migrations/003_widen_encrypted_columns.sql` | 加密列加宽（`expected_salary` 等 → `varchar(1000)`） |
| `migrations/004_fix_null_is_deleted.sql` | 修复 `is_deleted` 为 NULL 的旧行（**只更新确实有该列的表**，早期版本误写会整脚本中断） |
| `migrations/005_fix_profile_schema.sql` | **幂等**：加宽加密列 + `is_deleted` 收敛为 `NOT NULL DEFAULT 0`；`manage.py` 灌库后自动执行 |

执行（已有库，不会丢数据）：
```bash
python manage.py db migrate    # 依次执行 003 / 004 / 005
# 或手动：
mysql -u root -p --default-character-set=utf8mb4 < 数据库/migrations/005_fix_profile_schema.sql
```

> ℹ️ **结构漂移提醒**：`数据库结构.sql` 已同步 003/005 的列宽与 `is_deleted` 默认值。
> 自 2026-09-19 起，`数据库数据.sql` 已剔除全部用户数据（含早期「旧密钥」画像行），
> 新环境灌库后不会再有 `bad key` / `保存后查不到` 等遗留数据问题；`002_clean_user_data.sql` 仅在对旧库时才需要。

> ⚠️ 执行 002 前请先备份：`mysqldump -u root -p --single-transaction youthpath > backup.sql`

## 灌库后自检

```sql
USE youthpath;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='youthpath';  -- 31
SELECT COUNT(*) FROM youthpath.job_info;                  -- 9958
SELECT COUNT(*) FROM youthpath.job_requirement_profile;   -- 181
SELECT COUNT(*) FROM youthpath.user;                      -- 0（不再随灌库导入，需注册）
SELECT COUNT(*) FROM youthpath.student_profile;           -- 0（不再随灌库导入）
SELECT COUNT(*) FROM youthpath.invitation_code;           -- 3（管理员/企业端/导师）
```

## 邀请码

| user_role | 角色 |
|---|---|
| 2 | 管理员 |
| 3 | 企业端 |
| 4 | 导师 |

## 向量数据

见 [`向量数据/README.md`](./向量数据/README.md) —— 当前为**预留**状态，后续上传蚂蚁百宝箱企业版知识库。
