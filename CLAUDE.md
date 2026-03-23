# CLAUDE.md - MovieMind 后端

## 项目概述

MovieMind 是一个电影推荐系统，包含 Spring Boot 后端和 Next.js 前端。后端已实现基础 CRUD API 和认证功能。

## 已实现功能

### 1. 认证模块 (auth)
- **User 实体**：用户基本信息（用户名、密码哈希、创建时间、更新时间）
- **认证 API**：
  - `POST /api/auth/login` - 用户登录
  - `POST /api/auth/logout` - 用户登出
  - `GET /api/auth/me` - 获取当前用户信息
- **安全配置**：基于 Cookie 的会话管理，BCrypt 密码加密

### 2. 电影模块 (movie)
- **Movie 实体**：电影信息（标题、年份、海报、外部评分等）
- **Genre 和 Tag 实体**：电影类型和标签系统
- **电影 API**：
  - `GET /api/movies` - 获取电影列表（支持搜索、分页、排序）
  - `GET /api/movies/{id}` - 获取电影详情
  - `GET /api/movies/external/{externalId}` - 通过外部 ID 获取电影
  - `GET /api/movies/genres/{genreName}` - 获取指定类型的电影
  - `GET /api/movies/tags/{tagName}` - 获取指定标签的电影
  - `GET /api/movies/genres` - 获取所有类型
  - `GET /api/movies/tags` - 获取所有标签

### 3. 偏好管理模块 (preference)
- **Rating 实体**：用户评分记录（电影、分数、标签、备注、评分时间）
- **偏好 API**：
  - `GET /api/preferences` - 获取用户偏好列表（支持搜索、分页）
  - `POST /api/preferences` - 新增偏好
  - `PUT /api/preferences/{id}` - 更新偏好
  - `GET /api/preferences/{id}` - 获取单个偏好
  - `DELETE /api/preferences/{id}` - 删除偏好
  - `GET /api/preferences/top` - 获取用户高分电影
  - `GET /api/preferences/count` - 获取用户评分总数

### 4. 待看清单模块 (watchlist)
- **WatchlistItem 实体**：待看清单条目（用户、电影、状态、添加时间、观看时间）
- **待看清单 API**：
  - `GET /api/watchlist` - 获取待看清单（支持状态过滤、搜索）
  - `POST /api/watchlist` - 添加到待看清单
  - `PUT /api/watchlist/{id}/watched` - 标记为已看（可选评分）
  - `DELETE /api/watchlist/{id}` - 从待看清单移除
  - `GET /api/watchlist/{id}` - 获取单个条目
  - `GET /api/watchlist/stats` - 获取待看统计
  - `GET /api/watchlist/pending` - 获取待看电影
  - `GET /api/watchlist/watched` - 获取已看电影

### 5. 用户设置模块 (user & settings)
- **NotificationSettings 实体**：通知设置（待看提醒、新推荐通知、周摘要）
- **用户 API**：
  - `GET /api/users/me` - 获取当前用户信息
  - `PUT /api/users/me` - 更新用户信息
  - `GET /api/users/me/stats` - 获取用户统计
- **设置 API**：
  - `GET /api/settings/notifications` - 获取通知设置
  - `PUT /api/settings/notifications` - 更新通知设置
  - `DELETE /api/settings/data` - 清除用户数据

### 6. 推荐模块 (recommendation)
- **RecommendationEvent / RecommendationItem 实体**：推荐事件、候选项、本地得分、反馈状态
- **推荐 API**：
  - `GET /api/recommendations` - 获取当前用户推荐列表（本地规则打分）
  - `POST /api/recommendations/feedback` - 提交反馈（ADOPTED/REJECTED/IGNORED）
- **当前实现策略**：
  - 基于用户评分历史构建类型/标签偏好权重
  - 过滤已评分与已在待看清单的电影
  - 使用 `genre/tag/externalRating` 组合做本地评分
  - 记录推荐事件并支持反馈闭环（采纳可自动入待看）

### 7. 内容模块 (content)
- **MovieRawData 实体**：存储抓取的原始电影数据
- **MovieFeature 实体**：存储提取后的情感/主题/氛围/风格特征
- **内容 API**：
  - `POST /api/content/ingestion` - 抓取电影原始数据并落库
  - `POST /api/content/features/analyze/{movieId}` - 触发单片特征分析
  - `POST /api/content/features/analyze` - 触发批量特征分析
  - `GET /api/content/features/{movieId}` - 查询电影特征
- **当前实现策略**：
  - 已接入豆瓣建议接口（中国站点）
  - 使用 `rule-based-v1.1` 规则算法提取特征并记录算法版本

## 数据库设计

### 使用 H2 文件数据库
- 位置：`./data/dbfile`
- 自动创建和更新表结构
- 可通过 http://localhost:8080/h2-console 访问控制台

### 核心表结构
- `users` - 用户表
- `movies` - 电影表
- `genres` - 类型表
- `tags` - 标签表
- `ratings` - 评分表
- `watchlist_items` - 待看清单表
- `notification_settings` - 通知设置表
- 关联表：`movie_genres`, `movie_tags`

## 技术栈

- **后端框架**：Spring Boot 4.0.3
- **数据库**：H2 (文件模式)
- **ORM**：Spring Data JPA
- **安全**：Spring Security
- **构建工具**：Maven

## API 响应格式

所有 API 响应都使用统一的格式：
```json
{
  "message": "操作成功",
  "data": {
    // 具体数据
  }
}
```

分页响应：
```json
{
  "message": "成功",
  "data": {
    "content": [...],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false,
    "empty": false
  }
}
```

## 运行说明

### 启动后端服务
```bash
cd moviemind-server
mvn spring-boot:run
```
服务将运行在 http://localhost:8080

### H2 控制台
访问 http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/dbfile`
- Username: `sa`
- Password: (空)

### 测试数据
后端会自动初始化示例数据，包括：
- 23种电影类型（动作、喜剧、剧情等）
- 32个标签（搞笑、治愈、伤感等）
- 10部示例电影

## 前后端对接

前端（http://localhost:3000）已配置好与后端的对接：
- CORS 已配置允许前端访问
- API 路径与前端请求一致
- 数据结构匹配前端接口

## 下一步计划

1. **推荐系统增强**
   - 用户画像服务升级（引入更多行为特征）
   - 候选召回策略扩展（多路召回）
   - LLM rerank 集成（替换当前本地最终排序）

2. **内容分析增强**
   - 多数据源爬虫/开放接口（猫眼、腾讯视频等）
   - AI/LLM 特征提取增强（替换当前规则提取）
   - 内容特征质量评估与重算流程

3. **优化功能**
   - 缓存机制
   - 数据库索引优化
   - API 性能监控
