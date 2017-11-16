(ns alm.core
  (:require [clj-http.client :as client]
            [clojure.data.xml :refer :all]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as sgen]
            [alm.steps :refer :all]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html])
  (:import [java.io File])
  (:gen-class))

(def server "http://alm.farmersinsurance.com:8080/qcbin")
(def resource {:auth "/authentication-point/authenticate"
               :session "/rest/site-session"
               :runs "/rest/domains/DEFAULT/projects/QC_SAP/runs"
               :runsteps ""})
(def credentials ["arulprakash_pugazhendi" "aug-2017"])
(def headers {"Accept-Encoding" "gzip,deflate"
              "Host" "alm.farmersinsurance.com:8080"
              "Connection" "Keep-Alive"
              "User-Agent" "Apache-HttpClient/4.1.1 (java 1.5)"
              "Cookie" (str  "JSESSIONID" (str (java.util.UUID/randomUUID)))
              "Cookie2" "$Version=1"
              })

(defn get-sso-cookie
  []
  (let [response (client/get (str server (:auth resource))
                             {:proxy-host "10.148.0.180"
                              :proxy-port 80
                              :basic-auth credentials
                              :headers headers})
        key     (get-in response [:cookies "LWSSO_COOKIE_KEY" :value])]
    (str "LWSSO_COOKIE_KEY=" key ";Path=/;HTTPOnly")))

(defn get-session-cookie
  []
  (let [sso-cookie (get-sso-cookie)
        response (client/post (str server (:session resource))
                              {:proxy-host "10.148.0.180"
                               :proxy-port 80
                               :basic-auth credentials
                               :headers (merge headers {"Cookie" sso-cookie})})
        key     (get-in response [:cookies "QCSession" :value])
        qc-cookie (str "QCSession=" key ";Path=/;HTTPOnly")]
    (str qc-cookie sso-cookie)))

(defn test-url
  []
  (let [session-cookie (get-session-cookie)
        response (client/get (str server "/rest/domains/DEFAULT/projects/QC_SAP/customization/entities/run-step/fields")
                             {:proxy-host "10.148.0.180"
                              :proxy-port 80
                              :basic-auth credentials
                              :headers (merge headers {"Cookie" session-cookie})})]
    (println response)))

(defn get-sample
  []
  (let [session-cookie (get-session-cookie)
        response (client/get (str server "/rest/domains/DEFAULT/projects/QC_SAP/runs/28")
                             {:proxy-host "10.148.0.180"
                              :proxy-port 80
                              :basic-auth credentials
                              :headers (merge headers {"Cookie" session-cookie})})
        ]
    (spit "samplerun.xml" (:body response))))

(defn get-run-id
  [xmlstring]
  (as-> xmlstring x
    (parse x)
    (xml-seq x)
    (filter #(= :Field (:tag %)) x)
    (filter #(= (:Name (:attrs %)) "id") x)
    (:content (first  x))
    (:content (first x))
    (first x)))

(defn create-run
  []
  (let [body (slurp "runs.xml")
        session-cookie (get-session-cookie)
        response (client/post (str server (:runs resource))
                              {:proxy-host "10.148.0.180"
                               :proxy-port 80
                               :basic-auth credentials
                               :headers (merge headers {"Cookie" session-cookie
                                                        "Content-Type" "application/xml"})
                               :body body})
        runid (get-run-id (:body response))]
    (println response)))

(defn prepare-run
  (emit-str (sexp-as-element
             [:Entity {:Type "run-step"}
              [:ChildrenCount 0]
              [:Fields
               (map (fn [m] 
                      [:Field {:Name "desstep-id"} (first m)]
                      [:Field {:Name "status"} "Passed"]
                      [:Field {:Name "actual"} (second m)]) st)]])))

(defn prepare-steps
  []
  (emit-str (sexp-as-element
             [:Entity {:Type "run-step"}
              [:ChildrenCount 0]
              [:Fields
               (map (fn [m] 
                      [:Field {:Name "desstep-id"} (first m)]
                      [:Field {:Name "status"} "Passed"]
                      [:Field {:Name "actual"} (second m)]) st)]])))

(defn create-run-step
  []
  (let [body (slurp "run-steps.xml")
        session-cookie (get-session-cookie)
        response (client/put (str server (:runs resource) "/28/run-steps/801")
                             {:proxy-host "10.148.0.180"
                              :proxy-port 80
                              :basic-auth credentials
                              :headers (merge headers {"Cookie" session-cookie
                                                       "Content-Type" "application/xml"})
                              :body body})]
    (println response)))
