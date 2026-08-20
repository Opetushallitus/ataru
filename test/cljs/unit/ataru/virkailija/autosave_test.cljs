(ns ataru.virkailija.autosave-test
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.subs]
            [cljs.core.async :as async]
            [cljs.test :refer-macros [deftest is async]]
            [re-frame.core :as re-frame]
            [re-frame.db :as re-frame-db]))

; interval-loop viivästää varsinaista tallennusta debounce-ms verran ennen kuin
; handler kutsutaan. Testeissä käytetään tuotantokoodin oletusarvoa (1000ms) huomattavasti
; lyhyempää debounce-ms:ää, jotta testit eivät joudu odottamaan oikeaa debounce-aikaa.
(def debounce-delay-ms 40)
; ylimääräinen marginaali, jonka verran odotetaan debounce-viiveen umpeutumisen lisäksi
(def margin-ms 30)
; odotus, joka on lyhyempi kuin debounce-delay-ms, jotta tallennus on vielä kesken
(def pending-wait-ms 15)

(defn- reset-app-db! [path value]
  (re-frame/clear-subscription-cache!)
  (reset! re-frame-db/app-db (assoc-in {} path value)))

(defn- watch-and-force-updates
  [path]
  (re-frame/subscribe [:state-query path]))

(deftest sends-changed-value
  (async done
    (asyncm/go
      (let [path    [:autosave-test-0 :value]
            calls   (atom [])
            handler (fn [current prev] (swap! calls conj [current prev]))]
        (reset-app-db! path "initial")
        (let [value-to-watch (watch-and-force-updates path)
              _              @value-to-watch]
          (autosave/interval-loop {:subscribe-path path
                                    :debounce-ms    debounce-delay-ms
                                    :handler        handler})
          (swap! re-frame-db/app-db assoc-in path "changed")
          @value-to-watch
          (async/<! (async/timeout (+ debounce-delay-ms margin-ms)))
          (is (= [["changed" "initial"]] @calls)
              "a changed value should be sent to the handler once the debounce delay has elapsed")
          (done))))))

(deftest does-not-resend-an-already-saved-value-when-stopped
  (async done
    (asyncm/go
      (let [path    [:autosave-test-1 :value]
            calls   (atom [])
            handler (fn [current prev] (swap! calls conj [current prev]))]
        (reset-app-db! path "initial")
        (let [value-to-watch (watch-and-force-updates path)
              _              @value-to-watch
              stop-fn        (autosave/interval-loop {:subscribe-path path
                                                       :debounce-ms    debounce-delay-ms
                                                       :handler        handler})]
          (swap! re-frame-db/app-db assoc-in path "changed")
          @value-to-watch
          (async/<! (async/timeout (+ debounce-delay-ms margin-ms)))
          (is (= [["changed" "initial"]] @calls)
              "the debounced autosave should have fired exactly once for the edit")
          (stop-fn)
          (async/<! (async/timeout margin-ms))
          (is (= [["changed" "initial"]] @calls)
              "stopping right after a completed save must not trigger a redundant resend of the same, already-saved value")
          (done))))))

(deftest flushes-a-genuinely-pending-change-when-stopped
  (async done
    (asyncm/go
      (let [path    [:autosave-test-2 :value]
            calls   (atom [])
            handler (fn [current prev] (swap! calls conj [current prev]))]
        (reset-app-db! path "initial")
        (let [value-to-watch (watch-and-force-updates path)
              _              @value-to-watch
              stop-fn        (autosave/interval-loop {:subscribe-path path
                                                       :debounce-ms    debounce-delay-ms
                                                       :handler        handler})]
          (swap! re-frame-db/app-db assoc-in path "changed")
          @value-to-watch
          ; pysäytä ennen debounce-viiveen umpeutumista, kun tallennus on vielä kesken
          (async/<! (async/timeout pending-wait-ms))
          (stop-fn)
          (async/<! (async/timeout margin-ms))
          (is (= [["changed" "initial"]] @calls)
              "stopping while a change is still pending must flush it once, even though the debounce has not fired yet")
          (done))))))
