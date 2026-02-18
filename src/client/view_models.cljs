(ns client.view-models
  (:require ["@element-hq/web-shared-components" :refer [BaseViewModel]]
            [goog.object :as g]))

(defn create-room-list-vm [props]
  (let [initial-state #js {:isLoadingRooms true
                           :isRoomListEmpty true
                           :roomIds #js []
                           :filterIds #js ["unread" "people" "rooms"]}]
    (let [vm (new BaseViewModel props initial-state)]
      (g/extend vm
        #js {:onToggleFilter (fn [id] (js/console.log "Filter toggled:" id))
             :createChatRoom (fn [] (js/console.log "Create DM"))
             :createRoom     (fn [] (js/console.log "Create Room"))
             :getRoomItemViewModel (fn [id] 
                                     nil)
             :updateVisibleRooms (fn [start end] 
                                   (js/console.log "Scroll range:" start end))})
      vm)))
