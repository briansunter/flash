#_(:import goog.History)
(ns flash.routes
  (:require
   [goog.events :as events]
   [accountant.core :as accountant]
   [goog.history.EventType :as EventType]
   [re-frame.core :as re-frame]
   [bidi.bidi :as bidi]))

(defn parse-path
  "Parse URL parameters into a hashmap"
  [p]
  (let [[path params](clojure.string/split p #"\?")
        param-strs (clojure.string/split params #"\&")]
    {:params (into {} (for [[k v] (map #(clojure.string/split % #"=") param-strs)]
                        [(keyword k) v]))
     :path path}))

(def card-routes
  {"" :cards
   "/" {"" :cards
        "add" :add-card
        [:nav/card-id ""] :view-card
        [:nav/card-id "/"] :view-card
        true :not-found}
   true :not-found})

(def tag-routes
  {"" :tags
   "/" {"" :tags
        "add" :add-tag
        [:tag "/add-template"] :add-template-tag
        [:tag ""] :view-tag
        [:tag "/"] :view-tag
        true :not-found}
   true :not-found})

(def review-routes
  {"" :reviews
   "/" {"" :reviews
        "add" :add-review
        [:tag ""] :view-review
        [:tag "/"] :view-review
        true :not-found}
   true :not-found})

(def routes
  [
   "/" {"" :cards
        "cards" card-routes
        "tags" tag-routes
        "reviews" review-routes
        true :not-found}
   "" :actions
   true :not-found])

(def path-for-page
  (partial bidi/path-for routes))

(defn parse-route-params
  [params]
  (update params :nav/action-id js/parseInt))

(defn set-page!
  [match]
  (let [current-page (:handler match)
        route-params (:route-params match)
        query-params (:query-params match)
        parsed-route-params (parse-route-params route-params)]
    (re-frame/dispatch [:nav/set-current-page current-page parsed-route-params])))

(defn app-routes []
  (accountant/configure-navigation!
   {:nav-handler (fn [p]
                   (let [{:keys [path params]} (parse-path p)
                         match (bidi/match-route routes path)]
                     (set-page! (merge {:query-params params} match))))
    :path-exists? (fn [path]
                    (boolean (bidi/match-route routes path)))})
  (accountant/dispatch-current!))
