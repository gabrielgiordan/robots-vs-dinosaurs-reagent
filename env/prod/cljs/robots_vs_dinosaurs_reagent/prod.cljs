(ns robots-vs-dinosaurs-reagent.prod
  (:require
    [robots-vs-dinosaurs-reagent.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
