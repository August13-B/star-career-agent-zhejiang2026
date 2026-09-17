# 向量数据（岗位知识库向量库）

## 文件

| 文件 | 说明 |
|---|---|
| `岗位知识库向量_10139条.json.gz` | 岗位知识库向量数据（压缩包，约 60MB） |
| `样例_前3条.json` | 前 3 条样例（查看格式用，无需解压） |

## 数据规格

| 项 | 值 |
|---|---|
| 条目数 | **10139** |
| 向量维度 | **1024** |
| 生成模型 | text-embedding-v4（阿里云 DashScope） |
| 来源 | 国赛 A13 岗位知识库向量化结果（对应 `job_info` 约 1 万条岗位） |
| 原始大小 | 147 MB（解压后） |

## 格式

```json
{
  "embedding_knowledge_base": [
    {
      "embedding_id": "01fb67ba-7d94-4205-9c4d-5ed9fa404e5f",
      "embedding": "[-0.012260714, -0.031327754, ...]"
    }
  ]
}
```

> `embedding` 为**字符串形式的数组**（非 JSON 数组），使用时需先解析：
> ```python
> import json, gzip
> with gzip.open('岗位知识库向量_10139条.json.gz', 'rt', encoding='utf-8') as f:
>     items = json.load(f)['embedding_knowledge_base']
> vec = json.loads(items[0]['embedding'])   # -> list[float], 长度 1024
> ```

## 用途与后续规划（预留）

本目录数据当前**暂不参与运行时**，为以下目标预留：

1. **上传百宝箱企业版知识库**（A02 方案 B：知识库 + 多智能体工作流均在百宝箱侧）
   - 对应 issue：`1-5 知识库材料清点并上传百宝箱知识库`
2. 如需保留自研向量检索（PGVector / 本地向量库），可直接解压后导入

> ⚠️ 注意：`embedding_id` 与岗位数据的对应关系需结合 `job_info` 表（`id` 字段）确认；若后续需要「文本 + 向量」联合存储，需补充各条向量对应的文本内容。

## 解压

```bash
# Linux / WSL
gunzip -k 岗位知识库向量_10139条.json.gz

# Windows PowerShell
tar -xzf 岗位知识库向量_10139条.json.gz
```
