(defproject org.clojars.pet/yada-component "0.0.1-SNAPSHOT"
  :description "A yada webservice component."
  :url "https://github.com/mike706574/yada-component.git"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"
            :distribution :repo
            :comments "same as Clojure"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.8.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [aleph "0.4.1"]
                 [yada "1.2.0"]
                 [bidi "2.0.16"]]
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :profiles {:dev {:source-paths ["dev"]
                   :target-path "target/dev"
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [clj-http "3.4.1"]
                                  [org.clojure/data.json "0.2.6"]]}}
  :repl-options {:init-ns user})
