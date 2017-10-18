(ns lcmap.mastodon.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [lcmap.mastodon.core-test]))

(enable-console-print!)

(doo-tests 'lcmap.mastodon.core-test)
