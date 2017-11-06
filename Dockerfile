from nginx:latest

COPY resources/public/index.html /usr/share/nginx/html/index.html
COPY resources/public/js/compiled/mastodon_min.js /usr/share/nginx/html/js/compiled/mastodon_min.js
COPY resources/public/css /usr/share/nginx/html/css/


