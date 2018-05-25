#!/bin/bash

export CHIPMUNK_HOST=http://`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' resources_chipmunk_1`:5656
export ARD_HOST=http://192.168.43.5
#export AUX_HOST=https://edclpdsftp.cr.usgs.gov/downloads/lcmap/ancillary/tiled/
export AUX_HOST=http://192.168.43.4/aux/
export PARTITION_LEVEL=10
export ARD_PATH=/data/ard/\{tm,etm,oli_tirs\}/ARD_Tile/*/CU/
export SERVER_TYPE=aux

docker run -p 8080:80 -v /home/caustin/workspace/data:/data -e "SERVER_TYPE=${SERVER_TYPE}" -e "AUX_HOST=${AUX_HOST}" -e "ARD_PATH=${ARD_PATH}" -e "ARD_HOST=${ARD_HOST}" -e "CHIPMUNK_HOST=${CHIPMUNK_HOST}" -e "PARTITION_LEVEL=${PARTITION_LEVEL}" --ip="192.168.43.5" --network resources_lcmap_chipmunk mastodon10



