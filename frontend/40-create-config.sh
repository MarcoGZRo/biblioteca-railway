#!/bin/sh
set -eu
: "${API_BASE_URL:=http://localhost:8080}"
envsubst '${API_BASE_URL}' < /usr/share/nginx/html/config.js.template > /usr/share/nginx/html/config.js
