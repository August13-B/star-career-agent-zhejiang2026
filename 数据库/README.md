# 数据库灌库说明

## 文件说明

| 文件 | 内容 | 用途 |
|---|---|---|
| `数据库结构.sql` | **31 张表结构**（含 `CREATE DATABASE` + `USE`） | 一键建库建表 |
| `后端/sql/invitation_code_init.sql` | 邀请码表 + 3 条角色邀请码种子数据 | 初始化邀请码（可选） |

## 连接信息（与 `后端/.env` 对应）

| 项 | 值 |
|---|---|
| 主机 | `localhost:3306` |
| 库名 | `youthpath` |
| 字符集 | `utf8mb4` / `utf8mb4_0900_ai_ci` |
| 账号 | 见 `后端/.env` 的 `DB_USERNAME` / `DB_PASSWORD` |

## 灌库步骤

### 方式 1：命令行（推荐）

```bash
# 已在脚本内置 CREATE DATABASE + USE，无需手动选库
mysql -u root -p < 数据库/数据库结构.sql

# （可选）导入邀请码种子数据
mysql -u root -p youthpath < 后端/sql/invitation_code_init.sql
```

Windows 下 MySQL 客户端示例：
```bat
"D:\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p < "数据库\数据库结构.sql"
```

### 方式 2：Navicat / GUI 工具

1. 新建连接 → 主机 `localhost`、端口 `3306`、账号密码见 `.env`
2. 运行 SQL 文件 → 选择 `数据库/数据库结构.sql` → 执行

## ⚠️ 重要提醒

1. **脚本开头是 `DROP TABLE IF EXISTS`** —— 执行会**清空并重建**所有 31 张表，**现有数据会丢失**。
   仅在首次建库或确认可清空数据时执行；生产/演示库请先备份。
2. **本脚本仅含表结构，不含业务数据**。
   真实库中的业务数据（岗位 `job_info` 约 1 万条、岗位画像 `job_requirement_profile`、用户与画像等）**未包含在仓库中**，需另行导出后导入。
   导出命令参考：
   ```bash
   mysqldump -u root -p --no-create-info --complete-insert youthpath > data.sql
   ```

## 灌库后自检

```sql
USE youthpath;
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='youthpath';  -- 应为 31
SELECT COUNT(*) FROM youthpath.user;                 -- 用户数
SELECT COUNT(*) FROM youthpath.job_info;             -- 岗位数（需导入数据后才有值）
SELECT id, user_role, invitation_code FROM youthpath.invitation_code;  -- 邀请码
```

## 邀请码说明

| user_role | 角色 |
|---|---|
| 2 | 管理员 |
| 3 | 企业端 |
| 4 | 导师 |

`后端/sql/` 下另有 `invitation_code_table.sql`（仅建表，已被主结构覆盖）、`invitation_code_full.sql`（建表 + 冲突时重建），按需使用。
