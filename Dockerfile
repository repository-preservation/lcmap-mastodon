from nginx:latest
MAINTAINER USGS LCMAP http://eros.usgs.gov

RUN mkdir /usr/share/man/man1
RUN apt-get update
RUN apt-get install default-jdk curl vim -y

COPY resources/public/index.html /usr/share/nginx/html/index.html
COPY resources/public/js/compiled/mastodon_min.js /usr/share/nginx/html/js/compiled/mastodon_min.js
COPY resources/public/js/jquery.min.js /usr/share/nginx/html/js/jquery.min.js
COPY resources/public/css /usr/share/nginx/html/css/
COPY resources/public/images /usr/share/nginx/html/images/
COPY default.conf /etc/nginx/conf.d/default.conf
COPY startup.sh /usr/local/bin/startup.sh
COPY target/lcmap-mastodon-0.1.13-standalone.jar /usr/local/bin/

RUN mkdir /data

CMD /usr/local/bin/startup.sh
