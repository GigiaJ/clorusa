(ns client.ui
  (:require [reagent.core :as r]
            [client.timeline :as timeline])
  (:require-macros [macros :refer [ocall oget]]))

(defn login-screen [on-login-trigger]
  (let [fields (r/atom {:hs (or js/process.env.MATRIX_HOMESERVER "") :user "" :pass ""})]
    (fn [on-login-trigger]
      [:div.login-container.flex.flex-col.items-center.justify-center.h-screen.bg-gray-900
       [:h2.text-white.mb-4 "Clorusa Login"]
       [:input.mb-2.p-2.rounded.bg-gray-700.text-white
        {:type "text" :placeholder "Homeserver" :value (:hs @fields)
         :on-change #(swap! fields assoc :hs (.. % -target -value))}]
       [:input.mb-2.p-2.rounded.bg-gray-700.text-white
        {:type "text" :placeholder "Username"
         :on-change #(swap! fields assoc :user (.. % -target -value))}]
       [:input.mb-2.p-2.rounded.bg-gray-700.text-white
        {:type "password" :placeholder "Password"
         :on-change #(swap! fields assoc :pass (.. % -target -value))}]
       [:button.p-2.rounded.bg-blue-600.text-white.hover:bg-blue-500
        {:on-click (fn [e]
                     (.preventDefault e)
                     (on-login-trigger (:hs @fields) (:user @fields) (:pass @fields)))}
        "Login"]])))

(defn room-list-view [rooms selected-room-atom on-room-click]
  [:div.w-64.bg-gray-800.border-r.border-gray-700.overflow-y-auto
   [:h2.p-4.font-bold.border-b.border-gray-700 "Rooms"]
   (doall
    (for [room rooms]
      (let [rid (.-roomId ^js room)
            rname (.-name ^js room)]
        [:div.p-3.cursor-pointer.hover:bg-gray-700
         {:key rid
          :class (when (= @selected-room-atom room) "bg-gray-700")
          :on-click #(on-room-click room)}
         (or rname rid "Unnamed Room")])))])

(defn room-view [selected-room]
  [:div.flex-1.flex.flex-col.bg-gray-900
   (if-let [room selected-room]
     [:<>
      [:div.p-4.bg-gray-800.border-b.border-gray-700
       [:h2.text-xl.font-bold (or (.-name ^js room) "Room")]]
      [timeline/timeline-view]]
     [:div.flex-1.flex.items-center.justify-center.text-gray-500
      "Select a room to start chatting"])])
