FROM centos:centos7

RUN yum update -y

## Install Java
RUN yum install -y java-1.8.0-openjdk.x86_64 java-1.8.0-openjdk-devel.x86_64  java-1.8.0-openjdk-headless.x86_64

## Install NGINX
ENV nginxversion="1.16.1-1" \
    os="centos" \
    osversion="7" \
    elversion="7"

RUN yum install -y wget openssl sed &&\
    yum -y autoremove &&\
    yum clean all &&\
    wget http://nginx.org/packages/$os/$osversion/x86_64/RPMS/nginx-$nginxversion.el$elversion.ngx.x86_64.rpm &&\
    rpm -iv nginx-$nginxversion.el$elversion.ngx.x86_64.rpm

## Install Mastodon
COPY resources/public/index.html /usr/share/nginx/html/index.html
COPY resources/public/js/compiled/mastodon_min.js /usr/share/nginx/html/js/compiled/mastodon_min.js
COPY resources/public/js/jquery.min.js /usr/share/nginx/html/js/jquery.min.js
COPY resources/public/css /usr/share/nginx/html/css/
COPY resources/public/images /usr/share/nginx/html/images/
COPY resources/log4j.properties /log4j.properties
COPY default.conf /etc/nginx/conf.d/default.conf
COPY startup.sh /startup.sh
COPY project.clj /project.clj
COPY target/lcmap-mastodon-*-standalone.jar /

RUN mkdir /data

# Run!
CMD /startup.sh

