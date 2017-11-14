checkdeps:
	lein deps

repl:
	@echo "use 'lein figwheel'"

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

