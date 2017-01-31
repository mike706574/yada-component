(ns user
  "Tools for interactive development with the REPL. This file should
  not be included in a production build of the application."
  (:require
   [clojure.java.io :as io]
   [clojure.java.javadoc :refer [javadoc]]
   [clojure.pprint :refer [pprint]]
   [clojure.reflect :refer [reflect]]
   [clojure.repl :refer [apropos dir doc find-doc pst source]]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as test]
   [clojure.tools.namespace.repl :refer [refresh refresh-all]]
   [clojure.data.json :as json]
   [com.stuartsierra.component :as component]
   [clj-http.client :as http]
   [taoensso.timbre :as log]
   [yada.yada :as yada]
   [bidi.bidi :as bidi]
   [yada-component.core :as core]))

(def config {:id "test" :port 8080})

(defn hello-routes
  []
  ["/hello" (yada/resource
             {:methods
              {:get
               {:produces
                {:media-type "text/plain"
                 :language #{"en" "ja-jp;q=0.9" "it-it;q=0.9"}}
                :response (fn [request]
                            (log/info "Saying hello!")
                            (case (yada/language request)
                              "en" "Hello, world!\n"
                              "it-it" "Buongiorno, mondo!\n"
                              "ja-jp" "Konnichiwa sekai!\n"))}}})])

(defn routes
  []
  ["/api" (-> (hello-routes)
              (yada/swaggered
               {:info {:title "Hello API"
                       :version "1.0"
                       :description "An API"}
                :basePath "/api"})
              (bidi/tag :hello.resources/api))])

(defn system
  [config]
  {:app (core/yada-service config (routes))})

(defonce system
  (system config))

(defn init
  "Creates and initializes the system under development in the Var
  #'system."
  []
  (alter-var-root #'system (constantly (system config)))
  :init)

(defn start
  "Starts the system running, updates the Var #'system."
  []
  (alter-var-root #'system component/start-system)
  :started)

(defn stop
  "Stops the system if it is currently running, updates the Var
  #'system."
  []
  (alter-var-root #'system
                  (fn [s] (when s (component/stop-system s))))
  :stopped)

(defn go
  "Initializes and starts the system running."
  []
  (init)
  (start)
  :ready)

(defn reset
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (refresh :after `go))

(defn restart
  "Stops the system, reloads modified source files, and restarts it."
  []
  (stop)
  (go))

(comment :scratch
  (http/get "http://localhost:8080/api/hello" {:throw-exceptions false})
  (json/read-str (:body (http/get "http://localhost:8080/api/swagger.json" {:throw-exceptions false}))))
