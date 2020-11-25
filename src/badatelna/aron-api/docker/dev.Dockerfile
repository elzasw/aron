# syntax = docker/dockerfile:experimental

## BUILD image ##
FROM adoptopenjdk:11-jre-hotspot as builder
WORKDIR /build

# Copy files
COPY ./aron-api/build/libs/aron-api-0.0.1.jar ./app.jar

# Extract layers
RUN java -Djarmode=layertools -jar ./app.jar extract

## RUN Image ##
FROM adoptopenjdk:11-jre-hotspot
WORKDIR /app

COPY --from=builder /build/dependencies /app
COPY --from=builder /build/snapshot-dependencies /app
COPY --from=builder /build/spring-boot-loader /app
COPY --from=builder /build/application /app
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]


