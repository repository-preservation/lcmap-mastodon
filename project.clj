(defproject lcmap-mastodon "0.1.0-SNAPSHOT"
  :description "Functions for LCMAP data sources"
  :url "https://eros.usgs.gov"
  :license {:name "Unlicense"
            :url  ""}

  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [cljs-http "0.1.43"]]

  :plugins [[lein-cljsbuild "1.0.5"]]

  :source-paths ["src/cljs"]

  :cljsbuild {:builds  {:dev  {:source-paths ["src/cljs"]
                               :compiler     {:output-to     "js/lcmap-mastodon_dev.js"
                                              :optimizations :whitespace
                                              :pretty-print  true}}

                        :prod {:source-paths ["src/cljs"]
                               :compiler     {:output-to     "js/lcmap-mastodon.js"
                                              :optimizations :simple}}}}

  :profiles {:test {:resource-paths ["test" "test/resources"]} }

  :aliases {"cljs" ["do" "clean," "cljsbuild" "once" "dev"]})
