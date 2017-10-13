(require 'cljs.build.api)

(cljs.build.api/build "src" 
    {:main 'mastodon.core
     :output-to "out/main.js"}
)
