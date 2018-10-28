(ns flash.core
  "Example of `re-frame-simple`, an alternate `re-frame` syntax for simple use cases."
  (:require
   [re-view.re-frame-simple :as db :include-macros true]
   [re-frame.core :as re-frame]
   [clojure.walk :refer [keywordize-keys]]
   [reagent.core :as reagent]
   ;; this is re-frame-trace's separate instance of re-frame
   [mranderson047.re-frame.v0v10v2.re-frame.db :as trace-db]
   [mranderson047.re-frame.v0v10v2.re-frame.core :as trace-rf]
   ["../aws-exports" :default aws-exports]
   ["aws-amplify" :default amplify :refer (API,graphqlOperation)]
   ["aws-amplify-react" :refer (withAuthenticator)]
   ["react-infinite" :as ReactInfinite]
   ["@material-ui/core/Card" :default Card]
   [flash.routes :refer [app-routes]]
   ))


(def material-card (reagent/adapt-react-class Card))

(def react-infinite (reagent/adapt-react-class ReactInfinite))

(def listCardsQuery
  "
  query listCards {
    listCards {
      items{
        id
        name
        description
      }
    }
  }
  ")

(re-frame/reg-fx
 :graphql-query
 (fn [{:keys [on-success on-failure query]}]
   (-> (.graphql API (graphqlOperation query))
       (.then #(on-success (keywordize-keys (js->clj %))))
       (.catch #(on-failure (keywordize-keys (js->clj %)))))))

(re-frame/reg-event-db
 :loaded-cards
 (fn [db [_ {:keys [data]}]]
   (assoc-in db [:cards] (:items (:listCards data)))))

(re-frame/reg-event-db
 :nav/set-current-page
 (fn [db [_ current-page]]
   (assoc-in db [:current-page] current-page)))

(re-frame/reg-event-fx
 :load-cards
 (fn [cofx [_ a]]
   {:db       (assoc (:db cofx) :flag  a)
    :graphql-query {:on-success #(re-frame/dispatch [:loaded-cards %])
                    :query listCardsQuery}}))

(db/defupdate :initialize
  [db]
  {:cards []})

(defn card
  [c]
  [material-card [:p (str (js->clj (:name c)))]])

(defn cards-list
  [cards]
  [react-infinite
   {:use-window-as-scroll-container true
    :element-height 40}
   (map-indexed (fn [ i c ]
                  ^{:key i}
                  [card c]
                  )(take 10000 (cycle cards))
                )])

(defn cards-view [props]
  (re-frame/dispatch [:load-cards])
  (let [cards (db/get :cards)]
    [cards-list cards]))

(defn main-view [props]
(let [current-page (db/get :current-page)]
  [cards-view props]))

(def root-view
(reagent/adapt-react-class
 (withAuthenticator
  (reagent/reactify-component main-view) true)))

(defn ^:export render []
(reagent/render [root-view]
                (js/document.getElementById "shadow-re-frame")))

(defn ^:export init []
(.configure amplify aws-exports)
(db/dispatch [:initialize])
(app-routes)
(render)
(-> #(when-not (get-in @trace-db/app-db [:settings :show-panel?])
       (trace-rf/dispatch [:settings/user-toggle-panel]))
    (js/setTimeout 100)))
