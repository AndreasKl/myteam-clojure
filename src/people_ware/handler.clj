(ns people-ware.handler
  (:use ring.adapter.jetty)
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.coercions :as coercions]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [cheshire.core :as json]
            [compojure.response :as response])
  (:import (org.eclipse.jetty.server Server)))

(def db-config
  {
   :dbtype   "postgresql"
   :host     "127.0.0.1"
   :dbname   "test"
   :user     "test"
   :password "test"})

(def db-datasource (jdbc/get-datasource db-config))

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
             (GET "/" []
               (json/generate-string {:people (load-people)}))
             (POST "/" [request]
               (json/generate-string (save-person (json/parse-string (slurp (:body request))))))
             (GET "/:id" [id :<< coercions/as-int]
               (let [person (load-person id)]
                 (if person
                   (json/generate-string person)
                   {:status 404 :headers {"Content-Type" "text/html"} :body "Not Found"}))))
           (route/not-found "Not Found"))

(def app
  (wrap-defaults #'app-routes site-defaults))

(defonce ^Server server (atom nil))

(defn start []
  (let [jetty (run-jetty app {:port 8080 :join? false})]
    (reset! server jetty)))

(defn stop []
  (.stop @server))

(defn restart [] (stop) (start))

(defn -main []
  (migrate-database)
  (start))
