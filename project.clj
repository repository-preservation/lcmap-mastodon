(defproject lcmap-mastodon "0.1.8"
  :description "Functions for LCMAP data curation"
  :url "https://eros.usgs.gov"
  :license {:name "Unlicense"
            :url ""}

  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.9.0-beta4"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async  "0.3.443"]
                 [cljs-http "0.1.43"]
                 [lein-doo "0.1.8"]]

  :plugins [[lein-figwheel "0.5.14"]
            [lein-doo "0.1.8"]
            [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  ;; https://github.com/emezeske/lein-cljsbuild#hooks
  :hooks [leiningen.cljsbuild]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src" "test"]
                ;; The presence of a :figwheel configuration here will cause figwheel to inject the figwheel client into your build
                :figwheel {:open-urls ["http://localhost:3449/index-dev.html"]}

                :compiler {:main lcmap.mastodon.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/mastodon.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           :preloads [devtools.preload]}}

               ;; This next build is a compressed minified build for production. You can build this with: lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/mastodon_min.js"
                           :main lcmap.mastodon.core
                           :optimizations :advanced
                           :pretty-print true
                           :externs ["resources/public/js/compiled/mastodon_min.js"]}}

               ;; testing build
               {:id "test"
                :source-paths ["src" "test"]
                :compiler {:output-to "resources/public/js/compiled/mastodon_tst.js"
                           :output-dir "resources/public/js/compiled/out/test"
                           :main lcmap.mastodon.test-runner}}]}

  :figwheel {:css-dirs ["resources/public/css"] } ;; watch and update CSS
  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.4"]
                                  [figwheel-sidecar "0.5.13"]
                                  [com.cemerick/piggieback "0.2.2"]]
                   
                   :source-paths ["src" "dev"] ;; need to add dev source path here to get user.clj loaded
                   ;; for CIDER :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   ;; need to add the compliled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["resources/public/js/compiled" :target-path]}



  } ;;profiles
) ;;defproject
