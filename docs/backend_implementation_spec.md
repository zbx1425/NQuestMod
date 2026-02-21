# NQuest 后端实现规格文档

> 面向后端开发者的完整实现参考。包含所有数据模型、API 端点、业务逻辑、鉴权规则。

---

## 一、技术栈建议

| 层 | 推荐选型 | 说明 |
|----|---------|------|
| 框架 | Node.js + Fastify 或 Python + FastAPI | 轻量 CRUD + OAuth 场景 |
| 数据库 | PostgreSQL 15+ | JSONB 列原生支持 criteria 树存储 |
| 认证 | Discord OAuth2 → JWT | `discord-oauth2` (Node) 或 `authlib` (Python) |
| 部署 | Docker + Nginx 反代 | 标准容器化部署 |

---

## 二、配置项

后端需要以下环境变量 / 配置：

```
# Discord OAuth2
DISCORD_CLIENT_ID=...
DISCORD_CLIENT_SECRET=...
DISCORD_REDIRECT_URI=https://nquest-api.example.com/auth/discord/callback
DISCORD_GUILD_ID=...               # 用于查询成员角色的目标 Guild

# 角色映射（Discord Role ID → 应用角色）
DISCORD_ADMIN_ROLE_ID=...          # 拥有此 role 的成员为 ADMIN
DISCORD_AUTHOR_ROLE_ID=...         # 拥有此 role 的成员为 AUTHOR

# MC 服务器同步认证
SYNC_API_KEY=...                   # MC 服务器在 X-API-Key header 中发送的静态密钥

# JWT
JWT_SECRET=...                     # HMAC-SHA256 签名密钥
JWT_EXPIRES_IN=86400               # JWT 有效期（秒），默认 24 小时

# 数据库
DATABASE_URL=postgresql://user:pass@localhost:5432/nquest
```

---

## 三、数据库 Schema

### 3.1 `quests` 表

```sql
CREATE TABLE quests (
    id                TEXT PRIMARY KEY,         -- 用户自定义 slug，如 "mtr-central-line-tour"
    name              TEXT NOT NULL,
    description       TEXT,
    category          TEXT REFERENCES categories(id) ON DELETE SET NULL,
    tier              TEXT,
    quest_points      INTEGER NOT NULL DEFAULT 0,
    status            TEXT NOT NULL DEFAULT 'DRAFT'
                      CHECK (status IN ('DRAFT', 'PUBLISHED')),
    steps             JSONB NOT NULL DEFAULT '[]',
    default_criteria  JSONB,                    -- nullable，整个 Step 对象
    created_by        TEXT NOT NULL,            -- Discord User ID
    created_at        BIGINT NOT NULL,          -- epoch milliseconds
    last_modified_by  TEXT NOT NULL,            -- Discord User ID
    last_modified_at  BIGINT NOT NULL           -- epoch milliseconds
);
```

### 3.2 `quest_acl` 表

```sql
CREATE TABLE quest_acl (
    quest_id          TEXT NOT NULL REFERENCES quests(id) ON DELETE CASCADE,
    discord_user_id   TEXT NOT NULL,
    discord_username  TEXT,                     -- 冗余展示字段，不作鉴权依据
    role              TEXT NOT NULL CHECK (role IN ('OWNER', 'EDITOR')),
    PRIMARY KEY (quest_id, discord_user_id)
);
```

### 3.3 `categories` 表

```sql
CREATE TABLE categories (
    id                TEXT PRIMARY KEY,         -- 如 "mtr-lines"
    data              JSONB NOT NULL,           -- 完整 QuestCategory 对象（见下文）
    last_modified_at  BIGINT NOT NULL           -- epoch milliseconds
);
```

`data` 列的 JSON 结构：

```json
{
    "name": "MTR Lines",
    "description": "Quests about MTR lines",
    "icon": "minecraft:rail",
    "order": 0,
    "tiers": {
        "easy": { "name": "Easy", "icon": "minecraft:gold_nugget", "order": 0 },
        "hard": { "name": "Hard", "icon": "minecraft:gold_ingot", "order": 1 }
    }
}
```

### 3.4 `sync_metadata` 表

```sql
CREATE TABLE sync_metadata (
    key               TEXT PRIMARY KEY,
    value             TEXT NOT NULL
);

-- 初始化时插入
INSERT INTO sync_metadata (key, value) VALUES ('last_modified', '0');
```

`last_modified` 记录任何影响同步内容（PUBLISHED Quest 或 Category）的变更时间戳。

### 3.5 索引

```sql
CREATE INDEX idx_quests_status ON quests(status);
CREATE INDEX idx_quests_category ON quests(category);
CREATE INDEX idx_quests_last_modified ON quests(last_modified_at);
CREATE INDEX idx_quest_acl_user ON quest_acl(discord_user_id);
```

---

## 四、数据模型（TypeScript 类型定义参考）

### 4.1 Quest（完整后端模型）

```typescript
interface Quest {
    id: string;
    name: string;
    description: string;
    category: string | null;
    tier: string | null;
    questPoints: number;
    status: 'DRAFT' | 'PUBLISHED';
    steps: Step[];
    defaultCriteria: Step | null;
    createdBy: UserRef;
    createdAt: number;
    lastModifiedBy: UserRef;
    lastModifiedAt: number;
    acl: AclEntry[];
}

interface UserRef {
    discordUserId: string;
    username: string;
}
```

### 4.2 Quest（同步 bundle 中的精简模型）

同步给 MC 服务器时，**剥离元数据字段**，只保留游戏逻辑需要的字段：

```typescript
interface QuestSync {
    id: string;
    name: string;
    description: string;
    category: string | null;
    tier: string | null;
    questPoints: number;
    status: 'PUBLISHED';       // bundle 中只有 PUBLISHED
    steps: Step[];
    defaultCriteria: Step | null;
}
```

### 4.3 Step

```typescript
interface Step {
    criteria: Criterion;           // 完成条件（多态 JSON，由 "type" 字段区分）
    failureCriteria?: Criterion;   // 步骤级失败条件（可选）
}
```

### 4.4 Criterion（多态 JSON）

每个 Criterion 对象包含 `"type"` 字段标识具体类型。后端**不需要理解 Criterion 的内部结构**，将其作为不透明 JSON 存储。

已知类型（供前端校验参考）：

| type | 说明 | 关键字段 |
|------|------|---------|
| `ConstantCriterion` | 常量 | `value: boolean` |
| `ManualTriggerCriterion` | 手动触发 | `id: string, description: string` |
| `InBoundsCriterion` | AABB 区域 | `min: Vec3d, max: Vec3d` |
| `OverSpeedCriterion` | 速度阈值 | `speedThreshold: number` |
| `TeleportDetectCriterion` | 传送检测 | 无 |
| `VisitStationCriterion` | 到达车站 | `stationName: string` |
| `RideLineCriterion` | 乘坐线路 | `lineName: string` |
| `RideToStationCriterion` | 乘车到站（语法糖） | `stationName: string` |
| `RideLineToStationCriterion` | 乘指定线路到站（语法糖） | `lineName: string, stationName: string` |
| `AndCriterion` | 所有子条件满足 | `criteria: Criterion[]` |
| `OrCriterion` | 任一子条件满足 | `criteria: Criterion[]` |
| `NotCriterion` | 取反 | `base: Criterion` |
| `LatchingCriterion` | 一旦满足则锁定 | `base: Criterion` |
| `RisingEdgeAndConditionCriterion` | 上升沿 + 条件 | `trigger: Criterion, condition: Criterion` |
| `Descriptor` | 添加自定义描述 | `description: string, base: Criterion` |

### 4.5 QuestCategory

```typescript
interface QuestCategory {
    name: string;
    description: string;
    icon: string;            // Minecraft ResourceLocation 格式，如 "minecraft:book"
    order: number;
    tiers: Record<string, QuestTier>;
}

interface QuestTier {
    name: string;
    icon: string;
    order: number;
}
```

### 4.6 AclEntry

```typescript
interface AclEntry {
    discordUserId: string;
    discordUsername: string;
    role: 'OWNER' | 'EDITOR';
}
```

---

## 五、认证与鉴权

### 5.1 Discord OAuth2 流程

**Scope 要求：** `identify`, `guilds.members.read`

```
1. 用户访问 GET /auth/discord
2. 后端生成 state 参数（防 CSRF），重定向到：
   https://discord.com/api/oauth2/authorize
     ?client_id={DISCORD_CLIENT_ID}
     &redirect_uri={DISCORD_REDIRECT_URI}
     &response_type=code
     &scope=identify%20guilds.members.read
     &state={random_state}

3. 用户授权后，Discord 回调 GET /auth/discord/callback?code=xxx&state=xxx

4. 后端用 code 换取 access_token：
   POST https://discord.com/api/v10/oauth2/token
   Content-Type: application/x-www-form-urlencoded
   Body: client_id, client_secret, grant_type=authorization_code, code, redirect_uri

5. 用 access_token 获取用户身份：
   GET https://discord.com/api/v10/users/@me
   Authorization: Bearer {access_token}
   → { id, username, discriminator, avatar, ... }

6. 用 Bot Token（不是用户 token）查询该用户在 Guild 中的角色：
   GET https://discord.com/api/v10/guilds/{guild_id}/members/{user_id}
   Authorization: Bot {bot_token}
   → { roles: ["role_id_1", "role_id_2", ...], ... }
   注意：需要 Bot 在 Guild 中且有 GUILD_MEMBERS intent

7. 映射角色：
   if roles contains DISCORD_ADMIN_ROLE_ID → appRoles.push("ADMIN")
   if roles contains DISCORD_AUTHOR_ROLE_ID → appRoles.push("AUTHOR")
   （ADMIN 隐含 AUTHOR 权限）

8. 签发 JWT：
   {
     "sub": "discord_user_id",
     "username": "display_name",
     "roles": ["AUTHOR"],     // 或 ["ADMIN"] 或 ["ADMIN", "AUTHOR"]
     "guildId": "...",
     "iat": ...,
     "exp": ...
   }

9. 返回 JWT 给前端（Set-Cookie 或 JSON body）
```

### 5.2 两种认证方式

| 场景 | 认证方式 | Header |
|------|---------|--------|
| Webapp 请求 | JWT Bearer Token | `Authorization: Bearer <jwt>` |
| MC 服务器同步 | 静态 API Key | `X-API-Key: <key>` |

### 5.3 中间件逻辑

```
function authenticateJwt(req):
    token = req.headers.authorization?.replace("Bearer ", "")
    if !token: return 401
    try:
        payload = jwt.verify(token, JWT_SECRET)
        req.user = { discordUserId: payload.sub, username: payload.username, roles: payload.roles }
    catch:
        return 401

function authenticateApiKey(req):
    key = req.headers["x-api-key"]
    if key != SYNC_API_KEY: return 401

function requireRole(...requiredRoles):
    return (req) =>
        if !req.user: return 401
        if !requiredRoles.some(r => req.user.roles.includes(r)): return 403
```

### 5.4 权限检查辅助函数

```
function canEditQuest(user, quest, acl):
    if "ADMIN" in user.roles: return true
    return acl.some(e => e.discordUserId == user.discordUserId && e.role in ["OWNER", "EDITOR"])

function isQuestOwner(user, quest, acl):
    if "ADMIN" in user.roles: return true
    return acl.some(e => e.discordUserId == user.discordUserId && e.role == "OWNER")

function canViewQuest(user, quest, acl):
    if quest.status == "PUBLISHED": return true
    if "ADMIN" in user.roles: return true
    return acl.some(e => e.discordUserId == user.discordUserId)
```

---

## 六、API 端点详细规格

**Base URL:** `/api/v1`

### 6.1 认证端点

#### `GET /auth/discord`

重定向到 Discord OAuth2 授权页。

- **无认证要求**
- **Query 参数：** 可选 `redirect` — 授权完成后前端跳转地址
- **响应：** `302 Redirect` → Discord OAuth URL
- **实现：** 生成随机 `state`，存入 session/cookie，重定向

#### `GET /auth/discord/callback`

Discord OAuth2 回调。

- **无认证要求**
- **Query 参数：** `code`, `state`
- **响应：** 返回 JWT（方式取决于前端需求，可以是 JSON body 或 redirect + cookie）
- **实现：**
  1. 验证 `state` 参数（防 CSRF）
  2. 用 `code` 换 access_token
  3. 获取用户信息 + Guild 角色
  4. 签发 JWT
  5. 返回

#### `GET /auth/me`

返回当前登录用户信息。

- **认证：** JWT Bearer
- **响应：**

```json
{
    "discordUserId": "123456789",
    "username": "PlayerOne",
    "avatar": "https://cdn.discordapp.com/avatars/...",
    "roles": ["AUTHOR"]
}
```

---

### 6.2 Quest CRUD 端点

#### `GET /quests`

列出 Quest。

- **认证：** JWT Bearer（可选 — 未认证只返回 PUBLISHED）
- **Query 参数：**
  - `status` — `DRAFT` | `PUBLISHED`（可选，未认证时强制为 PUBLISHED）
  - `category` — 按分类过滤（可选）
  - `page` — 页码，默认 1
  - `size` — 每页数量，默认 20，最大 100
- **响应：**

```json
{
    "items": [
        {
            "id": "mtr-central-line",
            "name": "Central Line Tour",
            "description": "Visit all stations on the Central Line",
            "category": "mtr-lines",
            "tier": "easy",
            "questPoints": 10,
            "status": "PUBLISHED",
            "createdBy": { "discordUserId": "123", "username": "Author1" },
            "lastModifiedAt": 1708000000000,
            "acl": [
                { "discordUserId": "123", "discordUsername": "Author1", "role": "OWNER" }
            ]
        }
    ],
    "total": 42,
    "page": 1,
    "size": 20
}
```

- **过滤逻辑（重要）：**
  - 未登录 → 只返回 `status = 'PUBLISHED'`
  - AUTHOR → 返回所有 PUBLISHED + 自己在 ACL 中的 DRAFT
  - ADMIN → 返回所有

```sql
-- ADMIN: 无额外过滤
SELECT q.*, array_agg(acl) as acl FROM quests q
LEFT JOIN quest_acl acl ON q.id = acl.quest_id
WHERE (:status IS NULL OR q.status = :status)
  AND (:category IS NULL OR q.category = :category)
GROUP BY q.id
ORDER BY q.last_modified_at DESC
LIMIT :size OFFSET (:page - 1) * :size;

-- AUTHOR: 加上可见性过滤
... WHERE (q.status = 'PUBLISHED' OR q.id IN (
    SELECT quest_id FROM quest_acl WHERE discord_user_id = :userId
)) AND ...

-- 未认证: 强制 PUBLISHED
... WHERE q.status = 'PUBLISHED' AND ...
```

#### `GET /quests/:id`

获取 Quest 详情（含 steps 和 defaultCriteria）。

- **认证：** JWT Bearer（可选）
- **响应：** 完整 Quest 对象（见模型定义）
- **权限检查：** `canViewQuest(user, quest, acl)` — 未通过返回 404（不暴露存在性）
- **错误：** 404 `QUEST_NOT_FOUND`

#### `POST /quests`

创建新 Quest。

- **认证：** JWT Bearer
- **权限：** `requireRole("AUTHOR", "ADMIN")`
- **请求 Body：**

```json
{
    "id": "my-quest-slug",
    "name": "My Quest",
    "description": "A fun quest",
    "category": "mtr-lines",
    "tier": "easy",
    "questPoints": 10,
    "steps": [
        {
            "criteria": { "type": "VisitStationCriterion", "stationName": "Central" },
            "failureCriteria": null
        }
    ],
    "defaultCriteria": null
}
```

- **实现逻辑：**
  1. 校验 `id` 格式：仅允许 `[a-z0-9_-]`，长度 1-64
  2. 校验 `id` 唯一性：若已存在返回 409
  3. 校验 `category` 存在性（如果提供了非 null 值）
  4. 设置 `status = 'DRAFT'`
  5. 设置 `created_by = user.discordUserId`, `created_at = now()`
  6. 设置 `last_modified_by = user.discordUserId`, `last_modified_at = now()`
  7. 创建 ACL 记录：`{ quest_id, discord_user_id: user.discordUserId, discord_username: user.username, role: 'OWNER' }`
  8. 返回完整 Quest 对象

- **响应：** `201 Created` + Quest 对象
- **错误：** 400 `INVALID_QUEST_ID` | 409 `QUEST_ID_ALREADY_EXISTS`

#### `PUT /quests/:id`

更新 Quest。

- **认证：** JWT Bearer
- **权限：** `canEditQuest(user, quest, acl)` — 否则 403
- **请求 Body：** 与创建相同的字段（`id` 不可修改，忽略请求中的 `id`）
- **不允许通过此端点修改的字段：** `id`, `status`, `acl`, `createdBy`, `createdAt`
- **实现逻辑：**
  1. 查找 quest，不存在返回 404
  2. 权限检查
  3. 更新可变字段：`name`, `description`, `category`, `tier`, `questPoints`, `steps`, `defaultCriteria`
  4. 更新 `last_modified_by`, `last_modified_at`
  5. **如果 quest 当前是 PUBLISHED，更新 `sync_metadata.last_modified`**（触发 MC 同步）
  6. 返回更新后的完整 Quest

- **响应：** `200 OK` + Quest 对象
- **错误：** 403 `FORBIDDEN` | 404 `QUEST_NOT_FOUND`

#### `DELETE /quests/:id`

删除 Quest。

- **认证：** JWT Bearer
- **权限：** `isQuestOwner(user, quest, acl)` — 否则 403
- **实现逻辑：**
  1. 查找 quest，不存在返回 404
  2. 权限检查
  3. **如果 quest 当前是 PUBLISHED，更新 `sync_metadata.last_modified`**
  4. 删除 quest（ACL 级联删除）
  5. 返回 204

- **响应：** `204 No Content`
- **错误：** 403 `FORBIDDEN` | 404 `QUEST_NOT_FOUND`

#### `POST /quests/:id/publish`

将 Quest 从 DRAFT 发布为 PUBLISHED。

- **认证：** JWT Bearer
- **权限：** `requireRole("ADMIN")` — **仅 ADMIN 可操作**
- **实现逻辑：**
  1. 查找 quest，不存在返回 404
  2. 若已经是 PUBLISHED，返回 409
  3. 设置 `status = 'PUBLISHED'`
  4. 更新 `last_modified_by`, `last_modified_at`
  5. **更新 `sync_metadata.last_modified`**
  6. 返回更新后的 Quest

- **响应：** `200 OK` + `{ "status": "PUBLISHED" }`
- **错误：** 403 `FORBIDDEN` | 404 `QUEST_NOT_FOUND` | 409 `ALREADY_PUBLISHED`

#### `POST /quests/:id/unpublish`

将 Quest 从 PUBLISHED 下架为 DRAFT。

- **认证：** JWT Bearer
- **权限：** `isQuestOwner(user, quest, acl)` — OWNER 或 ADMIN
- **实现逻辑：**
  1. 查找 quest，不存在返回 404
  2. 若已经是 DRAFT，返回 409
  3. 设置 `status = 'DRAFT'`
  4. 更新 `last_modified_by`, `last_modified_at`
  5. **更新 `sync_metadata.last_modified`**（MC 服务器需要感知下架）
  6. 返回更新后的 Quest

- **响应：** `200 OK` + `{ "status": "DRAFT" }`
- **错误：** 403 `FORBIDDEN` | 404 `QUEST_NOT_FOUND` | 409 `ALREADY_DRAFT`

---

### 6.3 ACL 管理端点

#### `GET /quests/:id/acl`

获取 Quest 的 ACL 列表。

- **认证：** JWT Bearer
- **权限：** `isQuestOwner(user, quest, acl)` — OWNER 或 ADMIN
- **响应：**

```json
[
    { "discordUserId": "123", "discordUsername": "Author1", "role": "OWNER" },
    { "discordUserId": "456", "discordUsername": "Editor1", "role": "EDITOR" }
]
```

#### `PUT /quests/:id/acl`

设置 Quest 的 ACL（整体替换）。

- **认证：** JWT Bearer
- **权限：** `isQuestOwner(user, quest, acl)` — OWNER 或 ADMIN
- **请求 Body：**

```json
[
    { "discordUserId": "123", "role": "OWNER" },
    { "discordUserId": "456", "role": "EDITOR" }
]
```

- **校验规则：**
  1. 至少包含一个 OWNER
  2. 非 ADMIN 用户不能移除自己的 OWNER 角色（防止意外锁定自己）
  3. `role` 只能是 `OWNER` 或 `EDITOR`
  4. 不允许重复的 `discordUserId`

- **实现逻辑：**
  1. 校验上述规则
  2. 删除该 quest 所有现有 ACL 记录
  3. 插入新的 ACL 记录
  4. 可选：对每个 `discordUserId`，调用 Discord API 获取 `discordUsername` 填充冗余字段
  5. 返回更新后的 ACL 列表

- **响应：** `200 OK` + ACL 数组
- **错误：** 400 `INVALID_ACL` | 403 `FORBIDDEN` | 404 `QUEST_NOT_FOUND`

---

### 6.4 Category 管理端点

所有 Category 端点**仅 ADMIN 可操作**（写操作）。

#### `GET /categories`

列出所有 Category。

- **认证：** 可选（公开读取）
- **响应：**

```json
{
    "mtr-lines": {
        "name": "MTR Lines",
        "description": "Quests about MTR lines",
        "icon": "minecraft:rail",
        "order": 0,
        "tiers": {
            "easy": { "name": "Easy", "icon": "minecraft:gold_nugget", "order": 0 },
            "hard": { "name": "Hard", "icon": "minecraft:gold_ingot", "order": 1 }
        }
    }
}
```

格式说明：返回一个 Map，key 是 category ID，value 是 QuestCategory 对象。与 MC 服务器本地 `categories.json` 格式一致。

#### `POST /categories`

创建新 Category。

- **认证：** JWT Bearer
- **权限：** `requireRole("ADMIN")`
- **请求 Body：**

```json
{
    "id": "mtr-lines",
    "name": "MTR Lines",
    "description": "Quests about MTR lines",
    "icon": "minecraft:rail",
    "order": 0,
    "tiers": {
        "easy": { "name": "Easy", "icon": "minecraft:gold_nugget", "order": 0 }
    }
}
```

- **实现逻辑：**
  1. 校验 `id` 格式和唯一性
  2. 将 `id` 以外的字段存入 `data` JSONB 列
  3. 设置 `last_modified_at = now()`
  4. **更新 `sync_metadata.last_modified`**
  5. 返回完整 category

- **响应：** `201 Created`
- **错误：** 400 `INVALID_CATEGORY_ID` | 409 `CATEGORY_ID_ALREADY_EXISTS`

#### `PUT /categories/:id`

更新 Category。

- **认证：** JWT Bearer
- **权限：** `requireRole("ADMIN")`
- **实现逻辑：** 更新 `data` 和 `last_modified_at`，更新 `sync_metadata.last_modified`
- **响应：** `200 OK`

#### `DELETE /categories/:id`

删除 Category。

- **认证：** JWT Bearer
- **权限：** `requireRole("ADMIN")`
- **实现逻辑：**
  1. 删除 category
  2. 引用该 category 的 quest 的 `category` 字段变为 NULL（`ON DELETE SET NULL`）
  3. **更新 `sync_metadata.last_modified`**
- **响应：** `204 No Content`

---

### 6.5 同步端点（MC 服务器专用）

#### `GET /sync/status`

检查是否有数据更新。

- **认证：** `X-API-Key` header
- **响应：**

```json
{
    "lastModified": 1708000000000
}
```

- **实现：** `SELECT value FROM sync_metadata WHERE key = 'last_modified'`

#### `GET /sync/bundle`

获取完整的 PUBLISHED Quest 和 Category 数据包。

- **认证：** `X-API-Key` header
- **响应：**

```json
{
    "lastModified": 1708000000000,
    "quests": {
        "quest-id-1": {
            "id": "quest-id-1",
            "name": "...",
            "description": "...",
            "category": "mtr-lines",
            "tier": "easy",
            "questPoints": 10,
            "status": "PUBLISHED",
            "steps": [
                {
                    "criteria": { "type": "VisitStationCriterion", "stationName": "Central" }
                }
            ],
            "defaultCriteria": null
        }
    },
    "categories": {
        "mtr-lines": {
            "name": "MTR Lines",
            "description": "...",
            "icon": "minecraft:rail",
            "order": 0,
            "tiers": { ... }
        }
    }
}
```

- **实现逻辑：**

```sql
-- 获取所有 PUBLISHED Quest（不含元数据字段）
SELECT id, name, description, category, tier, quest_points, status, steps, default_criteria
FROM quests
WHERE status = 'PUBLISHED';

-- 获取所有 Category
SELECT id, data FROM categories;

-- 获取 lastModified
SELECT value FROM sync_metadata WHERE key = 'last_modified';
```

- **关键：Quest 响应中不包含 `createdBy`, `createdAt`, `lastModifiedBy`, `lastModifiedAt`, `acl`**。JSON 字段名使用 camelCase（`questPoints`, `defaultCriteria`）以与 MC 侧 Gson 序列化一致。

---

## 七、`sync_metadata.last_modified` 更新规则

以下操作必须更新全局 `last_modified` 时间戳（`UPDATE sync_metadata SET value = :now WHERE key = 'last_modified'`）：

| 操作 | 条件 |
|------|------|
| 更新 PUBLISHED Quest | 始终 |
| 删除 PUBLISHED Quest | 始终 |
| Quest DRAFT → PUBLISHED | 始终 |
| Quest PUBLISHED → DRAFT | 始终 |
| 创建/更新/删除 Category | 始终 |

以下操作**不需要**更新：

| 操作 | 原因 |
|------|------|
| 创建 Quest（新建为 DRAFT） | DRAFT 不同步 |
| 更新 DRAFT Quest | DRAFT 不同步 |
| 删除 DRAFT Quest | DRAFT 不同步 |
| 修改 ACL | ACL 不同步 |

---

## 八、错误响应格式

所有 4xx/5xx 响应使用统一格式：

```json
{
    "error": "ERROR_CODE",
    "message": "Human-readable description"
}
```

错误码枚举：

| HTTP | error | 场景 |
|------|-------|------|
| 400 | `INVALID_REQUEST` | 请求参数不合法 |
| 400 | `INVALID_QUEST_ID` | Quest ID 格式不合法 |
| 400 | `INVALID_ACL` | ACL 校验失败 |
| 401 | `UNAUTHORIZED` | 未提供或无效的认证 |
| 403 | `FORBIDDEN` | 无权限执行此操作 |
| 404 | `QUEST_NOT_FOUND` | Quest 不存在（或无查看权限） |
| 404 | `CATEGORY_NOT_FOUND` | Category 不存在 |
| 409 | `QUEST_ID_ALREADY_EXISTS` | Quest ID 重复 |
| 409 | `CATEGORY_ID_ALREADY_EXISTS` | Category ID 重复 |
| 409 | `ALREADY_PUBLISHED` | Quest 已经是 PUBLISHED 状态 |
| 409 | `ALREADY_DRAFT` | Quest 已经是 DRAFT 状态 |
| 500 | `INTERNAL_ERROR` | 服务器内部错误 |

---

## 九、CORS 配置

Webapp（SPA）与后端跨域部署时，需配置 CORS：

```
Access-Control-Allow-Origin: https://nquest-editor.example.com
Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
Access-Control-Allow-Headers: Authorization, Content-Type, X-API-Key
Access-Control-Allow-Credentials: true
```

---

## 十、实现检查清单

### Phase 1: 基础设施

- [ ] 项目初始化（选定框架、配置 linter、Dockerfile）
- [ ] 数据库连接 + migration（创建上述表和索引）
- [ ] 环境变量加载
- [ ] 统一错误处理中间件
- [ ] CORS 中间件
- [ ] 健康检查端点 `GET /health`

### Phase 2: 认证

- [ ] Discord OAuth2 流程（`/auth/discord`, `/auth/discord/callback`）
- [ ] JWT 签发与验证中间件
- [ ] API Key 认证中间件
- [ ] `GET /auth/me` 端点
- [ ] 角色映射逻辑（Discord Role → App Role）

### Phase 3: Category CRUD

- [ ] `GET /categories`
- [ ] `POST /categories`
- [ ] `PUT /categories/:id`
- [ ] `DELETE /categories/:id`
- [ ] 操作时更新 `sync_metadata.last_modified`

### Phase 4: Quest CRUD

- [ ] `GET /quests`（含分页、过滤、权限过滤）
- [ ] `GET /quests/:id`
- [ ] `POST /quests`（含 ID 校验、ACL 初始化）
- [ ] `PUT /quests/:id`（含权限检查）
- [ ] `DELETE /quests/:id`（含权限检查）
- [ ] `POST /quests/:id/publish`
- [ ] `POST /quests/:id/unpublish`
- [ ] 操作时按规则更新 `sync_metadata.last_modified`

### Phase 5: ACL 管理

- [ ] `GET /quests/:id/acl`
- [ ] `PUT /quests/:id/acl`（含所有校验规则）

### Phase 6: 同步端点

- [ ] `GET /sync/status`
- [ ] `GET /sync/bundle`（含 Quest 字段过滤）

### Phase 7: 测试与部署

- [ ] 端到端测试：完整 OAuth → CRUD → Sync 流程
- [ ] 用 MC Mod 的 `QuestSyncClient` 进行集成测试
- [ ] Dockerfile + docker-compose
- [ ] 生产环境部署
