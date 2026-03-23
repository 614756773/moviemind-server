# MovieMind Server

## 本地启动

```bash
mvn spring-boot:run
```

默认端口为 `8080`，可通过环境变量覆盖：

```bash
SERVER_PORT=8081 mvn spring-boot:run
```

## 数据库配置

项目默认使用 H2 文件数据库（`./data/dbfile`），可直接启动。

如果启动时报数据库连接错误，可通过环境变量改为你自己的数据库配置：

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/moviemind \
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver \
SPRING_DATASOURCE_USERNAME=root \
SPRING_DATASOURCE_PASSWORD=your_password \
SPRING_JPA_DATABASE_PLATFORM=org.hibernate.dialect.MySQLDialect \
SPRING_JPA_HIBERNATE_DDL_AUTO=update \
mvn spring-boot:run
```

## H2 控制台

1. 启动程序后访问 http://localhost:8080/h2-console  
2. 用户名：`sa`  
3. 密码：空  
4. JDBC URL 与 [application.properties](src/main/resources/application.properties) 中 `spring.datasource.url` 一致。  

## 内容特征提取算法（当前实现）

当前使用 `rule-based-v1.1` 规则算法：

1. 将电影标题、类型、标签、简介、抓取原始文本合并为分析语料。  
2. 用加权情感词典计算情感分：  
   - `sentiment = (positiveWeight - negativeWeight) / (positiveWeight + negativeWeight)`  
3. 用主题/氛围/风格词典做关键词匹配提取特征标签。  
4. 将结果写入 `movie_features`，并记录 `algorithmVersion` 便于后续升级对比。  
