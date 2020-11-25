# syntax = docker/dockerfile:experimental

## BUILD image ##
FROM gradle:6.7.0-jdk11 AS builder
WORKDIR /build

# COPY config files
COPY ./aron-api/docker/global.settings.gradle ./settings.gradle
COPY ./aron-api/docker/global.build.gradle ./build.gradle

# Copy files
COPY ./entity-views ./entity-views
COPY ./common ./common
COPY ./aron-api ./aron-api

# Run build
RUN \
    --mount=type=cache,id=gradle,target=/root/.gradle \
    --mount=type=cache,id=gradle,target=/home/gradle/.gradle \

    --mount=type=cache,id=gradle-entity-views,target=/build/entity-views/.gradle \
    --mount=type=cache,id=gradle-entity-views-build,target=/build/entity-views/build \

    --mount=type=cache,id=gradle-entity-views-processor,target=/build/entity-views/entity-views-processor/.gradle \
    --mount=type=cache,id=gradle-entity-views-processor-build,target=/build/entity-views/entity-views-processor/build \

    --mount=type=cache,id=gradle-entity-views-api,target=/build/entity-views/entity-views-api/.gradle \
    --mount=type=cache,id=gradle-entity-views-api-build,target=/build/entity-views/entity-views-api/build \

    --mount=type=cache,id=gradle-common,target=/build/common/.gradle \
    --mount=type=cache,id=gradle-common-build,target=/build/common/build \

    --mount=type=cache,id=gradle-aron-api-root,target=/build/.gradle \
    --mount=type=cache,id=gradle-aron-api,target=/build/aron-api/.gradle \
    --mount=type=cache,id=gradle-aron-api-build,target=/build/aron-api/build \

    gradle --no-daemon build

# extract layers
RUN --mount=type=cache,id=gradle-aron-api-build,target=/build/aron-api/build \
    java -Djarmode=layertools -jar ./aron-api/build/libs/aron-api-0.0.1.jar extract

## RUN Image ##
FROM adoptopenjdk:11-jre-hotspot
WORKDIR /app

COPY --from=builder /build/dependencies /app
COPY --from=builder /build/snapshot-dependencies /app
COPY --from=builder /build/spring-boot-loader /app
COPY --from=builder /build/application /app
ENTRYPOINT ["java", "org.springframework.boot.loader.JarLauncher"]


