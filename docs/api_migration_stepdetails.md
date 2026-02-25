# API 迁移：`stepDurations` → `stepDetails`

> 本文档描述 NQuest Mod 对 Ranking API 中 `stepDurations` 字段的替换变更，供后端和前端同事对照调整。

---

## 变更概述

| 项目 | 旧版 | 新版 |
|------|------|------|
| 字段名 | `stepDurations` | `stepDetails` |
| 值类型 | `Map<int, long>`（步骤索引 → 毫秒用时） | `Map<int, StepDetail>`（步骤索引 → 详情对象） |
| 影响的 endpoint | `POST /api/completions`（请求体）；所有返回 completion 记录的 endpoint（响应体） | 同左 |

### StepDetail 对象结构

```json
{
  "durationMillis": 120000,
  "description": "Ride Tokyo Express",
  "linesRidden": ["Tokyo Express", "Central Line"]
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `durationMillis` | `long` | 该步骤的有效游玩用时（毫秒），已排除玩家离线时段 |
| `description` | `string` | 步骤的展示文本，由 `Step.getDisplayRepr().getString()` 生成（例如 `"Ride Tokyo Express"`、`"Visit Shibuya Station"`） |
| `linesRidden` | `string[]` | 玩家在完成该步骤前乘坐过的所有线路名称列表，按首次乘坐顺序排列；无乘坐记录时为空数组 `[]` |

---

## 受影响的 Endpoint

### 1. `POST /api/completions`（提交完成记录）

**旧请求体：**
```json
{
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "Steve",
  "questId": "tokyo_express",
  "questName": "Tokyo Express",
  "completionTime": 1708790400000,
  "durationMillis": 342000,
  "questPoints": 50,
  "stepDurations": { "0": 120000, "1": 98000, "2": 124000 }
}
```

**新请求体：**
```json
{
  "playerUuid": "550e8400-e29b-41d4-a716-446655440000",
  "playerName": "Steve",
  "questId": "tokyo_express",
  "questName": "Tokyo Express",
  "completionTime": 1708790400000,
  "durationMillis": 342000,
  "questPoints": 50,
  "stepDetails": {
    "0": {
      "durationMillis": 120000,
      "description": "Ride Tokyo Express",
      "linesRidden": ["Tokyo Express"]
    },
    "1": {
      "durationMillis": 98000,
      "description": "Visit Shibuya Station",
      "linesRidden": ["Tokyo Express", "Yamanote Line"]
    },
    "2": {
      "durationMillis": 124000,
      "description": "Visit Shinjuku Station",
      "linesRidden": ["Yamanote Line"]
    }
  }
}
```

> **注意：** `durationMillis`（顶层）仍然保留，其值等于所有 `stepDetails[*].durationMillis` 之和。该值现在排除了玩家离线时间。

### 2. `GET /api/leaderboards/speedrun/{questId}`（速通排行榜）

响应中每个 entry 的 `stepDurations` 应替换为 `stepDetails`：

```json
{
  "entries": [
    {
      "rank": 1,
      "playerUuid": "...",
      "playerName": "Steve",
      "durationMillis": 185000,
      "completionTime": 1708790400000,
      "completionId": 1234,
      "isWorldRecord": true,
      "stepDetails": {
        "0": { "durationMillis": 60000, "description": "Ride Tokyo Express", "linesRidden": ["Tokyo Express"] },
        "1": { "durationMillis": 55000, "description": "Visit Shibuya Station", "linesRidden": [] },
        "2": { "durationMillis": 70000, "description": "Visit Shinjuku Station", "linesRidden": ["Yamanote Line"] }
      }
    }
  ],
  "total": 42,
  "quest": { "id": "tokyo_express", "name": "Tokyo Express" }
}
```

### 3. `GET /api/players/{uuid}/history`（玩家历史记录）

同上，每个 entry 中的 `stepDurations` 替换为 `stepDetails`。

### 4. `GET /api/quests/{questId}/stats`（任务统计）

`stepAnalytics` 数组可考虑扩展，增加 `description` 字段：

```json
{
  "stepAnalytics": [
    {
      "stepIndex": 0,
      "description": "Ride Tokyo Express",
      "avgDurationMillis": 100000,
      "medianDurationMillis": 95000
    }
  ]
}
```

---

## 后端迁移指引

### 数据库 Schema 变更

```sql
-- 方案 A：重命名列（推荐，保持 JSONB 类型）
ALTER TABLE quest_completions RENAME COLUMN step_durations TO step_details;

-- 方案 B：新增列 + 迁移
ALTER TABLE quest_completions ADD COLUMN step_details JSONB;
UPDATE quest_completions SET step_details = (
    SELECT jsonb_object_agg(
        key,
        jsonb_build_object('durationMillis', value::bigint, 'description', null, 'linesRidden', '[]'::jsonb)
    )
    FROM jsonb_each_text(step_durations)
) WHERE step_durations IS NOT NULL;
ALTER TABLE quest_completions DROP COLUMN step_durations;
```

### 接收逻辑

- 解析请求体时，将 `stepDetails` 按 `Map<String, StepDetail>` 反序列化
- 存储时直接以 JSONB 写入 `step_details` 列
- 计算 `stepAnalytics` 统计时，从 `step_details -> '0' -> 'durationMillis'` 读取用时

### 向后兼容

Mod 端在读取后端返回数据时，兼容旧格式：
- 如果响应中包含 `stepDetails`，直接使用
- 如果只有 `stepDurations`（旧数据），自动转换为 `StepDetail`（`description = null`，`linesRidden = []`）

后端在迁移期间也可做类似兼容：
- 如果请求体中包含 `stepDetails`，使用新格式
- 如果只有 `stepDurations`（旧版 Mod），按旧逻辑处理或转换

---

## 前端迁移指引

### 展示建议

- **步骤名称**：使用 `stepDetails[i].description` 作为步骤标题，无需再从 quest 定义中查找
- **步骤用时**：使用 `stepDetails[i].durationMillis`
- **乘坐线路**：使用 `stepDetails[i].linesRidden` 数组展示玩家在该步骤中乘坐的线路，可作为标签或列表
- **旧数据兼容**：如果 `description` 为 `null`，回退到显示 `"Step {i+1}"`

---

## 其他行为变更

### `durationMillis` 语义变更

顶层 `durationMillis` 现在表示**有效游玩时间**（排除离线时段），不再是 `completionTime - startTime` 的简单差值。等于所有步骤 `durationMillis` 之和。

这意味着速通排行榜的排名将更加公正，不会因为玩家中途下线而产生时间惩罚。
