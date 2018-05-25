#!/bin/bash

export ARD_HOST=http://192.168.43.5
#export IWDS_HOST=http://192.168.43.4:5656
export CHIPMUNK_HOST=http://`docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' resources_chipmunk_1`:5656

export PARTITION_LEVEL=10



