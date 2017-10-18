(defproject lcmap-mastodon "0.1.0-SNAPSHOT"
  :description "Functions for LCMAP data sources"
  :url "https://eros.usgs.gov"
  :license {:name "Unlicense"
            :url  ""}

  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 [org.clojure/core.async "0.3.443"]
                 [org.clojure/clojurescript "0.0-3308"]
                 [cljs-http "0.1.43"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-doo "0.1.8"]
            [lein-figwheel "0.5.13"]]

  ;;:hooks [leiningen.cljsbuild]

  :source-paths ["src"]

  :cljsbuild {:builds {:dev {:source-paths ["src"]
                             :compiler     {:output-to "js/lcmap-mastodon_dev.js"
                                            :optimizations :whitespace
                                            :pretty-print  true}}

                        :prod {:source-paths ["src"]
                               :compiler     {:output-to     "js/lcmap-mastodon.js"
                                              :optimizations :simple}}
                        :test {:source-paths ["src" "test"]
                               :compiler {:main lcmap.mastodon.test-runner
                                          :output-to "js/compiled/mastodon_test.js"
                                          :optimizations :none}
                               }
                       }}

  :profiles {:test {:resource-paths ["test" "test/resources"]} }

  :aliases {"cljs" ["do" "clean," "cljsbuild" "once" "dev"]})

