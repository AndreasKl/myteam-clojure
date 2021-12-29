(defproject people-ware "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [compojure "1.6.2"]
                 [ring/ring-jetty-adapter "1.9.4"]
                 [ring/ring-defaults "0.3.3"]
                 [com.github.seancorfield/honeysql "2.1.833"]
                 [com.github.seancorfield/next.jdbc "1.2.761"]
                 [org.postgresql/postgresql "42.3.1"]]
  :plugins [[lein-ring "0.12.6"]]
  :ring {:handler people-ware.handler/app}
  :profiles
  {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring/ring-mock "0.4.0"]]}})
