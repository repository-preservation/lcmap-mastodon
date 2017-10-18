build: clean
	lein cljsbuild once

repl:
	lein repl

clean:
	rm -rf js

unittest:
	lein doo phantom test once


	
