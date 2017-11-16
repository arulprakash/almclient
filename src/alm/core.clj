(ns alm.core
  (:require [clj-http.client :as client]
            [clojure.data.xml :refer :all]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as sgen])
  (:gen-class))

(def server "http://alm.farmsersinsurance.com:8080/qcbin")
(def resource {:auth "/authentication-point/authenticate"
               :session ""
               :runs ""
               :runsteps ""})
(def credentials ["arulprakash_pugazhendi" "aug-2017"])
(def headers {"Accept-Encoding" "gzip,deflate"
              "Content-Type" "application/soap+xml;charset=UTF-8"
              "SOAPaction" ""
              "Host" "alm.farmersinsurance.com:8080"
              "Connection" "Keep-Alive"
              "User-Agent" "Apache-HttpClient/4.1.1 (java 1.5)"
              "Cookie" (str  "JSESSIONID" (str (java.util.UUID/randomUUID))
              "Cookie2" "$Version=1"})
(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
(defn read-excel
  [])

(defn prepare-edn
  []
  (let [runsteps (java.io.StringReader. (slurp "resources/runsteps.xml"))]
    (spit "resources/steps.edn" (parse runsteps))))

(defn emit-xml
  [])

(defn get-sso-cookie
  []
  (let [response (client/post (str server (:auth resource))
                 {:basic-auth credentials
                  :headers headers})]
    (println response)))

(defn get-session-cookie
  [])

(defn create-run
  [])

(defn create-run-step
  [])
