(ns ataru.application-common.components.dropdown-actions
  "collapse-dropdown/expand-dropdown yhdistävät re-frame-tilanpäivityksen
  sivun vierityksen lukitsemiseen/vapauttamiseen mobiilin
  kokoruutuesityksessä."
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [schema.core :as s]
            [ataru.application-common.components.dropdown-viewport :as viewport]))

(s/defn collapse-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  ;; unlock-body-scroll! ei saa ajaa synkronisesti tässä: se poistaisi html/
  ;; body:n vieritys-lukon HETI, mutta a-dropdown--fullscreen-luokka (jonka
  ;; poistuminen on se, mikä oikeasti saa kokoruutuvalikon lakkaamasta
  ;; olemasta position: fixed :has()-wrapperin kautta) poistuu vasta kun
  ;; expanded?-tilan muutos on ehtinyt renderöityä. Näiden kahden välissä
  ;; oleva hetki, jolloin vieritys on jo sallittu mutta kokoruutuylitys on
  ;; silti kiinnitetty, aiheutti näkyvän välähdyksen (koko sivu "venyi"
  ;; hetkeksi) valinnan yhteydessä — after-render synkronoi ne samaan
  ;; committiin.
  (reagent/after-render viewport/unlock-body-scroll!)
  (re-frame/dispatch [:application-components/collapse-dropdown {:dropdown-id dropdown-id}]))

(s/defn expand-dropdown
  [{:keys [dropdown-id]} :- {:dropdown-id s/Str}]
  (viewport/lock-body-scroll!)
  (re-frame/dispatch [:application-components/expand-dropdown {:dropdown-id dropdown-id}]))
