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
    [aleph.http :as http]))

(defn hello-world-handler
  [req]
  {:status 200
   :headers {"content-type" "text/plain"}
   :body "howdy universe!"})

(def handler
  (params/wrap-params
    (compojure/routes
      (GET "/hello"         [] hello-world-handler)
      (route/not-found "No such page."))))

(defn start
  []
  (http/start-server handler {:port 10000})
  (println "Joustokontti started at port 10000"))

(defn -main [& args]
  (start))
