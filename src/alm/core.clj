(ns alm.core
  (:require [alm.common :refer :all]
            [clj-http.client :as client]
            [clojure.data.xml :refer :all]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as html]
            [dk.ative.docjure.spreadsheet :refer :all])
  (:import [java.io File])
  (:gen-class))

(defn read-actuals
  [sheetname]
  (->> (load-workbook "actuals.xlsx")
       (select-sheet sheetname)
       (select-columns {:A :stepno
                        :B :description
                        :C :expected
                        :D :actual })))

(defn prep-steps
  [runid stepid desstep-id actuals]
  (map 
   (fn [desstep m id]
     (let [stepurl (str server (:runs resource) "/" runid "/run-steps/" id)]
       (crud client/put
             stepurl
             (emit-str (sexp-as-element
                        [:Entity {:Type "run-step"}
                         [:ChildrenCount 0]
                         [:Fields
                          [:Field {:Name "desstep-id"} [:Value desstep]]
                          [:Field {:Name "name"} [:Value (:stepno m)]]
                          [:Field {:Name "description"} [:Value (:description m)]]
                          [:Field {:Name "expected"} [:Value (:expected m)]]
                          [:Field {:Name "status"} [:Value "Passed"]]
                          [:Field {:Name "actual"} [:Value (:actual m)]]]])))))
   desstep-id
   actuals
   stepid))

(defn update-run
  [r activity]
  (let [actuals (read-actuals activity)
        runid r
        stepsurl (str server (:runs resource) "/" runid "/run-steps?page-size=250")
        stepid (get-id-list (get-resource stepsurl) "id")
        desstep-id (get-id-list (get-resource stepsurl) "desstep-id")]
    (prep-steps runid stepid desstep-id actuals)))
