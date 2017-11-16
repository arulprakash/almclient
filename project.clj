(defproject alm "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clojure-future-spec "1.9.0-beta4"]
                 [org.clojure/test.check "0.9.0"]
                 [org.clojure/data.xml "0.0.8"]
                 [clj-http "3.7.0"]
                 [enlive "1.1.6"]
                 ]
  :main ^:skip-aot alm.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
