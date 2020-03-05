;; https://aleph.io/examples/literate.html#aleph.examples.http
;; https://github.com/ztellman/aleph
;; https://github.com/exoscale/exopaste/
;; https://www.exoscale.com/syslog/clojure-application-tutorial/
;; https://clojurebridge.org/community-docs/docs/getting-started/helloworld/
(ns joustokontti.main
  (:require
    [compojure.core :as compojure :refer [GET]]
    [ring.middleware.params :as params]
    [compojure.route :as route]
    [aleph.http :as http]
    [byte-streams :as bs]
    [cheshire.core :refer :all])
  (:import (java.net NetworkInterface)))

(def metadata-uri (System/getenv "ECS_CONTAINER_METADATA_URI"))

(def hostname (System/getenv "HOSTNAME"))

(def meta-docker
  (try
    (parse-string
      (-> @(http/get metadata-uri) :body bs/to-string) true)
    (catch Exception e nil)))

(def meta-task
  (try
    (parse-string
      (-> @(http/get (str metadata-uri "/task")) :body bs/to-string) true)
    (catch Exception e nil)))

(def ip-address
  (-> (->> (NetworkInterface/getNetworkInterfaces)
         enumeration-seq
         (map bean)
         (mapcat :interfaceAddresses)
         (map bean)
         (filter :broadcast)
         (filter #(= (.getClass (:address %)) java.net.Inet4Address)))
    (nth 0)
    (get :address)
    .getHostAddress))

(defn trace [handler-name req]
  (println handler-name)
  (clojure.pprint/pprint   req)
  (newline))

(defn body [content]
  (str content "\n--\n"
    (if (nil? meta-docker)
      (str "Hostname: " hostname)
      (str "DockerId: " (get meta-docker :DockerId) "\nDockerName: " (get meta-docker :DockerName)))))

(defn hello-world-handler
  [req]
  (trace "hello-world-handler" req)
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (body "Howdy universe!")})

(defn meta-handler
  [req]
  (trace "meta-handler" req)
  (if (nil? meta-docker) {:status 204}
    {:status 202
     :headers {"content-type" "application/json"}
     :body (generate-string meta-docker)}))

(defn meta-task-handler
  [req]
  (trace "meta-task-handler" req)
  (if (nil? meta-task) {:status 204}
    {:status 202
      :headers {"content-type" "application/json"}
      :body (generate-string meta-task)}))

(defn meta-stats-handler
  [req]
  (trace "meta-stats-handler" req)
  (if (nil? metadata-uri) {:status 204}
    {:status 202
      :headers {"content-type" "application/json"}
      :body (-> @(http/get (str metadata-uri "/stats")) :body bs/to-string)}))

(defn meta-task-stats-handler
  [req]
  (trace "meta-task-stats-handler" req)
  (if (nil? metadata-uri) {:status 204}
    {:status 202
      :headers {"content-type" "application/json"}
      :body (-> @(http/get (str metadata-uri "/task/stats")) :body bs/to-string)}))

(defn not-found-handler
  [req]
  (trace "not-found-handler" req)
  {
    :status 404
    :body (body "No such page :`(")})

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/hello"           [] hello-world-handler)
      (GET "/meta"            [] meta-handler)
      (GET "/meta/task"       [] meta-task-handler)
      (GET "/meta/stats"      [] meta-stats-handler)
      (GET "/meta/task/stats" [] meta-task-stats-handler)
      (route/not-found           not-found-handler))))

(defn start
  [port]
  (http/start-server handler {:port port})
  (println "Joustokontti started at port" port)
  (println "IP: " ip-address)
  (println "Hostname: " hostname)
  (println "metadata uri: " metadata-uri)
  (clojure.pprint/pprint meta-docker)
  (println "--")
  (newline))

(defn -main [& args]
  (let [port (if (seq args) (Integer/parseInt (first args)) 8080)]
    (start port)))
