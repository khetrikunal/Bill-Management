#!/bin/sh
set -eu

if [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  : "${DB_HOST:?DB_HOST is required when SPRING_DATASOURCE_URL is not set}"
  : "${DB_PORT:?DB_PORT is required when SPRING_DATASOURCE_URL is not set}"
  : "${DB_NAME:?DB_NAME is required when SPRING_DATASOURCE_URL is not set}"
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}"
fi

exec java -jar /app/app.jar
