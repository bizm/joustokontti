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
    [byte-streams :as bs])
  (:import (java.net NetworkInterface)))

(def metadata-uri (System/getenv "ECS_CONTAINER_METADATA_URI"))

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
  (str content "\n--\n" ip-address))

(defn hello-world-handler
  [req]
  (trace "hello-world-handler" req)
  {:status 200
   :headers {"content-type" "text/plain"}
   :body (body "Howdy universe!")})

(defn not-found-handler
  [req]
  (trace "not-found-handler" req)
  {
    :status 404
    :body (body "No such page :`(")})

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/hello"         [] hello-world-handler)
      (route/not-found         not-found-handler))))

(defn start
  [port]
  (http/start-server handler {:port port})
  (println "Joustokontti started at port" port)
  (println "IP: " ip-address)
  (println "metadata uri: " metadata-uri)
  (println (-> @(http/get metadata-uri) :body bs/to-string))
  (println "--")
  (newline))

(defn -main [& args]
  (let [port (if (seq args) (Integer/parseInt (first args)) 8080)]
    (start port)))
