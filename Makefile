TAG:=`head -n 1 project.clj | grep -o '[0-9]*\.[0-9]*\.[0-9]*'`
IMAGE:=usgseros/lcmap-mastdon

checkdeps:
	lein deps

repl:
	@echo "use 'lein figwheel'"

replincs:
	@echo "(require '[mastodon.http :as mhttp] '[mastodon.core :as mcore] '[mastodon.data :as testdata])"
	@echo "(require '[cljs.core.async :refer [<! >! chan pipeline]])"
	@echo "(require-macros '[cljs.core.async.macros :refer [go go-loop]])"

runtests:
	lein doo phantom test once
	lein test	

build-min:
	lein cljsbuild once min

clean:
	rm -rf resources/public/js

docker-build: build-min
	docker build -t $(IMAGE):$(TAG) .

docker-push:
	docker push $(IMAGE):$(TAG)

faux-ard-container:
	cd resources/nginx; docker build -t faux-ard .

chipmunkip:
	docker inspect -f "{{ .NetworkSettings.Networks.resources_lcmap_chipmunk.IPAddress }}" ${NAME}

deps-up-d:
	docker-compose -f resources/docker-compose.yml up -d cassandra
	sleep 20
	docker-compose -f resources/docker-compose.yml up -d chipmunk
	sleep 10
	bin/seed
	docker-compose -f resources/docker-compose.yml up -d nemo

deps-down:
	docker-compose -f resources/docker-compose.yml down





