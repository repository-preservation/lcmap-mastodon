#!/bin/bash
nohup java -jar orb-0.1.0-SNAPSHOT-standalone.jar &
/usr/sbin/nginx -g 'daemon off;'
