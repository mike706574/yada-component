(ns yada-component.core
  (:require [com.stuartsierra.component :as component]
            [taoensso.timbre :as log]
            [yada.yada :as yada]
            [bidi.ring :as bidi-ring]
            [aleph.http :as aleph-http]))

(defn- already-started
  [{:keys [id port] :as service}]
  (log/info (str "Service " id " already started on port " port "."))
  service)

(defn- start-service
  [{:keys [id port routes] :as service} routes]
  (log/info (str "Starting " id " on port " port "..."))
  (try
    (let [handler (bidi-ring/make-handler routes)
          server (aleph-http/start-server handler {:port port})]
      (log/info (str "Finished starting."))
      (assoc service :server server))
    (catch java.net.BindException e
      (throw (ex-info (str "Port " port " is already in use.") {:id id
                                                                :port port})))))

(defn- stop-service
  [{:keys [id port server] :as service}]
  (log/info (str "Stopping " id " on port " port "..."))
  (.close server)
  (dissoc service :server))

(defn- already-stopped
  [{:keys [id] :as service}]
  (log/info (str id " already stopped."))
  service)

(defrecord YadaService [id port routes server]
  component/Lifecycle
  (start [this]
    (if server
      (already-started this)
      (start-service this routes)))
  (stop [this]
    (if server
      (stop-service this)
      (already-stopped this))))

(defn yada-service
  [{:keys [port] :as config} routes]
  {:pre [(integer? port)
         (> port 0)
         (vector? routes)]}
  (component/using (map->YadaService (assoc config :routes routes)) []))
