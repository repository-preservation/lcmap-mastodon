#!/bin/bash

INDEX=/usr/share/nginx/html/index.html

sed -i "s!<ARDHOST>!${ARD_HOST}!g" ${INDEX}
sed -i "s!<IWDSHOST>!${IWDS_HOST}!g" ${INDEX}
sed -i "s!<INGESTHOST>!${ARD_HOST}!g" ${INDEX}
sed -i "s!<INGESTPARTITIONING>!${PARTITION_LEVEL}!g" ${INDEX}

nohup java -jar /usr/local/bin/lcmap-mastodon-1.0.0-standalone.jar &
/usr/sbin/nginx -g 'daemon off;'

