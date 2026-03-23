# MovieMind 后端代码结构总览

> 参考 `CLAUDE.md` 与当前代码目录整理，便于快速理解模块边界与调用关系。

## 1. 顶层目录

- `src/main/java/com/huguo/moviemind_server`：业务代码主目录。
- `src/main/resources/application.properties`：运行配置（端口、H2、JPA、日志）。
- `src/test/java/...`：基础启动测试。
- `pom.xml`：Spring Boot + JPA + Security + H2 依赖定义。

## 2. 分层与包结构

项目采用典型的 **Controller -> Service -> Repository -> Entity** 分层。

- `auth/`：认证与当前用户上下文。
  - `controller/AuthController`：登录、登出、获取当前用户。
  - `service/AuthService`, `UserDetailsServiceImpl`：认证逻辑与 Spring Security 用户加载。
  - `repository/UserRepository`：用户持久层。
  - `model/User`：用户实体。
- `movie/`：电影信息检索。
  - `controller/MovieController`：电影列表、详情、按标签/年份等查询。
  - `service/MovieService`：电影查询组合逻辑。
  - `repository/MovieRepository`, `TagRepository`：电影与标签查询。
  - `model/Movie`, `Tag`：电影与标签实体。
- `preference/`：用户评分偏好。
  - `controller/RatingController`：评分 CRUD、Top 评分、评分计数。
  - `service/RatingService`：评分业务逻辑。
  - `repository/RatingRepository`：评分持久层。
  - `model/Rating`：评分实体。
- `watchlist/`：待看清单。
  - `controller/WatchlistController`：清单查询、新增、标记已看、统计。
  - `service/WatchlistService`：待看业务逻辑。
  - `repository/WatchlistItemRepository`：待看条目持久层。
  - `model/WatchlistItem`：待看条目实体。
- `user/`：用户信息与通知设置。
  - `controller/UserController`, `NotificationSettingsController`
  - `service/UserService`
  - `repository/NotificationSettingsRepository`
  - `model/NotificationSettings`
- `common/`：跨模块通用能力。
  - `dto/ApiResponse`, `PageResponse`：统一响应模型。
  - `exception/*`：统一异常与全局异常处理。
- `config/SecurityConfig`：Spring Security、会话、鉴权规则配置。
- `content/`：内容抓取与特征提取。
  - `client/ChineseMovieDataProvider`, `DoubanSuggestionProvider`：中文站点数据接入（豆瓣建议接口）。
  - `service/MovieDataIngestionService`：抓取并落库原始电影数据。
  - `service/RuleBasedFeatureExtractor`, `MovieFeatureExtractionService`：规则特征提取与特征存储。
  - `controller/ContentController`：触发抓取/分析与查询特征接口。
- `recommendation/`：推荐生成与反馈闭环（本地规则版）。
  - `service/RecommendationService`：候选过滤、偏好打分、推荐事件写入。
  - `controller/RecommendationController`：推荐获取与反馈提交接口。
  - `model/RecommendationEvent`, `RecommendationItem`：推荐事件与反馈数据模型。

## 3. API 与安全边界

- 认证接口：`/api/auth/**` 放行。
- H2 控制台：`/h2-console/**` 放行。
- 其余 `/api/**` 需要登录后访问。
- 登录失败/登出成功返回 JSON，适配前端 API 调用场景。

## 4. 典型请求路径（以“获取待看清单”为例）

1. `WatchlistController` 接收 HTTP 请求并做参数校验。
2. 调用 `WatchlistService` 执行业务判断（当前用户、状态过滤、分页等）。
3. `WatchlistService` 调用 `WatchlistItemRepository` 进行数据库查询。
4. 结果包装成 `ApiResponse` 或分页结构返回。

## 5. 数据与运行配置要点

- 数据库：H2 文件模式，默认路径 `./data/dbfile`。
- JPA DDL：`update`，启动时按实体自动同步表结构。
- 默认端口：`8080`。
- 安全与业务日志级别均开启 `DEBUG`（开发排查方便）。

## 6. 快速阅读建议

建议按以下顺序阅读，最快建立整体心智模型：

1. `CLAUDE.md`（功能总览）
2. `config/SecurityConfig.java`（访问规则与认证机制）
3. 选一个模块纵向读：`controller -> service -> repository -> model`
4. `common/` 中响应与异常，理解全局返回风格
5. `application.properties`，确认运行环境与数据库策略
