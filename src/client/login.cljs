(ns client.login
  (:require
   [client.view-models :refer [create-room-list-vm]]
   [reagent.core :as r]
   [promesa.core :as p]
   [client.state :refer [sdk-world]]
  [client.session-store :refer [SessionStore]]
  [client.sdk-ctrl :as sdk-ctrl]
   ["generated-compat" :as sdk]
   ["@element-hq/web-shared-components" :refer [RoomListView BaseViewModel]])
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

(defn start-sync! [client]
  (p/let [sync-service (-> (.syncService client) (.withOfflineMode) (.finish))
          rls-instance (.roomListService sync-service)
          room-list    (.allRooms rls-instance)]
    (let [rl-vm (create-room-list-vm #js {:client client :roomListService rls-instance})]
      (do
        (aset rl-vm "onUpdate"
              (fn [updates] (js/console.log "UPDATE TRIGGERED!" (alength updates))))
        (let [entries-result (.entriesWithDynamicAdapters room-list 200 rl-vm)]
            (aset rl-vm "entries_handle" (.entriesStream entries-result))
            (aset rl-vm "controller" (.controller entries-result)))
          (swap! sdk-world assoc-in [:vms :room-list] rl-vm)
          (.start sync-service)
          (.setRange  (.-controller rl-vm) 0 50)
          (.addOnePage (.-controller rl-vm))
          (js/console.log "Background stream handle secured. Syncing...")))))

(defn login! [hs user pass on-success]
(let [sdk-root (if (.-ClientBuilder sdk) sdk (.-default sdk))
    ClientBuilder (.-ClientBuilder sdk-root)
    SSVBuilder    (.-SlidingSyncVersionBuilder sdk-root)
    IDBBuilder    (.-IndexedDbStoreBuilder sdk-root)]

(let [store (SessionStore.)]
(-> (p/let [
            store-id   (.generateStoreId store)
            passphrase (.generatePassphrase store)
            store-name (.getStoreName store store-id)
            store-config (-> (new IDBBuilder store-name)
                             (.passphrase passphrase))

            builder (-> (new ClientBuilder)
                        (.slidingSyncVersionBuilder (.-DiscoverNative SSVBuilder))
                        (.serverNameOrHomeserverUrl hs)
                        (.indexeddbStore store-config)
                        (.autoEnableCrossSigning true))
            client  (.build builder)
            _ (.login client user pass)
            session (.session client)]
      (js/console.log "Login successful. Negotiated version:" (.slidingSyncVersion client))
      (.save store session passphrase store-id)
      (on-success client))

    (p/catch (fn [e]
               (js/console.error "Login Error:" e)
               (js/alert (str "Login Failed: " (.-message e)))))))))

(defn restore-client! [session passphrase store-id on-success]
  (let [sdk-root (if (.-ClientBuilder sdk) sdk (.-default sdk))
        ClientBuilder (.-ClientBuilder sdk-root)
        IDBBuilder    (.-IndexedDbStoreBuilder sdk-root)
        store (SessionStore.)
        store-name (.getStoreName store store-id)]
    (js/console.log "Attempting to restore session for store:" store-name)
    (p/let [store-config (-> (new IDBBuilder store-name)
                             (.passphrase passphrase))
            builder (-> (new ClientBuilder)
                        (.homeserverUrl (.-homeserverUrl session))
                        (.indexeddbStore store-config)
                        (.autoEnableCrossSigning true))
            client (.build builder)]
      (.restoreSession client session)
      (js/console.log "Session Restored!")
      (on-success client))))

(defn bootstrap! []
(p/let [_ (init-sdk!)]
(let [store (SessionStore.)
      sessions (.loadSessions store)
      user-id (first (js/Object.keys sessions))]
  (if user-id
    (let [data (aget sessions user-id)]
      (js/console.log "Found persistent session for:" user-id)
      (-> (restore-client! (aget data "session")
                           (aget data "passphrase")
                           (aget data "storeId")
                           start-sync!)
          (p/catch (fn [e]
                     (js/console.error "Restore failed (invalidating session):" e)
                     (.clear store user-id)
                     ))))
    (js/console.log "No session found. Waiting for user login.")))))
