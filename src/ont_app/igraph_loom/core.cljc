(ns ont-app.igraph-loom.core
  (:require
   [loom.graph :as loom]
   [ont-app.igraph.core :as igraph :refer :all]
   [ont-app.igraph.graph :as native-normal]
   [ont-app.graph-log.levels :refer :all]
   ))

(defn cleanup
  "Returns `lg`, with orphaned `nodes` removed
  Where
  - `lg` is a loom graph
  - `nodes` := #{`node` ...}. Optional, defaulting to (nodes lg)
  - `node` names a node in `lg`
  "
  ([lg]
   (cleanup lg (loom/nodes lg))
   )
  ([lg nodes]
   (apply loom/remove-nodes
          lg
          (filter (fn [n] (and (empty? (loom/in-edges lg n))
                               (empty? (loom/out-edges lg n))))
                  nodes))))

(declare get-subjects)
(declare do-get-p-o)
(defn get-normal-form
  "Returns all the edges in the `loom-graph`, rendered in normal form, using identifiers for nodes and edge properties where they can be inferred, otherwise using default identifiers"
  [loom-graph]
  (let [collect-p-o (fn [s acc p o]
                      (let [po (or (acc s) {})]
                        (assoc acc s (assoc po p o))))
        collect-s (fn [acc s]
                    (reduce-kv
                     (partial collect-p-o s)
                     acc           
                     (do-get-p-o loom-graph s)))

        ]
    (reduce collect-s {} (get-subjects loom-graph))))

    
(defn get-subjects
  "Returns (`node-id`, ... ) in the `loom-graph`
  Where
  - `node-id` idenfies some `node` in `loom-graph` which serves as the :src in some `edge`
  - `loom-graph` implements one of Loom's graph protocols
  - `node` is the actual member of (nodes `loom-graph`)
  - `edge` implements the Loom Edge protocol
  "
  [loom-graph]
  (info ::StartingGetSubjects
        :loom-graph loom-graph)
  (let [history (atom #{})
        new? (fn [x] (not (@history x)))
        record (fn [node-id]
                 (swap! history conj node-id)
                 node-id)
        ]
    (map record (filter new? (map loom/src (-> loom-graph (loom/edges)))))))


(defn do-get-p-o
  "Returns normal descriptions of the node referenced by `s` in `loom-graph`
  Where
  - `s` identifies some `node` in `loom-graph`
  - `node` is a node in `loom-graph`
  - `loom-graph` implements one of Loom's graph protocols  
  "
  [loom-graph s]
  (let [collect-o (fn [p acc o]
                    (let [os (or (acc p) #{})]
                      (assoc acc p (conj os o))))
        ]
    (reduce (partial collect-o :adj)
            {}
            (-> loom-graph
                (loom/successors s)))))


(defn do-get-o
  "Returns #{`object`, ...} for `s` and `p` in `loom-graph`
  Where
  - `object` :~ #{`node-id` `literal`}
  - `s` identifies a `subject-node` in `loom-graph`
  - `p` identifies a `property-id` pertaining to an `edge` in `loom-graph`
  - `loom-graph` implements one of Loom's graph protocols
  - `node-id` identifies `object` if it is a :dest of `edge`
  - `literal` is an attriubte of of `subject-node` which is not a node in `loom-graph`
  - `subject-node` is the :src of `edge`
  - `property-id` identifies the property pertinent to `edge`.
  - `edge` joins `subject-node` and `object` in `loom-graph`
  "
  
  [loom-graph s p]
  (let [collect-o (fn [p acc o]
                    (let [os (or (acc p) #{})]
                      (assoc acc p (conj os o))))
        ]
    (when (not (= p :adj))
      (throw (ex-info "Need to support property names"
                      {:type ::NeedToSupportPropertyNames
                       :loom-graph loom-graph
                       :s s
                       :p p
                       })))
    (-> loom-graph
        (loom/successors s))))
  

(defn do-ask
  "Returns #`edge` for `s` and `p` and `o`  in `loom-graph`
  Where
  - `object` :~ #{`node-id` `literal`}
  - `s` identifies a `subject-node` in `loom-graph`
  - `p` identifies a `property-id` pertaining to an `edge` in `loom-graph`
  - `o` identifies the :dest in `edge`
  - `loom-graph` implements one of Loom's graph protocols
  - `node-id` identifies `object` if it is a :dest of `edge`
  - `literal` is an attriubte of of `subject-node` which is not a node in `loom-graph`
  - `subject-node` is the :src of `edge`
  - `property-id` identifies the property pertinent to `edge`.
  "
  
  [loom-graph s p o]
  ((loom/successors loom-graph s) o))


(defn query-graph
  [loom-graph q]
  "Returns [`binding`, ...] for `q` posed to `loom-graph`
  Where
  - `binding` := {<key> <value>, ...} as specified in `q`
  - `q` is a query in some as-yet unspecified format (watch this space)
  - `loom-graph` implements one of Loom's graph protocols
 "
  (as-> loom-graph it
    (get-normal-form it)
    (native-normal/make-graph :contents it)
    (native-normal/query-graph it q)))



(defrecord LoomGraph [loom-graph]

  igraph/IGraph
  (normal-form [_] (get-normal-form loom-graph)) 
  (subjects [_] (get-subjects loom-graph))
  (get-p-o [_ s] (do-get-p-o loom-graph s))
  (get-o [_ s p] (do-get-o loom-graph s p))
  (ask [_ s p o] (do-ask loom-graph s p o))
  (query [_ q] (query-graph loom-graph q))
  (mutability [_] ::igraph/immutable)
  
  #?(:clj clojure.lang.IFn
     :cljs cljs.core/IFn)
  (invoke [g] (normal-form g))
  (invoke [g s] (get-p-o g s))
  (invoke [g s p] (match-or-traverse g s p))
  (invoke [g s p o] (match-or-traverse g s p o))

  igraph/IGraphImmutable
  (add [g to-add] (add-to-graph g to-add))
  (subtract [g to-subtract] (remove-from-graph g to-subtract))
  
  ;; igraph/IGraphSet
  ;; (union [g1 g2] (add-to-graph g1 (g2)))
  ;; (intersection [g1 g2] (intersection
  ;;                        (native-normal/make-graph :contents (normal-form g1))
  ;;                        (native-normal/make-graph :contents (normal-form g2))))
  ;; (difference [g1 g2] (remove-from-graph g1 (g2)))
  )

(defn make-loom-graph
  ([]
   (new LoomGraph (loom/digraph)))
  ([loom-graph]
   (new LoomGraph loom-graph)))


(defmethod add-to-graph [loom.graph.BasicEditableDigraph :vector]
  [lg v]
  (let [collect-edge (fn [s acc [p o]]
                       (when (not (= p :adj))
                         (throw (ex-info "Need to support property names"
                                         {:type ::NeedToSupportPropertyNames
                                          :lg lg
                                          :s s
                                          :p p
                                          })))
                       (conj acc [s o]))
          ]
     (apply loom/add-edges
            lg
            (reduce (partial collect-edge (first v))
                    []
                    (partition 2 (rest v))))))

(defmethod add-to-graph [LoomGraph :vector]
  [g v]
  (make-loom-graph
   (add-to-graph (:loom-graph g) v)))

(defmethod add-to-graph [LoomGraph :vector-of-vectors]
  [g vv]
  (make-loom-graph
   (reduce add-to-graph (:loom-graph g) vv)))

(defmethod add-to-graph [LoomGraph :normal-form]
  [g mmo]
  (let [collect-triple (fn [lg s p o]
                         (add-to-graph lg [s p o]))
        ]
    (make-loom-graph
     (reduce-spo collect-triple
                 (:loom-graph g)
                 (native-normal/make-graph :contents mmo)))))


(defmethod remove-from-graph [loom.graph.BasicEditableDigraph :vector]
  [lg v]
  (let [collect-edge (fn [s acc [p o]]
                       (when (not (= p :adj))
                         (throw (ex-info "Need to support property names"
                                         {:type ::NeedToSupportPropertyNames
                                          :lg lg
                                          :s s
                                          :p p
                                          })))
                       (conj acc [s o]))
          ]
     (apply loom/remove-edges
            lg
            (reduce (partial collect-edge (first v))
                    []
                    (partition 2 (rest v))))))

(defmethod remove-from-graph [LoomGraph :vector]
  [g v]
  (make-loom-graph
   (remove-from-graph (:loom-graph g) v)))

(defmethod remove-from-graph [LoomGraph :vector-of-vectors]
  [g vv]
  (make-loom-graph
   (reduce remove-from-graph (:loom-graph g) vv)))

(defmethod remove-from-graph [LoomGraph :normal-form]
  [g mmo]
  (let [collect-triple (fn [lg s p o]
                         (remove-from-graph lg [s p o]))
        ]
    (make-loom-graph
     (reduce-spo collect-triple
                 (:loom-graph g)
                 (native-normal/make-graph :contents mmo)))))

(defmethod remove-from-graph [LoomGraph :underspecified-triple]
  [g ut]
  (info ::StartingRemoveFromGraph
        :g g
        :ut ut)
  (make-loom-graph
   (case (count ut)
     1 (let [[s] ut]
         (loom/remove-nodes (:loom-graph g)
                            [s]))
     2 (let [[s p] ut
             collect-triple (fn [s p vv o]
                              (conj vv [s p o]))
             ]
         (reduce remove-from-graph
                 (:loom-graph g)
                 (reduce (partial collect-triple s p)
                         []
                         (g s p)))))))





