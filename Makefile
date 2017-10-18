build: clean
	lein cljsbuild once dev prod

repl:
	lein repl

clean:
	rm -rf js

unittest:
	lein doo phantom test once


	
