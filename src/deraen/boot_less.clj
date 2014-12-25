(ns deraen.boot-less
  {:boot/export-tasks true}
  (:require
   [clojure.java.io :as io]
   [boot.pod        :as pod]
   [boot.core       :as core]
   [boot.util       :as util]
   [boot.tmpdir     :as tmpd]))

(def ^:private deps
  '[[org.webjars/webjars-locator "0.19"]
    [org.slf4j/slf4j-nop "1.7.7"]
    [slingshot "0.12.1"]])

(defn- find-mainfiles [fs]
  (->> fs
       core/input-files
       (core/by-ext [".main.less"])))

(core/deftask less
  "Compile Less code."
  []
  (let [output-dir  (core/temp-dir!)
        p           (-> (core/get-env)
                        (update-in [:dependencies] into deps)
                        pod/make-pod
                        future)
        last-less   (atom nil)]
    (core/with-pre-wrap fileset
      (let [less (->> fileset
                      (core/fileset-diff @last-less)
                      core/input-files
                      (core/by-ext [".less"]))]
        (reset! last-less fileset)
        (when (seq less)
          (util/info "Compiling {less}... %d changed files.\n" (count less))
          (doseq [f (find-mainfiles fileset)]
            (pod/with-call-in @p
              (deraen.boot-less.impl/less-compile
                ~(.getPath (tmpd/file f))
                ~(.getPath output-dir)
                ~(tmpd/path f))))))
        (-> fileset
            (core/add-resource output-dir)
            core/commit!))))
