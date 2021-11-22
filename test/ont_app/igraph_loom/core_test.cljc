(ns ont-app.igraph-loom.core_test
  (:require [clojure.test :refer :all]
            [loom.graph :as loom]
            [ont-app.igraph.core :as igraph :refer :all]
            [ont-app.igraph.graph :as native-normal :refer [make-graph]]
            [ont-app.graph-log.levels :refer :all]
            [ont-app.graph-log.core :as glog]
            [ont-app.igraph-loom.core :refer :all]))


(defn log-reset!
  []
  (glog/log-reset!))

(def digraph (loom/digraph))


(def g (-> (make-loom-graph digraph)
           (add [[:bob
                  :knows :george
                  :likes :beer]
                 [:beer
                  :isa :beverage]
                 ])))
                  

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
 
