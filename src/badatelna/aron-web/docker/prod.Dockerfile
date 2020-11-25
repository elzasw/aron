# syntax = docker/dockerfile:experimental

## BUILD image ##
FROM node:13.12.0-alpine AS builder
WORKDIR /build

# Switch to yarn 2
RUN yarn set version berry

# Copy package files
COPY ./package.json ./yarn.lock ./
COPY ./common-web/package.json ./common-web/package.json
COPY ./aron-web/package.json ./aron-web/package.json
COPY ./aron-web/docker/.yarnrc.yml ./.yarnrc.yml

# RUN echo "" > .yarnrc.yml

# Install dependencies
RUN yarn install

# Copy and build common
COPY ./common-web ./common-web
RUN yarn workspace @eas/common-web build

# Copy and build app
COPY ./aron-web ./aron-web
RUN yarn workspace @eas/aron-web build


## RUN Image ##
FROM httpd:alpine

# Apache conf
COPY ./aron-web/docker/httpd.conf /usr/local/apache2/conf/httpd.conf

COPY --from=builder /build/aron-web/dist/ /usr/local/apache2/htdocs/
COPY ./aron-web/docker/.htaccess /usr/local/apache2/htdocs/
