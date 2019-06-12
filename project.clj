(defproject robots-vs-dinosaurs-reagent "0.1.0-SNAPSHOT"
  :description "Robots vs Dinosaurs Reagent"
  :url "http://robots-vs-dinosaurs-reagent.herokuapp.com"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.8.1"]
                 [cljs-ajax "0.8.0"]]
  :source-paths ["src/clj"]
  :plugins [[lein-less "1.7.5"]]
  :profiles {:dev {:source-paths ["src" "env/dev/clj"]
                   :dependencies [[binaryage/devtools "0.9.10"]
                                  [com.google.javascript/closure-compiler-unshaded "v20190325"]
                                  [org.clojure/google-closure-library "0.0-20190213-2033d5d9"]
                                  [figwheel-sidecar "0.5.18"]
                                  [nrepl "0.6.0"]
                                  [ring/ring-core "1.7.0-RC1"]
                                  [thheller/shadow-cljs "2.8.39"]
                                  [cider/piggieback "0.4.1"]]}})
