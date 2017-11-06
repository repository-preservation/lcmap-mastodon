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

