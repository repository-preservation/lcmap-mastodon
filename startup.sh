#!/bin/bash

INDEX=/usr/share/nginx/html/index.html
VERSION=`head -n1 project.clj | grep -oP "[\d]+\.[\d]+\.[\d]+(-SNAPSHOT)?(-RC[\echo+])?"`

sed -i "s!<ARDHOST>!${ARD_HOST}!g"    ${INDEX}
sed -i "s!<CHIPMUNKHOST>!${CHIPMUNK_HOST}!g"  ${INDEX}
sed -i "s!<NEMOHOST>!${NEMO_HOST}!g"  ${INDEX}
sed -i "s!<AUXHOST>!${AUX_HOST}!g"    ${INDEX}
sed -i "s!<INGESTHOST>!${ARD_HOST}!g" ${INDEX}
sed -i "s!<INGESTPARTITIONING>!${PARTITION_LEVEL}!g" ${INDEX}

if [ ${DATA_TYPE} == "ard" ]; then
  #uncomment javascript populating year select dropdowns
  sed -i "s/\/\/ARD//g" ${INDEX}
fi

/usr/sbin/nginx -g 'daemon off;' & java -jar /lcmap-mastodon-${VERSION}-standalone.jar
