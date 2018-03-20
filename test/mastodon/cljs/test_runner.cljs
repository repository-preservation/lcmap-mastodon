(ns mastodon.cljs.test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [mastodon.cljs.core-test]
            [mastodon.cljs.dom-test]))

(enable-console-print!)

(doo-tests  'mastodon.cljs.dom-test
            'mastodon.cljs.core-test)
