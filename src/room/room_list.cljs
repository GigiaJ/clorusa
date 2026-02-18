(ns room.room-list)

(defn handle-loading-state-change [this state]
  (js/console.log "State:" state)
  (let [is-loading (not= state "Loaded")]
    (.merge (.-snapshot this) #js {:isLoadingRooms is-loading})))

(defn run [this]
  (go
    (try
      (let [rls       (.-roomListService (.-props this))
            room-list (<! (.allRooms rls))]
        (set! (.-roomList this) room-list)
        
        (let [loading-res (.loadingState room-list (js-obj "onUpdate" (.-handleLoadingStateChange this)))]
          (set! (.-stateStream this) (.-stateStream loading-res))
          (.handleLoadingStateChange this (.-state loading-res))
          (.track (.-disposables this) (fn [] (when-let [s (.-stateStream this)] (.cancel s)))))

        (let [entries-res (.entriesWithDynamicAdapters room-list 200 this)]
          (set! (.-entriesStream this) (.entriesStream entries-res))
          (set! (.-controller this)    (.controller entries-res))
          
          (.track (.-disposables this) (fn [] (when-let [s (.-entriesStream this)] (.cancel s))))
          (.addOnePage (.-controller this))))

      (catch :default e
        (js/console.error "Failed to initialize room list:" e)
        (.merge (.-snapshot this) #js {:isLoadingRooms false :isRoomListEmpty true})))))
