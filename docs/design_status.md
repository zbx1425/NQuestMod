# NQuest 项目现状文档

> 版本 0.4.4 · Fabric 1.20.4 · 最后更新 2026-02-21

---

## 一、项目概述

NQuest（全称 Nemo's Quest Mod）是一个 Minecraft Fabric 服务端模组，为 Let's Play 服务器上使用 Minecraft Transit Railway (MTR) 模组搭建的铁路网络提供**任务寻宝 / 打卡探索**玩法。玩家通过乘坐列车、到达车站、进入指定区域或完成地图上设置的互动来逐步完成任务，获得 Quest Points (QP)，并在排行榜上竞争。

---

## 二、UX 设计现状

### 2.1 入口

| 入口方式 | 说明 |
|---------|------|
| `/nquest` 命令 | 无参数时打开 GUI（`GuiStarter`）；若有正在进行的任务则直接进入 CurrentQuestScreen，否则进入 MainMenuScreen |

### 2.2 GUI 屏幕树

所有 GUI 均为**服务端 GUI**（基于 `eu.pb4:sgui`，使用原版箱子/容器界面渲染）。

```
GuiStarter.openEntry()
├─ 有活跃任务？ → CurrentQuestScreen
│                   └─ Abort Quest → DialogGui（确认放弃）
└─ 无活跃任务？ → MainMenuScreen (9×3)
                    ├─ [槽11] Start a Quest → QuestListScreen (9×4, 分类标签)
                    │                            └─ 选择任务 → DialogGui（确认开始）
                    ├─ [槽13] Leaderboards → LeaderboardScreen (9×4, 两级标签)
                    │                          ├─ Tab: Total QP（全时段/月度）
                    │                          ├─ Tab: Total Completions（全时段/月度）
                    │                          └─ Tab: Speedruns → QuestListScreen → QuestSpeedrunScreen
                    │                                                                  └─ 点击条目 → QuestCompletionDetailScreen
                    └─ [槽15] My Profile → ProfileScreen (9×3)
                                            ├─ 总 QP
                                            ├─ 总完成数
                                            └─ Quest History → QuestHistoryScreen (9×4, 分页)
                                                                └─ 点击条目 → QuestCompletionDetailScreen
```

### 2.3 GUI 基础组件

| 组件 | 说明 |
|------|------|
| `ParentedGui` | 继承 `SimpleGui`，带"返回"按钮（`goBack()` 返回父界面） |
| `ItemListGui<T>` | 分页列表，支持异步数据加载（`CompletableFuture`）、Loading 状态（雪球）、Error 状态（屏障）、上下页翻页（纸张图标，带 500ms 冷却） |
| `TabbedItemListGui<T, P, S>` | 在 `ItemListGui` 基础上加一级/二级标签切换（显示在第 0 行），用灰色/白色玻璃区分选中状态 |
| `DialogGui` | 确认/取消对话框，3×9 布局，Cancel（红色混凝土）和 Confirm（绿色混凝土） |

### 2.4 实时反馈

| 反馈渠道 | 触发场景 | 内容 |
|----------|---------|------|
| **聊天消息** | 任务开始 | ⭐ Quest Started! ⭐ + 任务名 + 第一步提示 |
| | 步骤完成 | ✔ Step Complete: ... + ▶ Next: ... |
| | 任务完成 | ⭐ Quest Complete! ⭐ + 用时 + QP 奖励 |
| | 任务放弃 | ✘ Quest Aborted ✘ |
| | 任务失败 | ✘ Quest Failed ✘ + 失败原因 |
| **Boss Bar** | 任务进行中 | 显示当前步骤的 `displayRepr`，进度条 = `currentStep / totalSteps`，蓝色 PROGRESS 样式 |
| **音效** | 步骤/任务完成 | `amethyst_block_resonate`（音量 2.0） |
| | 任务完成 | `ui_toast_challenge_complete` |
| | 放弃/失败 | `anvil_land`（音量 0.5） |

### 2.5 任务列表展示

- **QuestListScreen** 按 `QuestCategory` 分标签页（一级标签），每个标签内按 `QuestTier.order` 排序
- 每个任务显示：名称、Tier 标签（黄色）、描述 lore
- 图标：Tier 有自定义 icon（通过 ResourceLocation 指定物品），否则默认 `Items.BOOK`

### 2.6 当前任务界面

- **CurrentQuestScreen** 列出所有步骤
- 已完成步骤：绿色陶瓦（GREEN_TERRACOTTA）
- 当前步骤：黄色混凝土（YELLOW_CONCRETE）
- 未到步骤：灰色混凝土（GRAY_CONCRETE）
- 步骤编号：物品堆叠数 = `stepIndex + 1`
- 底部有"Abort Quest"按钮（BARRIER 图标），点击后弹出 DialogGui 确认

### 2.7 排行榜

| 类型 | 数据来源 | 条目展示 |
|------|---------|---------|
| Total QP | `getOverallQPLeaderboard` | 玩家头像 + `#排名 玩家名` + `xxx QP` |
| Total Completions | `getQuestCompletionsLeaderboard` | 同上 + `xxx completions` |
| Speedruns | `getQuestTimeLeaderboard`（先选任务） | 玩家头像 + 排名 + `H:MM:SS` + 可点击查看详情 |

- 支持全时段 / 月度切换（二级标签）

### 2.8 个人资料 & 历史

- **ProfileScreen**：玩家头像、总 QP、总完成数、进入历史记录入口
- **QuestHistoryScreen**：分页列出历史完成记录，每条显示任务名、完成时间、用时、获得 QP
- **QuestCompletionDetailScreen**：逐步展示每步耗时，头部显示任务名、完成者、QP、总用时

---

## 三、技术架构现状

### 3.1 模块划分

```
cn.zbx1425.nquestmod
├── NQuestMod.java          # 模组入口，生命周期管理
├── Commands.java           # Brigadier 命令注册（/nquest 及子命令）
├── QuestNotifications.java # IQuestCallbacks 实现，聊天/Boss Bar/音效通知
├── CommandSigner.java      # HMAC-MD5 时间戳签名（保护远程 set 命令）
│
├── data/                   # 核心数据与逻辑
│   ├── QuestDispatcher.java      # 任务调度核心：开始/停止/推进/失败判定
│   ├── QuestPersistence.java     # JSON 持久化（任务定义、分类、签名密钥）
│   ├── QuestException.java       # 错误类型枚举 + Brigadier 异常转换
│   ├── IQuestCallbacks.java      # 任务生命周期回调接口
│   ├── RuntimeTypeAdapterFactory  # Gson 多态序列化
│   ├── Vec3d.java                # 3D 坐标 + AABB 判定
│   │
│   ├── quest/              # 任务数据模型
│   │   ├── Quest.java            # 任务定义：id, name, description, category, tier, questPoints, steps, defaultCriteria
│   │   ├── Step.java             # 步骤 = criteria + failureCriteria（可选）
│   │   ├── QuestProgress.java    # 玩家正在进行的任务运行时状态
│   │   ├── PlayerProfile.java    # 玩家档案：UUID, 总QP, 总完成数, activeQuests
│   │   ├── QuestCategory.java    # 分类：name, description, icon, order, tiers
│   │   ├── QuestTier.java        # 分级：name, icon, order
│   │   └── QuestCompletionData   # 完成记录快照
│   │
│   ├── criteria/           # 条件系统
│   │   ├── Criterion.java        # 接口：isFulfilled / getDisplayRepr / createStatefulInstance / propagateManualTrigger
│   │   ├── CriteriaRegistry.java # 注册所有 Criterion 子类到 Gson RuntimeTypeAdapterFactory
│   │   ├── 逻辑组合: AndCriterion, OrCriterion, NotCriterion
│   │   ├── 状态保持: LatchingCriterion（一旦满足不可逆）, RisingEdgeAndConditionCriterion（上升沿+条件）
│   │   ├── 触发器: ManualTriggerCriterion, ConstantCriterion
│   │   ├── 包装器: Descriptor（自定义显示文本）
│   │   ├── 位置: InBoundsCriterion（AABB 区域）
│   │   ├── 运动: OverSpeedCriterion, TeleportDetectCriterion
│   │   │
│   │   └── mtr/            # MTR 专用条件
│   │       ├── MtrNameUtil.java          # 站名/线名匹配（支持 name: 和 id: 前缀）
│   │       ├── VisitStationCriterion     # 在指定车站区域内
│   │       ├── RideLineCriterion         # 正在乘坐指定线路
│   │       ├── RideToStationCriterion    # 乘车到达指定车站
│   │       └── RideLineToStationCriterion# 乘坐指定线路到达指定车站
│   │
│   └── ranking/            # 排行与持久化
│       ├── QuestUserDatabase.java  # SQLite：玩家档案 CRUD + 排行榜查询 + 完成记录
│       ├── PlayerQPEntry.java      # QP 排行条目
│       ├── PlayerCompletionsEntry  # 完成数排行条目
│       └── QuestTimeEntry.java     # 速通排行条目
│
├── interop/                # MTR 桥接层
│   ├── TscStatus.java            # 共享状态：玩家位置 → MTR 车站/线路映射
│   └── GenerationStatus.java     # 传送检测：generation 计数 + 玩家 warp 标记
│
├── sgui/                   # 服务端 GUI（全部 11 个屏幕类）
│   ├── GuiStarter, MainMenuScreen, QuestListScreen, CurrentQuestScreen
│   ├── LeaderboardScreen, QuestSpeedrunScreen, QuestCompletionDetailScreen
│   ├── ProfileScreen, QuestHistoryScreen
│   ├── ParentedGui, ItemListGui, TabbedItemListGui, DialogGui
│
└── mixin/                  # Mixin 注入
    ├── SimulatorMixin.java       # 注入 MTR Simulator.tick()，同步站点/线路数据到 TscStatus
    ├── ServerPlayerMixin.java    # 注入 teleportTo()，标记传送事件到 GenerationStatus
    └── ScoreboardCommandMixin.java # 注入 setScore()，兼容旧版 scoreboard 触发
```

### 3.2 数据流：任务进度推进

```
每秒触发一次 (ServerTickEvents, tickCount % 20 == 15)
    │
    ▼
QuestDispatcher.updatePlayers()
    │  遍历 playerProfiles → 每个有 activeQuests 的玩家
    ▼
tryAdvance(profile, quest, progress, player, triggerId=null)
    │
    ├─ 1. 如果 currentStepStateful == null，创建当前步骤的有状态实例
    │     （Criterion.createStatefulInstance()，深拷贝用于保持 Latching 等状态）
    │
    ├─ 2. 检查失败条件 → isFailedAndReason()
    │     （优先检查步骤级 failureCriteria，否则检查任务级 defaultCriteria.failureCriteria）
    │     → 若失败：移除活跃任务，回调 onQuestFailed
    │
    └─ 3. 检查完成条件 → isFulfilled(player)
          → 若满足：advanceQuestStep()
              ├─ currentStepIndex++
              ├─ 回调 onStepCompleted
              ├─ 若所有步骤完成 → 生成 QuestCompletionData，更新 profile 统计，
              │                    回调 onQuestCompleted，写入 SQLite
              └─ 否则 → 记录下一步开始时间
```

### 3.3 数据流：MTR 状态同步

```
每秒第 5 tick (tickCount % 20 == 5):
    TscStatus.requestUpdate(server)
    → 记录所有在线玩家的 BlockPos 到 CLIENT_POSITIONS

MTR Simulator.tick() 结尾 (SimulatorMixin):
    → 检查 nonce 是否更新
    → 解析 STATION_NAME_REQUESTS（延迟加载站名）
    → 如果有任务进行中(isAnyQuestGoingOn)：
        ├─ 对每个 client：找出玩家所在的所有 Station area → CLIENTS[uuid]
        └─ 对每个 siding 上的乘客：附加其所乘线路信息 → CLIENTS[uuid]

MTR Criterion 在 isFulfilled() 中读取:
    TscStatus.getClientState(player)
    → 获取 stations: Collection<NameIdData>, line: NameIdData
```

### 3.4 数据流：手动触发

```
/nquest trigger <player> <trigger_id>  (权限等级 2)
    │
    ▼
QuestDispatcher.triggerManualCriterion(uuid, triggerId, player)
    │  遍历该玩家的 activeQuests
    ▼
tryAdvance(... triggerId)
    ├─ propagateManualTrigger(triggerId)
    │     递归传播到所有嵌套 Criterion
    │     ManualTriggerCriterion: 如果 id 匹配则设为 isTriggered=true
    └─ 然后正常走 isFulfilled / isFailedAndReason 检查

兼容路径（ScoreboardCommandMixin）:
    setScore 目标为 "mtrq_quest_complete" 时
    → 自动触发 "legacy_trigger_<score>"
```

### 3.5 持久化

| 类型 | 存储位置 | 格式 | 时机 |
|------|---------|------|------|
| 任务定义 | `<world>/nquest/quests/<id>.json` | JSON (Gson + RuntimeTypeAdapterFactory) | set 命令时立即保存；启动时加载全部 |
| 分类定义 | `<world>/nquest/categories.json` | JSON | set 命令时保存；服务器关闭时保存；启动时加载 |
| 签名密钥 | `<world>/nquest/command_signer.json` | JSON（UUID） | 首次启动自动生成 |
| 玩家档案 | `<world>/nquest/user.db` (SQLite) | 表 `player_profiles` | 玩家退出时保存；加入时加载 |
| 完成记录 | 同上 | 表 `quest_completions`（含 stepDurations JSON） | 任务完成时插入 |

### 3.6 命令体系

| 命令 | 权限 | 功能 |
|------|------|------|
| `/nquest` | 0 | 打开 GUI |
| `/nquest start <player> <quest_id>` | 2 | 为指定玩家开始任务 |
| `/nquest stop [player]` | 0/2 | 放弃任务（无参为自己，指定玩家需权限2） |
| `/nquest trigger <player> <trigger_id>` | 2 | 手动触发条件 |
| `/nquest quests set <sign> <json>` | 2 | 设置任务定义（需签名） |
| `/nquest quests get <quest_id>` | 3 | 获取任务 JSON |
| `/nquest quests remove <quest_id>` | 3 | 删除任务定义 |
| `/nquest categories set <sign> <json>` | 2 | 设置分类定义（需签名） |
| `/nquest categories get` | 3 | 获取分类 JSON |
| `/nquest sign` | 3 | 生成 5 分钟有效的 HMAC-MD5 时间戳签名 |

签名机制：`quests set` 和 `categories set` 需要通过 `/nquest sign` 获取签名，防止未授权修改。

### 3.7 条件系统详解

条件系统是任务的核心，采用**组合模式 + 多态序列化**。

**接口 `Criterion`：**
- `isFulfilled(ServerPlayer)` → 是否满足
- `getDisplayRepr()` → 用于 GUI/聊天的显示文本
- `createStatefulInstance()` → 深拷贝，用于 Latching/RisingEdge 等有状态条件
- `propagateManualTrigger(String)` → 传播手动触发信号

**已实现的条件类型：**

| 类型 | 说明 | 有状态 |
|------|------|--------|
| `ConstantCriterion` | 常量 true/false | 否 |
| `ManualTriggerCriterion` | 匹配 triggerId 后设为 true | 是 |
| `InBoundsCriterion` | 玩家在 AABB 内 | 否 |
| `OverSpeedCriterion` | 玩家速度超过阈值（曼哈顿距离/tick） | 是 |
| `TeleportDetectCriterion` | 玩家发生传送 | 是 |
| `VisitStationCriterion` | 玩家在匹配的 MTR 车站区域内 | 否 |
| `RideLineCriterion` | 玩家正在乘坐匹配的 MTR 线路 | 否 |
| `RideToStationCriterion` | 乘车 + 到站（RisingEdge 包装） | 是 |
| `RideLineToStationCriterion` | 乘指定线路 + 到站 | 是 |
| `AndCriterion` | 全部子条件满足 | 取决于子条件 |
| `OrCriterion` | 任一子条件满足 | 取决于子条件 |
| `NotCriterion` | 取反 | 取决于子条件 |
| `LatchingCriterion` | 一旦满足则锁定 | 是 |
| `RisingEdgeAndConditionCriterion` | trigger 从 false→true 的瞬间 condition 也为 true | 是 |
| `Descriptor` | 为条件添加自定义描述文本 | 取决于子条件 |

### 3.8 任务模型

**Quest 结构：**
- `id` (String) — 唯一标识
- `name`, `description` — 显示信息
- `category`, `tier` — 分类/等级（关联 QuestCategory/QuestTier）
- `questPoints` (int) — 奖励 QP
- `defaultCriteria` (Step, 可选) — 全局失败条件，适用于所有步骤
- `steps` (List<Step>) — 步骤序列，按顺序完成

**Step 结构：**
- `criteria` (Criterion) — 完成条件
- `failureCriteria` (Criterion, 可选) — 步骤级失败条件（优先于 defaultCriteria 的 failureCriteria）

**限制：** 当前一个玩家同一时间只能进行一个任务（`QUEST_ONLY_ONE_AT_A_TIME`）。

### 3.9 依赖关系

| 依赖 | 版本 | 用途 |
|------|------|------|
| Fabric Loader | 0.17.2 | 模组加载器 |
| Fabric API | 0.97.3+1.20.4 | 事件/命令/网络 API |
| Minecraft Transit Railway (MTR) | 4.0.0 | 列车/车站/线路系统 |
| SGUI (Polymer) | 1.4.2+1.20.4 | 服务端 GUI 框架 |
| SQLite JDBC | 3.50.3.0 | 玩家数据持久化 |
| Gson | Minecraft 内置 | JSON 序列化 |

### 3.10 Mixin 注入点

| Mixin | 目标 | 注入点 | 功能 |
|-------|------|--------|------|
| `SimulatorMixin` | `org.mtr.core.simulation.Simulator` | `tick()` RETURN | 将 MTR 模拟器中的站点/线路/乘客数据同步到 `TscStatus` |
| `ServerPlayerMixin` | `net.minecraft.server.level.ServerPlayer` | `teleportTo` 两个重载 | 标记传送事件到 `GenerationStatus`（用于 `TeleportDetectCriterion`） |
| `ScoreboardCommandMixin` | `net.minecraft.server.commands.ScoreboardCommand` | `setScore` | 兼容旧版：`mtrq_quest_complete` 计分板目标触发 `legacy_trigger_<score>` |

---

## 四、JSON Schema 与实际实现的差异

`docs/json_schema.txt` 中定义的 schema 与代码实际支持的模型存在差异，应以代码为准。

---

## 五、外部 API

项目引用了 MTR 系统地图 API（`docs/system_map_api.txt`），用于获取车站和线路数据：

- **端点：** `GET https://letsplay.minecrafttransitrailway.com/system-map/mtr/api/map/stations-and-routes?dimension=0`
- **用途：** 在代码中未直接调用此 API；模组通过 Mixin 直接从 MTR 的 `Simulator` 实例读取数据。此 API 可用于外部工具（如任务编辑器）获取站点/线路列表。

---

## 六、当前设计特点 & 待关注事项

### 设计亮点
1. **条件系统高度可组合** — 通过 And/Or/Not/Latching/RisingEdge 等逻辑条件的嵌套组合，能表达复杂的游戏逻辑
2. **有状态条件与深拷贝** — `createStatefulInstance()` 机制让同一任务定义可被多个玩家同时使用而互不干扰
3. **解耦设计** — 核心逻辑（`QuestDispatcher`）与 Minecraft 特定逻辑（通知、GUI）分离，通过 `IQuestCallbacks` 接口连接
4. **MTR 集成非侵入** — 通过 Mixin + 共享状态（`TscStatus`）实现，不修改 MTR 代码
5. **签名保护** — 任务/分类的远程写入需要 HMAC-MD5 签名，防止滥用

### 待关注事项
1. **JSON Schema 过时** — `docs/json_schema.txt` 未反映代码中的全部条件类型和字段，需要更新
2. **单任务限制** — 玩家同时只能进行一个任务，`activeQuests` 虽为 Map 但实际被限制为单条目
3. **QuestHistoryScreen 总数估算** — 历史记录无法获取精确总数，使用 `99999` 作为占位值
4. **TscStatus 同步时序** — 更新请求（tick 5）和任务推进（tick 15）之间有 0.5 秒间隔，依赖 MTR Simulator 在此期间完成 tick
5. **错误处理** — `addQuestCompletion` 的 SQLException 被包装为 RuntimeException
6. **QuestCompletionDetailScreen 初始化顺序** — `rowContentStarts = 1` 在 `init()` 之后才设置
7. **所有 GUI 文本为英文硬编码** — 无 i18n 支持
8. **preTouchDescriptions()** — 用于预加载 MTR 站名的异步解析，是一个权宜之计（代码注释中也提到）
