(defproject ont-app/igraph-loom "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/data.priority-map "1.0.0"]
                 ;; ...dependency clarification
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.879"]
                 [aysylu/loom "1.0.2"]
                 [ont-app/graph-log "0.1.4"]

                 ]
  :repl-options {:init-ns ont-app.igraph-loom})
