FROM nginxinc/nginx-unprivileged:stable-alpine

COPY --chown=nginx:nginx FrontEnd/ /usr/share/nginx/html/
COPY --chown=nginx:nginx deploy/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 8080
