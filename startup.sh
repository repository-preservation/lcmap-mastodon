#!/bin/bash
nohup java -jar /usr/local/bin/lcmap-mastodon-0.1.13-standalone.jar &
/usr/sbin/nginx -g 'daemon off;'
