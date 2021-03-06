(ns flash.core
  "Example of `re-frame-simple`, an alternate `re-frame` syntax for simple use cases."
  (:require
   [re-view.re-frame-simple :as db :include-macros true]
   [re-frame.core :as re-frame]
   [clojure.walk :refer [keywordize-keys]]
   [reagent.core :as reagent]
   ;; this is re-frame-trace's separate instance of re-frame
   #_[mranderson047.re-frame.v0v10v2.re-frame.db :as trace-db]
   #_[mranderson047.re-frame.v0v10v2.re-frame.core :as trace-rf]
   ["../aws-exports" :default aws-exports]
   ["aws-amplify" :default amplify :refer (API,graphqlOperation)]
   ["aws-amplify-react" :refer (withAuthenticator)]
   ["react-infinite" :as ReactInfinite]
   ["@material-ui/icons/Add" :default AddIcon]
   ["@material-ui/core/Button" :default Button]
   ["@material-ui/core/Card" :default Card]
   ["@material-ui/core/CardContent" :default CardContent]
   ["@material-ui/core/TextField" :default TextField]
   ["@material-ui/core/AppBar" :default AppBar]
   ["@material-ui/core/Toolbar" :default Toolbar]
   ["@material-ui/core/IconButton" :default IconButton]
   ["@material-ui/icons/Menu" :default Menu]
   ["@material-ui/core/Typography" :default Typography]
   ["material-ui-chip-input" :default ChipInput]
   ["@material-ui/core/List" :default List]
   ["@material-ui/core/Divider" :default Divider]
   ["@material-ui/core/ListItem" :default ListItem]
   ["@material-ui/core/CardActions" :default CardActions]
   [flash.routes :refer [app-routes path-for-page]]))

(def card-actions (reagent/adapt-react-class CardActions))
(def divider (reagent/adapt-react-class Divider))
(def list (reagent/adapt-react-class List))
(def list-item (reagent/adapt-react-class ListItem))
(def chip-input (reagent/adapt-react-class ChipInput))
(def typography (reagent/adapt-react-class Typography))
(def menu-icon (reagent/adapt-react-class Menu))
(def icon-button (reagent/adapt-react-class IconButton))
(def tool-bar (reagent/adapt-react-class Toolbar))
(def app-bar (reagent/adapt-react-class AppBar))
(def text-field (reagent/adapt-react-class TextField))
(def button (reagent/adapt-react-class Button))
(def add-icon (reagent/adapt-react-class AddIcon))
(def material-card (reagent/adapt-react-class Card))
(def card-content (reagent/adapt-react-class CardContent))
(def react-infinite (reagent/adapt-react-class ReactInfinite))

(def listCardsQuery
  "
  query listCards {
    listCards(limit: 1000) {
      items{
        id
        name
        description
      }
    }
  }
  ")

(def createCardMutation
  "
  mutation createCard($name:String! $description: String, $tags:[String!]) {
  createCard(input:{
    name:$name
    description:$description
    tags:$tags
  }){
    id
    name
    description
    tags
  }
  }
  ")

(def createReviewMutation
  "
  mutation createReview($type:String! $cardReviewsId: ID!) {
  createReview(input:{
    type:$type
    cardReviewsId:$cardReviewsId
  }){
    id
  }
  }
  ")

(re-frame/reg-fx
 :graphql-query
 (fn [{:keys [on-success on-failure query]}]
   (-> (.graphql API (graphqlOperation query))
       (.then #(on-success (keywordize-keys (js->clj %))))
       (.catch #(on-failure (keywordize-keys (js->clj %)))))))

(re-frame/reg-fx
 :graphql-mutation
 (fn [{:keys [on-success on-failure mutation details]}]
   (-> (.graphql API (graphqlOperation mutation details))
       (.then #(on-success (keywordize-keys (js->clj %))))
       (.catch #(on-failure (keywordize-keys (js->clj %)))))))

(re-frame/reg-event-db
 :created-card
 (fn [db [_ {:keys [data]}]]
   (-> (update-in db [:cards] #(conj % (:createCard data)))
       (assoc :add-card/front "")
       (assoc :add-card/back "")
       (assoc :add-card/tags []))))

(re-frame/reg-event-fx
 :create-card
 (fn [cofx [_ c]]
   {:db       (:db cofx)
    :graphql-mutation {:on-success #(re-frame/dispatch [:created-card %])
                       :mutation createCardMutation
                       :details (clj->js c)}}))
(re-frame/reg-event-db
 :created-review
 (fn [db [_ {:keys [data]}]]
   (-> (update-in db [:reviews/reviews] #(conj % (:createReview data))))))

(re-frame/reg-event-fx
 :review/create-review
 (fn [cofx [_ c]]
   {:db       (:db cofx)
    :graphql-mutation {:on-success #(re-frame/dispatch [:created-review %])
                       :mutation createReviewMutation
                       :details (clj->js c)}}))
(re-frame/reg-event-db
 :loaded-cards
 (fn [db [_ {:keys [data]}]]
   (assoc-in db [:cards] (:items (:listCards data)))))

(re-frame/reg-event-fx
 :load-cards
 (fn [cofx [_ a]]
   {:db       (assoc (:db cofx) :flag  a)
    :graphql-query {:on-success #(re-frame/dispatch [:loaded-cards %])
                    :query listCardsQuery}}))

(re-frame/reg-event-db
 :nav/set-current-page
 (fn [db [_ current-page]]
   (assoc-in db [:current-page] current-page)))

(db/defupdate :initialize
  [db]
  {:cards []
   :add-card/tags ["foo"]})

(defn card
  [c]
  [:div
   [list-item
    {:button true}
    [:p (str (js->clj (:name c)))]]
   [divider]])

(defn add-button
  []
  [button {:style {:position :fixed
                   :bottom "2em"
                   :right "2em"}
           :color "primary"
           :variant "fab"
           :size "medium"
           :href (path-for-page :add-card)}
   [add-icon]])

(defn cards-list
  [cards]
  [list
   (for [c cards]
     ^{:key (:id c)}
     [card c])])

(defn cards-view [props]
  (re-frame/dispatch [:load-cards])
  (let [cards (db/get :cards)]
    [:div
     [app-bar {:position :sticky}
      [tool-bar
       [icon-button
        {:color :inherit}
        [menu-icon]]
       [typography {:variant :h6 :color :inherit}
        "My Cards"]]]
     [cards-list cards]
     [add-button]]))

(defn add-card-view []
  (re-frame/dispatch [:load-cards])
  (let [front (db/get :add-card/front)
        back (db/get :add-card/back)
        tags (db/get :add-card/tags)]
    [:div
     {:style {:flex-direction :column
              :display :flex}}
     [app-bar {:position :sticky}
      [tool-bar
       {:style {:justify-content :space-between}}
       [:div]
       [button {:style {:color "white"}
                :on-click #(re-frame/dispatch [:create-card {:name front :description back :tags tags}])} "Save"]]]
     [text-field {:placeholder "Front"
                  :on-change #(db/assoc! :add-card/front (-> % .-target .-value))
                  :value front}]
     [text-field {:placeholder "Back"
                  :on-change #(db/assoc! :add-card/back (-> % .-target .-value))
                  :value back}]
     [chip-input {:value (clj->js tags)
                  :on-add (fn [c] (db/update! :add-card/tags #(conj % c)))
                  :on-delete (fn [c] (db/update! :add-card/tags pop))}]]))

(re-frame/reg-sub
 :reviews/suggested-reviews
 (fn [db _]
   (map (fn [c] {:type "info" :card c :cardReviewsId (c :id)}) (:cards db))))

(defn reviews-view
  []
  (re-frame/dispatch [:load-cards])
  (let [reviews (re-frame/subscribe [:reviews/suggested-reviews])]
    [:div
     [list
      {:style {:justify-content :center
               :flex-direction :column
               :display :flex}}
      (for [r @reviews]
        [material-card
         {:style {:width "80vw"
                  :margin-bottom "2em"}}
         [card-content
          {:style {:justify-content :center
                   :display :flex}}
          [:h1 (str (:name (:card r)))
           ]]
         [card-actions
          {:style {:display :flex
                   :justify-content :flex-end}}
          [button
           {:on-click #(re-frame/dispatch [:review/create-review {:type (r :type)
                                                                  :cardReviewsId (r :cardReviewsId)}])}
           "OK"]]
         ])]]))

(defn main-view [props]
  (let [current-page (db/get :current-page)]
    (case current-page
      :cards [cards-view props]
      :add-card [add-card-view]
      :reviews  [reviews-view])))

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
  #_(-> #(when-not (get-in @trace-db/app-db [:settings :show-panel?])
           (trace-rf/dispatch [:settings/user-toggle-panel]))
        (js/setTimeout 100)))
