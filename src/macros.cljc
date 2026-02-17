(ns macros)

(defmacro ocall
  [obj method & args]
  `(let [obj# ~obj
         m# ~(name method)
         f# (js/goog.object.get obj# m#)]
     (if (fn? f#)
       (.apply f# obj# (cljs.core/array ~@args))
       (throw (js/Error. (str "Method " m# " is not a function (is it a property?)"))))))


(defmacro oget
  "Property access for WASM objects that hides behind prototypes."
  [obj prop]
  `(js/goog.object.get ~obj ~(name prop)))
