from nginx:latest

COPY default.conf /etc/nginx/conf.d/default.conf
RUN mkdir -p /usr/share/nginx/ard/005015
COPY touchscript.sh /usr/share/nginx/ard/005015/touchscript.sh
RUN cd /usr/share/nginx/ard/005015;./touchscript.sh
#COPY json/005015.json /usr/share/nginx/ard/005015


