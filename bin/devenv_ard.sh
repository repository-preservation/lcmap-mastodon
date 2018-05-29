#!/bin/bash

export CHIPMUNK_HOST=http://`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' resources_chipmunk_1`:5656
export ARD_HOST=http://192.168.43.5
#export IWDS_HOST=http://192.168.43.3:5656
export PARTITION_LEVEL=10
export ARD_PATH=/data/ard/\{tm,etm,oli_tirs\}/ARD_Tile/*/CU/
export SERVER_TYPE=ard
export NEMO_RESOURCE=http://127.0.0.1:5757/inventory_by_tile?tile=

docker run -p 8080:80 -v /home/caustin/workspace/data:/data -e "SERVER_TYPE=${SERVER_TYPE}" -e "ARD_PATH=${ARD_PATH}" -e "ARD_HOST=${ARD_HOST}" -e "CHIPMUNK_HOST=${CHIPMUNK_HOST}" -e "PARTITION_LEVEL=${PARTITION_LEVEL}" --ip="192.168.43.5" --network resources_lcmap_chipmunk mastodon10



