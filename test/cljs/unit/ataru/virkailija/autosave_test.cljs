(ns ataru.virkailija.autosave-test
  (:require-macros [cljs.core.async.macros :as asyncm])
  (:require [ataru.virkailija.autosave :as autosave]
            [ataru.virkailija.subs]
            [cljs.core.async :as async]
            [cljs.test :refer-macros [deftest is async]]
            [re-frame.core :as re-frame]
            [re-frame.db :as re-frame-db]))

; interval-loop viivästää varsinaista tallennusta ataru.cljs-util/debounce:n oletusarvon
; 1000ms verran, joten näiden testien täytyy odottaa tuon viiveen yli nähdäkseen
; laukesiko tallennus.
(def debounce-delay-ms 1000)

(defn- reset-app-db! [path value]
  (re-frame/clear-subscription-cache!)
  (reset! re-frame-db/app-db (assoc-in {} path value)))

(defn- watch-and-force-updates
  [path]
  (re-frame/subscribe [:state-query path]))

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
                                                       :handler        handler})]
          (swap! re-frame-db/app-db assoc-in path "changed")
          @value-to-watch
          (async/<! (async/timeout (+ debounce-delay-ms 150)))
          (is (= [["changed" "initial"]] @calls)
              "the debounced autosave should have fired exactly once for the edit")
          (stop-fn)
          (async/<! (async/timeout 150))
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
                                                       :handler        handler})]
          (swap! re-frame-db/app-db assoc-in path "changed")
          @value-to-watch
          ; pysäytä ennen debounce-viiveen umpeutumista, kun tallennus on vielä kesken
          (async/<! (async/timeout 50))
          (stop-fn)
          (async/<! (async/timeout 150))
          (is (= [["changed" "initial"]] @calls)
              "stopping while a change is still pending must flush it once, even though the debounce has not fired yet")
          (done))))))
