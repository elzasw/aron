FROM node:13.12.0-alpine

RUN apk add --no-cache dos2unix

WORKDIR /build
EXPOSE 80

COPY entrypoint.sh .

RUN dos2unix entrypoint.sh
ENTRYPOINT ["sh", "entrypoint.sh"]