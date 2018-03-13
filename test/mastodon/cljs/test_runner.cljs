(ns mastodon.cljs.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [mastodon.cljs.core-test]))

(enable-console-print!)

(doo-tests 'mastodon.cljs.core-test)
