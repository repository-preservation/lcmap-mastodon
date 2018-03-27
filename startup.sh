#!/bin/bash

INDEX=/usr/share/nginx/html/index.html

VERSION=`head -n1 project.clj | grep -oP "[\d]+\.[\d]+\.[\d]+(-SNAPSHOT)?(-RC[\echo+])?"`

sed -i "s!<ARDHOST>!${ARD_HOST}!g" ${INDEX}
sed -i "s!<IWDSHOST>!${IWDS_HOST}!g" ${INDEX}
sed -i "s!<INGESTHOST>!${ARD_HOST}!g" ${INDEX}
sed -i "s!<INGESTPARTITIONING>!${PARTITION_LEVEL}!g" ${INDEX}

nohup java -jar /lcmap-mastodon-${VERSION}-standalone.jar & /usr/sbin/nginx -g 'daemon off;'

