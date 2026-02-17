(ns build-hooks
  (:require [clojure.java.io :as io]))

(defn copy-wasm
  {:shadow.build/stage :flush}
  [build-state & args]
  (let [source (io/file "src/generated-compat/wasm-bindgen/index_bg.wasm")
        target (io/file "resources/public/generated-compat/wasm-bindgen/index_bg.wasm")]
    (when (.exists source)
      (io/make-parents target)
      (io/copy source target)
      (println "--- WASM copied to public/js ---")))
  build-state)
