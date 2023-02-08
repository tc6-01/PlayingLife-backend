# 镜像依赖
FROM maven:3.8.1-jdk-8-slim as builder

# Copy local code to the container image.
# 工作目录
WORKDIR /app
# 项目中用到的文件进行复制
COPY pom.xml .
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
CMD ["java","-jar","/app/target/user-center-back-end-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]