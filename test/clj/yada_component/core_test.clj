(ns yada-component.core-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [clj-http.client :as http]
            [taoensso.timbre :as log]
            [yada.yada :as yada]
            [bidi.bidi :as bidi]
            [clojure.data.json :as json]
            [yada-component.core :as core]))

(def config {:id "test" :port 8081})

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

(defmacro with-system
  [& body]
  `(let [~'system (component/start-system (system config))]
     (try
       ~@body
       (finally (component/stop-system ~'system)))))

(deftest saying-hello
  (with-system
    (testing "should say hello in English by default"
      (let [{:keys [status body]} (http/get "http://localhost:8081/api/hello")]
        (is (= 200 status))
        (is (= "Hello, world!\n" body))))

    (testing "should say hello in English when requested"
      (let [{:keys [status body]} (http/get "http://localhost:8081/api/hello"
                                            {:headers {"Accept-Language" "en"}
                                             :throw-exceptions false})]
        (is (= 200 status))
        (is (= "Hello, world!\n" body))))

    (testing "should say hello in Italian when requested"
      (let [{:keys [status body]} (http/get "http://localhost:8081/api/hello"
                                            {:headers {"Accept-Language" "it-it"}
                                             :throw-exceptions false})]
        (is (= 200 status))
        (is (= "Buongiorno, mondo!\n" body))))

    (testing "should say hello in Japanese when requested"
      (let [{:keys [status body]} (http/get "http://localhost:8081/api/hello"
                                            {:headers {"Accept-Language" "ja-jp"}
                                             :throw-exceptions false})]
        (is (= 200 status))
        (is (= "Konnichiwa sekai!\n" body))))

    (testing "should reject unsupported language"
      (let [{:keys [status body]} (http/get "http://localhost:8081/api/hello"
                                            {:headers {"Accept-Language" "wat"}
                                             :throw-exceptions false})]
        (is (= 406 status))
        (is (= "\r\n\r\n{:status 406}\n" body))))

    (testing "should have swagger"
      (let [{:keys [status body]} (http/get "http://localhost:8081/api/swagger.json"
                                            {:throw-exceptions false})]
        (is (= 200 status))
        (is (= {"swagger" "2.0",
                "info"
                {"title" "Hello API" "version" "1.0" "description" "An API"}
                "produces" ["application/json"]
                "consumes" ["application/json"]
                "paths"
                {"/hello"
                 {"get"
                  {"produces" ["text/plain"]
                   "responses" {"default" {"description" ""}}}}}
                "basePath" "/api"
                "definitions" {}}
               (json/read-str body)))))))
