# NQuest Ranking API 契约

> MC 服务器 Mod ↔ Rust/Axum 后端之间的 Ranking 相关 REST API。
> 所有请求需携带 `X-API-Key` header（值同 `sync_config.json` 中的 `apiKey`）。
> Base path: `{backendUrl}/api`

---

## 数据模型

### 后端数据库 Schema

```sql
CREATE TABLE quest_completions (
    id              BIGSERIAL PRIMARY KEY,
    player_uuid     TEXT NOT NULL,
    player_name     TEXT NOT NULL,
    quest_id        TEXT NOT NULL,
    quest_name      TEXT NOT NULL,
    completion_time BIGINT NOT NULL,
    duration_millis BIGINT NOT NULL,
    quest_points    INTEGER NOT NULL,
    step_durations  JSONB,
    created_at      BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

CREATE TABLE qp_transactions (
    id                BIGSERIAL PRIMARY KEY,
    player_uuid       TEXT NOT NULL,
    transaction_type  TEXT NOT NULL CHECK (transaction_type IN
        ('QUEST_COMPLETION', 'QP_ADJUSTMENT', 'SPEND', 'ADMIN_GRANT', 'ADMIN_DEDUCT')),
    amount            INTEGER NOT NULL,
    completion_id     BIGINT REFERENCES quest_completions(id),
    quest_id          TEXT,
    description       TEXT,
    created_at        BIGINT NOT NULL DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

CREATE TABLE player_balances (
    player_uuid             TEXT PRIMARY KEY,
    player_name             TEXT NOT NULL,
    qp_balance              INTEGER NOT NULL DEFAULT 0,
    total_quest_completions INTEGER NOT NULL DEFAULT 0,
    last_updated_at         BIGINT NOT NULL
);
```

### Derived Fields

- `qpBalance` = `SUM(qp_transactions.amount)` for that player
- `totalQpEarned` = `SUM(amount) WHERE transaction_type IN ('QUEST_COMPLETION', 'QP_ADJUSTMENT')`
  - Decreases when a QP_ADJUSTMENT with negative amount is applied
- `totalQpSpent` = `SUM(ABS(amount)) WHERE transaction_type = 'SPEND'`

### Transaction Types

| Type | Trigger | Amount | Description |
|------|---------|--------|-------------|
| `QUEST_COMPLETION` | Player completes a quest | `+questPoints` | `"Completed: {questName}"` |
| `QP_ADJUSTMENT` | Admin adjusts quest QP retroactively | `+/- delta per completion` | `"Quest QP adjusted {old}->{new}"` |
| `SPEND` | Player redeems QP | `-cost` | `"Redeemed: {item}"` |
| `ADMIN_GRANT` | Admin manually grants QP | `+amount` | Admin-specified reason |
| `ADMIN_DEDUCT` | Admin manually deducts QP | `-amount` | Admin-specified reason |

---

## Endpoints

### 1. Submit Quest Completion

`POST /api/completions`

Called by the Mod when a player completes a quest (non-debug mode).

**Request:**
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

**Response (201 Created):**
```json
{
  "completionId": 1234,
  "isPersonalBest": true,
  "isWorldRecord": false,
  "rank": 3,
  "updatedStats": {
    "qpBalance": 450,
    "totalQuestCompletions": 12
  }
}
```

**Backend logic:**
1. INSERT into `quest_completions`
2. INSERT into `qp_transactions` (type=QUEST_COMPLETION, amount=+questPoints)
3. UPDATE `player_balances` (balance += questPoints, completions += 1)
4. Determine PB/WR status
5. Return updated stats

---

### 2. QP Leaderboard

`GET /api/leaderboards/qp`

Ranks players by `qp_balance` (current available balance, affected by spending).

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `period` | `all_time` / `monthly` / `weekly` | `all_time` | Time range |
| `limit` | int | 50 | Max entries |
| `offset` | int | 0 | Pagination offset |

**Response:**
```json
{
  "entries": [
    { "rank": 1, "playerUuid": "...", "playerName": "Steve", "value": 1200 }
  ],
  "total": 150,
  "period": "all_time"
}
```

**Note:** For `monthly`/`weekly`, the backend computes balance from transactions within the time window, not from the cached `player_balances` table.

---

### 3. Completions Leaderboard

`GET /api/leaderboards/completions`

Parameters and response format identical to QP leaderboard. `value` = number of completions.

---

### 4. Speedrun Leaderboard

`GET /api/leaderboards/speedrun/{questId}`

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `period` | `all_time` / `monthly` / `weekly` | `all_time` | Time range |
| `mode` | `personal_best` / `all_runs` | `personal_best` | PB: one entry per player (best time). All: every completion. |
| `limit` | int | 50 | Max entries |
| `offset` | int | 0 | Pagination offset |

**Response:**
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
      "stepDurations": { "0": 60000, "1": 55000, "2": 70000 }
    }
  ],
  "total": 42,
  "quest": { "id": "tokyo_express", "name": "Tokyo Express" }
}
```

---

### 5. Player Profile

`GET /api/players/{uuid}/profile`

**Response:**
```json
{
  "playerUuid": "...",
  "playerName": "Steve",
  "qpBalance": 450,
  "totalQpEarned": 600,
  "totalQpSpent": 150,
  "totalQuestCompletions": 12,
  "personalBestCount": 3,
  "worldRecordCount": 1,
  "firstCompletionTime": 1700000000000,
  "recentActivity": [
    {
      "questId": "tokyo_express",
      "questName": "Tokyo Express",
      "durationMillis": 185000,
      "completionTime": 1708790400000,
      "isPersonalBest": true
    }
  ]
}
```

---

### 6. Player Quest History

`GET /api/players/{uuid}/history`

| Param | Type | Default |
|-------|------|---------|
| `limit` | int | 20 |
| `offset` | int | 0 |

**Response:**
```json
{
  "entries": [
    {
      "completionId": 1234,
      "questId": "tokyo_express",
      "questName": "Tokyo Express",
      "completionTime": 1708790400000,
      "durationMillis": 185000,
      "questPoints": 50,
      "stepDurations": { "0": 60000, "1": 55000, "2": 70000 },
      "isPersonalBest": true
    }
  ],
  "total": 12
}
```

---

### 7. Player Personal Bests

`GET /api/players/{uuid}/personal-bests`

Best time per quest for this player.

**Response:**
```json
{
  "entries": [
    {
      "questId": "tokyo_express",
      "questName": "Tokyo Express",
      "durationMillis": 185000,
      "completionTime": 1708790400000,
      "rank": 3
    }
  ]
}
```

---

### 8. Player Transactions

`GET /api/players/{uuid}/transactions`

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `type` | string | (all) | Filter by transaction_type |
| `limit` | int | 20 | |
| `offset` | int | 0 | |

**Response:**
```json
{
  "entries": [
    {
      "id": 5678,
      "type": "SPEND",
      "amount": -100,
      "description": "Redeemed: Diamond Pickaxe Skin",
      "questId": null,
      "completionId": null,
      "createdAt": 1708800000000
    }
  ],
  "total": 25
}
```

---

### 9. Spend QP

`POST /api/players/{uuid}/spend`

**Request:**
```json
{
  "amount": 100,
  "description": "Redeemed: Diamond Pickaxe Skin"
}
```

**Response (200):**
```json
{
  "transactionId": 5678,
  "newBalance": 350
}
```

**Error (409 - Insufficient Balance):**
```json
{
  "error": "INSUFFICIENT_BALANCE",
  "message": "Need 100 QP but only 50 available"
}
```

---

### 10. Quest Statistics (Web)

`GET /api/quests/{questId}/stats`

**Response:**
```json
{
  "questId": "tokyo_express",
  "questName": "Tokyo Express",
  "totalRuns": 234,
  "uniqueRunners": 87,
  "averageDurationMillis": 360000,
  "medianDurationMillis": 340000,
  "worldRecord": {
    "playerUuid": "...",
    "playerName": "Steve",
    "durationMillis": 185000,
    "completionTime": 1708790400000
  },
  "stepAnalytics": [
    { "stepIndex": 0, "avgDurationMillis": 100000, "medianDurationMillis": 95000 }
  ]
}
```

---

### 11. Recent Activity (Web)

`GET /api/activity/recent`

| Param | Type | Default |
|-------|------|---------|
| `limit` | int | 20 |

**Response:**
```json
{
  "entries": [
    {
      "playerUuid": "...",
      "playerName": "Steve",
      "questId": "tokyo_express",
      "questName": "Tokyo Express",
      "durationMillis": 185000,
      "completionTime": 1708790400000,
      "questPoints": 50,
      "isPersonalBest": true,
      "isWorldRecord": false
    }
  ]
}
```

---

### 12. Admin: Retroactive QP Adjustment

`POST /api/admin/quests/{questId}/adjust-qp`

Adjusts QP for all existing completions of the specified quest. Runs as an async job.

**Request:**
```json
{
  "newQuestPoints": 80,
  "reason": "Increased difficulty reward"
}
```

**Response (202 Accepted):**
```json
{
  "jobId": "abc-123",
  "affectedCompletions": 42,
  "qpDeltaPerCompletion": 30,
  "status": "PROCESSING"
}
```

**Backend async job:**
1. Find all `quest_completions` with `quest_id = {questId}`
2. Compute delta = `newQuestPoints - completion.quest_points`
3. For each completion: INSERT `qp_transactions(QP_ADJUSTMENT, delta)`
4. Recalculate `player_balances` for all affected players
5. Update quest definition's `questPoints`
6. Update `sync_metadata.last_modified`

---

### 13. Admin: Job Status

`GET /api/admin/jobs/{jobId}`

**Response:**
```json
{
  "jobId": "abc-123",
  "type": "QP_ADJUSTMENT",
  "status": "COMPLETED",
  "progress": { "processed": 42, "total": 42 },
  "createdAt": 1708800000000,
  "completedAt": 1708800005000
}
```

---

### 14. Bulk Import (Migration)

USER: No Need.

---

## Error Response Format

All error responses use:
```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable description"
}
```

| HTTP | Error Code | When |
|------|------------|------|
| 400 | `INVALID_REQUEST` | Malformed request |
| 401 | `UNAUTHORIZED` | Missing/invalid API key |
| 404 | `NOT_FOUND` | Player/quest/job not found |
| 409 | `INSUFFICIENT_BALANCE` | QP spend exceeds balance |
| 500 | `INTERNAL_ERROR` | Server error |
