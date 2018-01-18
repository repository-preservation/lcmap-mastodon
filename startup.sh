#!/bin/bash
INDEX=/usr/share/nginx/html/index.html

sed -i "s!<ARDHOST>!${ARD_HOST}!g" ${INDEX}
sed -i "s!<IWDSHOST>!${IWDS_HOST}!g" ${INDEX}
sed -i "s!<INGESTHOST>!${INGEST_HOST}!g" ${INDEX}

/usr/sbin/nginx -g 'daemon off;'
