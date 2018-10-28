(ns shadow-re-frame.simple
  "Example of `re-frame-simple`, an alternate `re-frame` syntax for simple use cases."
  (:require
   [re-view.re-frame-simple :as db :include-macros true]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [shadow-re-frame.welcome :as text]
   ;; this is re-frame-trace's separate instance of re-frame
   [mranderson047.re-frame.v0v10v2.re-frame.db :as trace-db]
   [mranderson047.re-frame.v0v10v2.re-frame.core :as trace-rf]
   ["../aws-exports" :default aws-exports]
   ["aws-amplify" :default amplify :refer (API,graphqlOperation)]
   ["aws-amplify-react" :refer (withAuthenticator)]
   ))

(.configure amplify aws-exports)

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
       (.then #(on-success (js->clj %)))
       (.catch #(on-failure (js->clj %)))
       )))

(re-frame/reg-event-db
 :loaded-cards
 (fn [db [_ items]]
   (assoc-in db [:cards] items)))

(re-frame/reg-event-fx
:load-cards
(fn [cofx [_ a]]
  {:db       (assoc (:db cofx) :flag  a)
   :graphql-query {:on-success #(re-frame/dispatch [:loaded-cards %])
                   :query listCardsQuery
                   }}))

(db/defupdate :initialize
"Initialize the `db` with the preselected emoji as counter IDs."
[db]
{:cards []})

(defn root-view-3 [props]
(re-frame/dispatch [:load-cards])
(let [cards (db/get :cards)]
  [:p (str cards)])  )

(def root-view
(reagent/adapt-react-class
 (withAuthenticator
  (reagent/reactify-component root-view-3) true)))

(defn ^:export render []
(reagent/render [root-view]
                (js/document.getElementById "shadow-re-frame")))

(defn ^:export init []

(db/dispatch [:initialize])
(render)


;; open re-frame-trace panel (after a timeout, to make sure it's state has loaded)
(-> #(when-not (get-in @trace-db/app-db [:settings :show-panel?])
       (trace-rf/dispatch [:settings/user-toggle-panel]))
    (js/setTimeout 100)))
