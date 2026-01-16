# Dockerfile
FROM openjdk:17-jdk-slim as builder
WORKDIR /D04-invoice-extraction
COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /D04-invoice-extraction

# 安装Tesseract OCR（用于降级方案）
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-chi-sim \
    libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

# 复制应用
COPY --from=builder /D04-invoice-extraction/target/D04-invoice-extraction-0.0.1-SNAPSHOT.jar app.jar
COPY --from=builder /D04-invoice-extraction/tessdata /usr/share/tessdata

# 创建非root用户
RUN useradd -m -u 1000 spring
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.ai.zhipuai.api-key=${ZHIPU_KEY}"]