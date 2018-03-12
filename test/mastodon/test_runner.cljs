(ns mastodon.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [mastodon.core-test]))

(enable-console-print!)

(doo-tests 'mastodon.core-test)
