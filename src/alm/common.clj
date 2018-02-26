(ns alm.common
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
(def resource {:auth      "/authentication-point/authenticate"
               :session   "/rest/site-session"
               :runs      "/rest/domains/DEFAULT/projects/QC_SAP/runs"
               :sets "/rest/domains/DEFAULT/projects/QC_SAP/test-sets"
               :runsteps  ""
               :instances "/rest/domains/DEFAULT/projects/QC_SAP/test-instances"})
(def credentials ["arulprakash_pugazhendi" "aug-2017"])
(def headers {"Accept-Encoding" "gzip,deflate"
              "Host"            "alm.farmersinsurance.com:8080"
              "Connection"      "Keep-Alive"
              "User-Agent"      "Apache-HttpClient/4.1.1 (java 1.5)"
              "Cookie"          (str "JSESSIONID" (str (java.util.UUID/randomUUID)))
              "Cookie2"         "$Version=1"})

(defn get-sso-cookie
  []
  (let [response (client/get (str server (:auth resource))
                             {:proxy-host "10.148.0.180"
                              :proxy-port 80
                              :basic-auth credentials
                              :headers    headers})
        key (get-in response [:cookies "LWSSO_COOKIE_KEY" :value])]
    (str "LWSSO_COOKIE_KEY=" key ";Path=/;HTTPOnly")))

(defn get-session-cookie
  []
  (let [sso-cookie (get-sso-cookie)
        response (client/post (str server (:session resource))
                              {:proxy-host "10.148.0.180"
                               :proxy-port 80
                               :basic-auth credentials
                               :headers    (merge headers {"Cookie" sso-cookie})})
        key (get-in response [:cookies "QCSession" :value])
        qc-cookie (str "QCSession=" key ";Path=/;HTTPOnly")]
    (str qc-cookie sso-cookie)))

(def session-cookie (atom (get-session-cookie)))

(defn get-resource
  [url]
  (try
    (let [response (client/get url
                               {:proxy-host "10.148.0.180"
                                :proxy-port 80
                                :basic-auth credentials
                                :headers    (merge headers {"Cookie" @session-cookie})})] 
      (:body response))
    (catch Exception e 
      (reset! session-cookie (get-session-cookie))
      (get-resource url))))

(defn get-sample
  []
  (let [response (client/get (str server "/rest/domains/DEFAULT/projects/QC_SAP/runs/875")
                             {:proxy-host "10.148.0.180"
                              :proxy-port 80
                              :basic-auth credentials
                              :headers    (merge headers {"Cookie" @session-cookie})})]
    (spit "samplerun.xml" (:body response))))

(defn get-id
  [xmlstring parm]
  (as-> (java.io.StringReader. xmlstring) x
    (parse x)
    (xml-seq x)
    (filter #(= :Field (:tag %)) x)
    (filter #(= (:Name (:attrs %)) parm) x)
    (:content (first x))
    (:content (first x))
    (first x)))

(defn get-id-list
  [xmlstring parm]
  (as-> (java.io.StringReader. xmlstring) x
    (parse x)
    (xml-seq x)
    (filter #(= :Field (:tag %)) x)
    (filter #(= (:Name (:attrs %)) parm) x)
    (map #(:content (first (:content %))) x)
    (flatten x)))

(defn crud
  [method url body]
  (:status (method url
                   {:proxy-host "10.148.0.180"
                    :proxy-port 80
                    :basic-auth credentials
                    :headers    (merge headers {"Cookie"       @session-cookie
                                                "Content-Type" "application/xml"})
                    :body       body})))

(defn crud-response
  [method url body]
  (:body (method url
                 {:proxy-host "10.148.0.180"
                  :proxy-port 80
                  :basic-auth credentials
                  :headers    (merge headers {"Cookie"       @session-cookie
                                              "Content-Type" "application/xml"})
                  :body       body})))

(defn update-run-pass
  [runid]
  (let [body (emit-str (sexp-as-element
                        [:Entity {:Type "run"}
                         [:ChildrenCount 0]
                         [:Fields
                          [:Field {:Name "status"} [:Value "Passed"]]]]))
        url (str server (:runs resource) "/" runid)]
    (crud client/put url body)))

(defn update-run-cycle
  [run-id cycle-id]
  (try
    (let [body (emit-str (sexp-as-element
                          [:Entity {:Type "run"}
                           [:ChildrenCount 0]
                           [:Fields
                            [:Field {:Name "assign-rcyc"} [:Value cycle-id]]]]))
          url (str server (:runs resource) "/" run-id)]
      (crud client/put url body))
    (catch Exception e)))
