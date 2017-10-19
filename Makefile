build:
	echo "cljs alias is defined in project.clj"
	lein cljs

repl:
	echo "point browser at http://127.0.0.1:9000 ; and open console"
	lein trampoline cljsbuild repl-listen

clean:
	lein do clean

runtests:
	lein doo phantom test once


