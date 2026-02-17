(ns client.login
  (:require
   [reagent.core :as r]
   [promesa.core :as p]
   [client.state :refer [sdk-world]]
   [client.sdk-ctrl :as sdk-ctrl]
   ["generated-compat" :as sdk])
  (:require-macros [macros :refer [ocall oget]])
  )

(defonce sdk-ready? (r/atom false))

(defn init-sdk! []
  (-> (p/let [_ (sdk/uniffiInitAsync)]
        (js/console.log "WASM loaded")
        (reset! sdk-ready? true)
        (swap! sdk-world assoc :loading? false))
      (p/catch (fn [e]
                 (js/console.error "WASM Load Failed:" e)
                 (swap! sdk-world assoc :loading? false)))))

(defn start-sync! [client on-success]
  (-> (p/let [rls-service (ocall client :roomListService)
              rls (ocall rls-service :allRooms)
              sync-service (ocall rls :syncService)
              sync-handle (-> sync-service (.withOfflineMode) (.finish))
              _ (.start ^js sync-handle)]
        (js/console.log "RoomListService & Sync bonded")
        ;; TODO wrap proper for type validation
        (swap! sdk-world assoc 
               :client (client.state/->MatrixClient client) 
               :loading? false)
        (client.sdk-ctrl/setup-room-list! rls)
        (on-success client))
      (p/catch 
          (fn [e]
            (js/console.error "Sync Chain Failed:" e)
            (swap! sdk-world assoc :loading? false)))))


(defn login! [hs user pass on-success on-rooms-update]
  (let [sdk-root (if (.-Client sdk) sdk (.-default sdk))
        ClientBuilder (.-ClientBuilder sdk-root)
        SSVBuilder (.-SlidingSyncVersionBuilder sdk-root)]
    (-> (p/let [
                builder (-> (new ClientBuilder)
                            (.slidingSyncVersionBuilder (.-DiscoverNative SSVBuilder))
                            (.serverNameOrHomeserverUrl hs)
                            (.inMemoryStore))
                client (.build builder)
                _ (.login ^js client user pass)]
          (let [version (.slidingSyncVersion ^js client)]
            (js/console.log "Negotiated Sync Version:" version))
          (start-sync! client on-success))
        (p/catch (fn [e] (js/console.error "Login Error:" e))))))
