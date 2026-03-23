## moviemind 后端系统分析与设计文档

**版本：** v0.1（顶层设计草案）  
**后端技术栈（推定）：** Spring Boot + Spring MVC + Spring Data/JPA + H2/关系型数据库  
**对应前端：** `moviemind-web`（Next.js 推荐系统前端）

---

## 1. 业务目标与场景概述

- 为用户提供**个性化电影推荐**，支持：
  - 电影推荐页：按用户兴趣推荐电影，支持“换一批”“不感兴趣”“采纳”。
  - 偏好管理页：管理用户已看电影的评分、备注与标签。
  - 待看清单：管理“计划观看”的电影，将其标记为已看并评分。
  - 设置页：账号信息、通知偏好、清除个人数据、退出登录。
- 推荐侧整体采用**两阶段架构**：
  - **第一阶段：本地高效召回 + 打分（预计算）**，从全量电影中为每个用户选出一批候选。
  - **第二阶段：大模型 rerank + 生成推荐理由**，在候选中精选并输出中文推荐语。
- 未来扩展：
  - 引入爬虫或开放 API 获取影评，对电影做**内容分析与打标**，作为推荐的额外特征。
  - 推荐算法的特征使用支持**可配置开关**，便于迭代。

### 1.1 当前落地进度（2026-03）

- ✅ 已落地基础模块：`auth`、`movie`、`preference`、`watchlist`、`user/settings`。
- ✅ 已落地内容模块 v1：
  - 中国站点数据接入（豆瓣建议接口）与原始数据存储；
  - 规则特征提取（`rule-based-v1.1`）与特征存储；
  - 支持接口触发单片/批量分析。
- ✅ 已落地推荐模块 v1：
  - 本地规则推荐（genre/tag/external rating 组合打分）；
  - 推荐事件记录与反馈提交；
  - 采纳反馈自动写入待看清单。
- ⏳ 未落地部分：
  - LLM rerank 与推荐理由生成增强；
  - 推荐候选预计算池与特征开关；
  - 多源内容抓取、离线调度、特征质量评估。

---

## 2. 核心领域模型设计

> 本节只讲“领域概念 & 关系”，不绑定具体 ORM 细节。字段为示意，可按需要增减。

### 2.1 User（用户）

- **职责**：系统的一切行为归属的主体，是所有偏好、评分、待看清单和设置的拥有者。
- **主要字段**
  - `id`: 用户唯一标识（建议 UUID）。
  - `username`: 登录名（与前端登录页一致）。
  - `passwordHash`: 密码哈希。
  - `createdAt`, `updatedAt`.
- **关系**
  - 1 - N `Rating`（评分/偏好记录）
  - 1 - N `WatchlistItem`
  - 1 - 1 `NotificationSettings`

### 2.2 Movie（电影）

- **职责**：推荐系统中的“物品”，是推荐、评分、待看的基础。
- **主要字段**
  - `id`: 内部电影 ID。
  - `externalId`: 外部数据源 ID（如 TMDB/豆瓣等，可选）。
  - `title`: 标题。
  - `year`: 上映年份。
  - `genres`: 类型列表（动作/科幻/剧情等），可用多对多表 `movie_genre`。
  - `tags`: 基础标签（如 “烧脑”“治愈”等），可用多对多表 `movie_tag`。
  - `posterUrl`: 海报地址。
  - `ratingExternal`: 外部评分（如 IMDB/豆瓣分数）。
  - `metadataJson`: 额外元数据（导演、主演、地区、时长等，以 JSON 存储）。
- **关系**
  - N - 1 `Rating`, N - 1 `WatchlistItem`, N - 1 `ContentAnalysisFeature`.

### 2.3 Rating / UserPreference（评分/偏好记录）

- **职责**：记录用户对“已观看电影”的评分与主观感受，是推荐算法最重要的显式反馈数据。
- **主要字段**
  - `id`
  - `userId`
  - `movieId`
  - `score`: 1–10 分。
  - `tags`: 用户为该电影打的标签（字符串列表或关联表），与前端偏好页标签体系对应。
  - `notes`: 用户备注。
  - `ratedAt`: 评分时间。
  - `source`: 来源（如 `MANUAL`, `FROM_WATCHLIST` 等）。
- **关系**
  - N - 1 `User`, N - 1 `Movie`.

### 2.4 WatchlistItem（待看清单条目）

- **职责**：记录用户“计划观看”的电影，以及从待看变为已看的过程。
- **主要字段**
  - `id`
  - `userId`
  - `movieId`
  - `status`: `PENDING` / `WATCHED`。
  - `addedAt`: 加入待看的时间。
  - `watchedAt`: 标记为已看的时间（可空）。
  - `ratingId`: 若在标记为已看时打了分，则指向对应 `Rating` 记录（可空）。
  - `aiReasonSnapshot`: 当时推荐理由的文案快照（便于在待看页展示“为何当初推荐/采纳”）。
- **关系**
  - N - 1 `User`, N - 1 `Movie`, 可选 1 - 1 `Rating`.

### 2.5 RecommendationEvent（推荐事件）

- **职责**：记录“系统在某次请求中向某用户推荐了哪些电影，以及后续反馈”，用于日后分析和算法迭代。
- **主要字段**
  - `id`
  - `userId`
  - `generatedAt`
  - `algorithmVersion`: 当前使用的推荐算法版本，例如 `"v1-local-genre-tag-sentiment"`。
  - `items`: 嵌套结构，或拆为子表：
    - `movieId`
    - `rankLocal`: 本地阶段排序位置。
    - `scoreLocal`: 本地阶段综合得分。
    - `rankFinal`: LLM 排序后的位置。
    - `aiReason`: 返回给前端的推荐理由。
    - `feedback`: 后续写入，如 `ADOPTED` / `REJECTED` / `IGNORED`。
    - `feedbackAt`: 用户反馈时间（可空）。

### 2.6 Tag / Genre（标签与类型词典）

- **职责**：规范电影和评分所使用的“类型”和“语义标签”。
- **主要字段**
  - `id`
  - `name`
  - `type`: 如 `GENRE` / `MOOD` / `STYLE` 等。
  - `description`

### 2.7 NotificationSettings（通知设置）

- **职责**：对应前端设置页中的通知开关。
- **主要字段**
  - `userId`（唯一）
  - `watchRemindersEnabled`: 是否开启“待评分提醒”。
  - `newRecommendationsEnabled`: 是否开启“新推荐通知”。
  - `weeklyDigestEnabled`: 是否开启“每周摘要”。
  - `updatedAt`.

### 2.8 ContentAnalysisFeature（内容分析特征）【扩展模块】

- **职责**：存储从爬虫或开放 API 获取的影评经处理后得到的高阶特征，供推荐算法使用。
- **主要字段**
  - `movieId`
  - `sentimentScore`: 整体情感得分（如 -1~1 或 0~1）。
  - `themes`: 主题标签列表（家庭、阶级、成长等）。
  - `moodTags`: 氛围标签（治愈、压抑、高能等）。
  - `styleTags`: 风格标签（黑色幽默、魔幻现实等）。
  - `keywordVectorRef`: 指向外部向量存储/向量 ID（可选）。
  - `dataSource`: 数据来源（douban、imdb、自家爬虫等）。
  - `updatedAt`.

### 2.9 RecommendationFeatureToggle（推荐特征开关）

- **职责**：控制本地算法在打分时启用哪些特征，便于实验与调优。
- **主要字段**
  - `id` 或使用单例配置。
  - `useGenre`, `useTag`, `useYear`, `useRegion`, `usePopularity`.
  - `useContentSentiment`, `useContentThemes`, `useContentMood`.
  - `updatedAt`.

---

## 3. 模块划分与包结构

系统采用“分层 + 业务模块”的结构。推荐一个典型的包组织方式（基于 Spring Boot 单体）：

```text
com.huoguo.moviemind
├─ auth           # 认证与用户管理
├─ user           # 用户领域（User、NotificationSettings）
├─ movie          # 电影内容与标签（Movie、Genre、Tag）
├─ preference     # 用户评分与偏好（Rating/UserPreference）
├─ watchlist      # 待看清单
├─ recommendation # 推荐服务（本地召回 + LLM rerank + 事件记录）
├─ content        # 内容分析模块（影评特征）
└─ common         # 通用基础设施（异常、拦截器、配置等）
```

下文按业务模块说明职责与对外接口。

### 3.1 auth 模块（认证）

- **职责**
  - 处理用户登录/退出。
  - 使用账号密码 + Cookie 进行认证（不单独设计 AuthSession 模型）。
  - 与 Spring Security 或手写过滤器集成，在请求中解析 Cookie，注入当前登录用户。
- **典型接口**
  - `POST /api/auth/login`：校验用户名密码，成功后设置登录 Cookie。
  - `POST /api/auth/logout`：清除 Cookie。
- **内部实现要点**
  - 密码存储使用安全哈希（如 BCrypt）。
  - 登录成功后，可将 `userId` 放入签名 Cookie 中（如 JWT 样式但只在服务端校验，不必建 session 表）。

### 3.2 user 模块（用户与设置）

- **职责**
  - 管理 `User` 与 `NotificationSettings`。
  - 提供给前端的用户信息与设置接口。
- **典型接口**
  - `GET /api/users/me`：获取当前登录用户信息。
  - `POST /api/users/change-password`：修改密码。
  - `GET /api/settings/notifications`：获取通知设置。
  - `PUT /api/settings/notifications`：更新通知设置。
  - `POST /api/settings/clear-data`：清除当前用户所有业务数据（偏好、待看、推荐事件等）。

### 3.3 movie 模块（电影内容）

- **职责**
  - 维护电影基础信息与静态标签/类型。
  - 为推荐模块与前端提供电影详情数据。
- **典型接口**
  - `GET /api/movies/{id}`：电影详情。
  - `GET /api/movies`：按条件检索电影（用于后台管理或调试）。
  - `GET /api/genres`, `GET /api/tags`：返回类型/标签词典。

### 3.4 preference 模块（用户评分与偏好）

- **职责**
  - 管理 `Rating/UserPreference` 记录。
  - 与前端偏好管理页对接。
  - 对推荐模块暴露用户偏好数据访问接口。
- **典型接口（REST）**
  - `GET /api/preferences`：查询当前用户的偏好列表（支持搜索、排序）。
  - `POST /api/preferences`：新增一条评分/偏好。
  - `PUT /api/preferences/{id}`：编辑评分或备注/标签。
  - `DELETE /api/preferences/{id}`：删除一条偏好。
- **对内部（recommendation）的服务接口**
  - `List<Rating> findRatingsByUser(Long userId)`：供画像/召回使用。

### 3.5 watchlist 模块（待看清单）

- **职责**
  - 管理 `WatchlistItem`。
  - 接收推荐模块“采纳”动作创建待看项。
  - 支持从待看转为已看并打分。
- **典型接口**
  - `GET /api/watchlist`：获取当前用户待看/已看列表（可按 status 过滤）。
  - `POST /api/watchlist`：添加待看条目（通常从推荐页“采纳”触发）。
  - `PUT /api/watchlist/{id}/mark-watched`：标记为已看并附带评分与备注：
    - 内部会：
      - 更新 `WatchlistItem.status`、`watchedAt`。
      - 在 `preference` 模块中创建或更新 `Rating` 记录。
  - `DELETE /api/watchlist/{id}`：移除条目。

### 3.6 content 模块（内容分析）

- **职责**
  - 存储由爬虫/开放 API + LLM/算法分析得到的 `ContentAnalysisFeature`。
  - 对推荐模块提供只读访问能力。
- **典型接口**
  - 内部接口（供推荐与数据采集任务使用）：
    - `ContentAnalysisFeature findByMovieId(...)`
    - 批量更新/写入接口供离线爬虫任务调用。
  - 不一定直接暴露给前端，可先做成后端内部模块。

### 3.7 recommendation 模块（推荐服务）

- **职责**
  - 从多个模块获取数据（User、Movie、Rating、Watchlist、ContentAnalysisFeature）。
  - 执行 **预计算本地召回 + 打分**。
  - 在线调用 **LLM rerank + 生成推荐理由**。
  - 记录 `RecommendationEvent` 与用户反馈。
- **对前端的接口**
  - `GET /api/recommendations`：
    - 输入：当前登录用户（从 Cookie 中解析）。
    - 输出：一批推荐电影列表，每条含：
      - `movieId`, `title`, `year`, `genres`, `posterUrl`
      - `scoreLocal`（可不透出给前端，只供调试）
      - `aiReason`
  - `POST /api/recommendations/feedback`：
    - 输入：`recommendationEventId`, `movieId`, `feedbackType (ADOPTED/REJECTED)`。
    - 行为：更新 `RecommendationEvent` 中对应 item 的 feedback 字段。
    - 若 `ADOPTED`，可触发：
      - 在 `watchlist` 模块中新建 `WatchlistItem`。

---

## 4. 推荐算法与预计算设计

### 4.1 用户画像构建（User Profile）

在 `recommendation` 模块中实现 `UserProfileService`，核心逻辑：

- 输入：
  - 用户的 `Rating` 列表。
  - 可选：最近 `Watchlist`、观看行为等。
- 输出：
  - 用户画像对象 `UserProfile`，包含：
    - `genreWeights`: 各类型偏好权重。
    - `tagWeights`: 各标签偏好权重。
    - `yearPreference`: 偏好年代分布。
    - 可选 `regionPreference`, `directorPreference` 等。
- 计算方式示例：
  - 评分权重：`w(score) = max(0, score - 5)`。
  - Genre 权重：对用户高分电影的 Genre 加权计数。
  - Tag 权重：对用户打标签做加权计数。

### 4.2 候选召回与本地打分（CandidateService）

**步骤**

1. **候选池过滤**
   - 从 `movie` 模块获取电影集合（可以全量或按时间/热度过滤）。
   - 排除：
     - 用户已评分的电影（`Rating`）。
     - 用户 `Watchlist` 中 status 为 `WATCHED` 或 `PENDING` 的电影。
     - 用户明确“不感兴趣”的电影（从 `RecommendationEvent` 的反馈中获取）。

2. **读取 content 特征**
   - 对候选电影读取 `ContentAnalysisFeature`（若存在）。

3. **根据特征开关计算本地得分**
   - 读取 `RecommendationFeatureToggle` 配置。
   - 对每个候选电影计算：
     - `simGenre`：电影类型与用户 `genreWeights` 的相似度。
     - `simTag`：电影标签与用户 `tagWeights` 的相似度。
     - `priorPopularity`：外部评分/热度。
     - `contentSentimentMatch`：用户偏好情感与电影 `sentimentScore` 的匹配度（可简单用阈值/区间）。
     - `contentThemeMatch`、`contentMoodMatch`：与用户曾高分电影主题/氛围的重合度。
   - 在启用的特征集合上做线性组合：
     - `scoreLocal = Σ (w_feature * value_feature)`。

4. **选出 Top-K 候选**
   - 按 `scoreLocal` 排序，取前 K（如 30–50 部），作为传给 LLM 的候选集。

### 4.3 预计算结果结构（UserCandidatePool）

- **实体/表：`UserCandidatePool`**
  - `userId`
  - `generatedAt`
  - `algorithmVersion`
  - `items`: 列表项（子表）：
    - `movieId`
    - `scoreLocal`
    - 可选 `reasonFeatures` 用于记录“贡献最高的几个特征”。

- **批处理任务**
  - 周期任务（如每天/每小时）：
    - 扫描活跃用户列表。
    - 为每个用户构建 `UserProfile`。
    - 调用 `CandidateService` 生成候选并打分。
    - 将结果写入/更新 `UserCandidatePool`。

### 4.4 在线推荐接口与 LLM rerank

- **在线流程（`RecommendationService`）**
  1. 根据当前登录用户 ID，从 `UserCandidatePool` 取最近一次结果。
  2. 若 pool 不存在或过期，则可以：
     - 同步再跑一次候选计算（简单版）；或
     - 返回兜底推荐（全局热门列表）。
  3. 从 pool 中取前 K 条电影。
  4. 准备 LLM 调用上下文：
     - 用户画像摘要（最喜欢的几部电影、偏好类型/标签）。
     - 候选电影的基础信息与 content 特征摘要。
  5. 调用 LLM，对 K 条候选进行 rerank + 生成 `aiReason`（此处仅作为黑盒，不在本设计展开）。
  6. 根据 LLM 输出得到最终 Top-N 推荐结果。
  7. 创建 `RecommendationEvent` 记录此次推荐会话。
  8. 返回推荐结果 DTO 给前端。

- **用户反馈处理（`RecommendationFeedbackService`）**
  - “不感兴趣”：
    - 更新 `RecommendationEvent.items.feedback = REJECTED`。
    - 将该电影加入“负反馈列表”，下次召回时过滤/减分。
  - “采纳”：
    - 更新 `feedback = ADOPTED`。
    - 在 `watchlist` 模块中创建对应 `WatchlistItem`。

---

## 5. 安全与数据清理

### 5.1 安全与权限

- 使用账号密码 + Cookie 实现认证：
  - 登录后写入签名 Cookie（包含 userId）。
  - 每个受保护接口通过过滤器或 Spring Security `Authentication` 获取当前用户。
- 授权策略简单：
  - 所有 `/api/**` 接口均要求登录。
  - 数据按 userId 做行级隔离，后端以当前用户 ID 为准进行查询/更新。

### 5.2 数据清理（用户“清除所有数据”）

对应前端设置页“清除所有数据”按钮：

- `POST /api/settings/clear-data` 行为：
  - 删除当前用户的：
    - `Rating` / `UserPreference`。
    - `WatchlistItem`。
    - 与该用户相关的 `RecommendationEvent`。
    - `NotificationSettings`（可重置为默认）。
  - 保留 `User` 账号本身（如需要也可以一并删除，由业务决定）。
  - 清除后可强制退出登录。

---

## 6. 非功能性需求简述

- **性能**
  - 大部分重计算工作放在预计算任务中完成，在线请求只需读取预计算结果 + 调用 LLM。
  - 通过分页/限制 K、N 的大小控制 LLM 调用成本。
- **扩展性**
  - 推荐特征通过 `RecommendationFeatureToggle` 控制是否启用。
  - 新特征（如演员相似度、影评 embedding）只需在 `ContentAnalysisFeature` 与打分函数中扩展。
  - 算法升级通过 `algorithmVersion` 标记，便于回滚与排错。
- **可观测性**
  - 建议对每次 `RecommendationEvent` 记录关键元信息，便于后续分析推荐效果。
  - 对 LLM 调用增加基础日志（耗时、异常），方便排查问题。

---

## 7. 实施优先级建议

1. **基础 CRUD 与认证**
   - 搭建 auth、user、movie、preference、watchlist 基础 API，打通前后端页面的最小可用链路。
2. **简单本地推荐（不含 LLM）**
   - 在 recommendation 模块中先实现“基于 Genre + Tag + 热度”的本地打分与推荐接口。
3. **引入预计算与特征开关**
   - 增加 `UserCandidatePool` 与批处理任务。
   - 实现 `RecommendationFeatureToggle` 配置逻辑。
4. **接入 LLM**
   - 在已有本地候选基础上，增加 LLM rerank 与 `aiReason` 生成。
5. **内容分析模块（content）**
   - 设计并实现 `ContentAnalysisFeature` 存储结构。
   - 对接爬虫/开放 API 与 LLM 分析流程，将结果写入 content 模块。
