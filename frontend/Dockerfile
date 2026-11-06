FROM nginx:stable-alpine
COPY index.html style.css app.js /usr/share/nginx/html/
COPY config.js.template /usr/share/nginx/html/config.js.template
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY 40-create-config.sh /docker-entrypoint.d/40-create-config.sh
RUN chmod +x /docker-entrypoint.d/40-create-config.sh
ENV PORT=8080
EXPOSE 8080
