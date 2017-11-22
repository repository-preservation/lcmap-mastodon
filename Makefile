checkdeps:
	lein deps

repl:
	@echo "use 'lein figwheel'"

replincs:
	@echo "(require '[lcmap.mastodon.http :as mhttp] '[lcmap.mastodon.core :as mcore] '[lcmap.mastodon.data :as testdata])"
	@echo "(require '[cljs.core.async :refer [<! >! chan pipeline]])"
	@echo "(require-macros '[cljs.core.async.macros :refer [go go-loop]])"

runtests:
	lein doo phantom test once

build-min:
	lein cljsbuild once min

clean:
	rm -rf resources/public/js

docker-build:
	docker build -t usgseros/lcmap-mastodon

faux-ard-container:
	cd resources/nginx; docker build -t faux-ard .

