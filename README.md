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
