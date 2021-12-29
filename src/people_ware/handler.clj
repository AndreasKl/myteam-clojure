(ns people-ware.handler
  (:use ring.adapter.jetty)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.coercions :as coercions]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :as ring-json]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [cheshire.core :as json]
            [compojure.response :as response]))

(def db-config
  {
   :dbtype   "postgresql"
   :host     "127.0.0.1"
   :dbname   "test"
   :user     "test"
   :password "test"})

(def db-datasource
  (jdbc/get-datasource db-config))

(defn connect-with-close [consumer]
  (with-open [connection (jdbc/get-connection db-datasource)]
    (consumer connection)))

(defn migrate-database []
  (connect-with-close
    (fn [conn] (jdbc/execute! conn ["CREATE TABLE IF NOT EXISTS people (id SERIAL, first_name TEXT, middle_name TEXT, last_name TEXT)"]))))

(defn load-person [id]
  (connect-with-close
    (fn [conn]
      (jdbc/execute-one! conn ["SELECT * FROM people WHERE id = ?" id] {:builder-fn rs/as-unqualified-lower-maps}))))

(defn load-people []
  (connect-with-close
    (fn [conn]
      (jdbc/execute! conn ["SELECT * FROM people"] {:builder-fn rs/as-unqualified-lower-maps}))))

(defn save-person [person]
  (println person)
  (load-person 1))

(defroutes app-routes
           (context "/people" []
             (GET "/" [_] {:body {:people (load-people)} :headers {"Content-Type" "application/json"}})
             (POST "/" req {:status 201 :body (save-person (:body req)) :headers {"Content-Type" "application/json"}})
             (GET "/:id" [id :<< coercions/as-int]
               (let [person (load-person id)]
                 (if person
                   {:body person :headers {"Content-Type" "application/json"}}
                   {:status 404 :headers {"Content-Type" "text/html"} :body "Not Found"}))))
           (route/not-found "Not Found"))

(def app
  (-> (wrap-defaults #'app-routes api-defaults)
      (ring-json/wrap-json-body {:keywords? true :bigdecimals? true})
      (ring-json/wrap-json-response)))

(defonce server (atom nil))

(defn start []
  (let [jetty (run-jetty app {:port 8080 :join? false})]
    (reset! server jetty)))

(defn stop []
  (.stop @server))

(defn restart [] (stop) (start))

(defn -main []
  (migrate-database)
  (start))
