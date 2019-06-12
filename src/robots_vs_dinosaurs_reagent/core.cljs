(ns robots-vs-dinosaurs-reagent.core
  (:require
    [reagent.core :as r]
    [ajax.core :refer [GET POST DELETE]]
    [cljs.core.async :refer [<! timeout]])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(def uri "http://localhost:8080/api")

;;
;; Util
;;
(defn merge-ratom!
  [ratom m]
  (r/rswap! ratom merge m))

(defn assoc-in-ratom-evt
  "`assoc-in` the ratom with the `evt` target value."
  ([ratom ks f]
   (fn [evt]
     (r/rswap! ratom assoc-in ks (f (some-> evt .-target .-value)))))
  ([ratom ks]
   (assoc-in-ratom-evt ratom ks partial)))

;;
;; Alert
;;
(defn alert-timeout
  [alert-cursor]
  (go
    (<! (timeout 3500))
    (reset! alert-cursor nil)))

(defn alert
  [alert-cursor]
  (let [{:keys [title status message alert-class badge-class]} @alert-cursor]
    ^{:key :alert}
    [:div.fixed-top.fade
     {:class (when status (alert-timeout alert-cursor) "show")}
     [:div.alert.alert-dismissible {:class alert-class :role "alert"}
      [:small.id.badge.text-monospace.mr-2 {:class badge-class} status]
      [:strong.mr-1 title]
      [:span message]]]))

;;
;; AJAX
;;
(defn error-handler
  [alert-ratom]
  (fn [{:keys [response status]}]
    (merge-ratom!
      alert-ratom
      {:title       "Holy guacamole!"
       :status      status
       :badge-class "badge-danger"
       :alert-class "alert-danger"
       :message     (-> response :error :message)})))

(defn success-handler
  [alert-ratom message]
  (reset!
    alert-ratom
    {:title       "Yeah!"
     :status      201
     :badge-class "badge-success"
     :alert-class "alert-success"
     :message     message}))

(defn fetch-room!
  [board-ratom alert-ratom room-id]
  (GET
    (str uri "/simulations/" room-id)
    {:handler         #(reset! board-ratom %)
     :error-handler   (error-handler alert-ratom)
     :response-format :json, :keywords? true}))

(defn fetch-rooms!
  [rooms-ratom alert-ratom]
  (GET
    (str uri "/simulations")
    {:handler         #(reset! rooms-ratom %)
     :error-handler   (error-handler alert-ratom)
     :response-format :json, :keywords? true}))

(defn delete-room!
  [rooms-ratom alert-ratom room-id]
  (DELETE
    (str uri "/simulations/" room-id)
    {:handler         #(fetch-rooms! rooms-ratom alert-ratom)
     :error-handler   (error-handler alert-ratom)
     :response-format :json, :keywords? true}))

(defn new-room!
  [rooms-ratom alert-ratom params]
  (js/console.log params)
  (POST
    (str uri "/simulations")
    {:params          params
     :handler         #((success-handler alert-ratom "Room created.")
                        (fetch-rooms! rooms-ratom alert-ratom))
     :error-handler   (error-handler alert-ratom)
     :response-format :json, :keywords? true}))

;;
;; Modal
;;
(defn modal
  [modal-ratom]
  (let [{:keys                           [title body]
         {:keys [text on-click classes]} :button} @modal-ratom]
    ^{:key :modal}
    [:div#modal.modal.fade {:tabIndex "-1" :role "dialog" :aria-hidden "true"}
     [:div.modal-dialog.modal-dialog-centered
      [:div.modal-content
       [:div.modal-header.bg-dark.text-white
        [:h5.modal-title title]]
       [:div.modal-body body]
       [:div.modal-footer
        [:button.btn.btn-secondary {:data-dismiss "modal" :aria-label "close"} "Cancel"]
        [:button {:class classes :on-click on-click :data-dismiss "modal" :aria-label "close"} text]]]]]))

(defn modal-delete-room
  [modal-ratom rooms-ratom alert-ratom {:keys [title id]}]
  (merge-ratom!
    modal-ratom
    {:title  "Confirm Deletion"
     :body   (str "Delete the room \"" title "\"?")
     :button {:text     "Delete"
              :classes  "btn btn-danger"
              :on-click (fn [_evt]
                          (delete-room! rooms-ratom alert-ratom id)
                          (fetch-rooms! rooms-ratom alert-ratom))}}))

(defn modal-new-room
  [modal-ratom rooms-ratom alert-ratom forms-ratom]
  (let
    [form-cursor (r/cursor forms-ratom [:new-room])
     modal-body
     [:div.modal-body
      [:div.mb-4
       [:span "What is the Simulation room title and the board size?"]]
      [:div.input-group.mb-3
       [:div.input-group-prepend
        [:span#new-input-title.input-group-text "Title"]]
       [:input.form-control
        {:type      "text" :aria-describedby "new-input-title"
         :minLength "2" :maxLength "40"
         :on-change (assoc-in-ratom-evt form-cursor [:title])}]]
      [:div.input-group.mb-3
       [:div.input-group-prepend
        [:span.input-group-text "Size"]]
       [:input#new-input-width.form-control
        {:type      "number" :placeholder "Width"
         :min       5 :max 50
         :on-change (assoc-in-ratom-evt form-cursor [:size :width] js/parseInt)}]
       [:input#new-input-height.form-control
        {:type      "number" :placeholder "Height"
         :min       5 :max 50
         :on-change (assoc-in-ratom-evt form-cursor [:size :height] js/parseInt)}]]]]
    (merge-ratom!
      modal-ratom
      {:title  "Confirm New Room"
       :body   modal-body
       :button {:text     "New"
                :classes  "btn btn-primary"
                :on-click (fn [_evt]
                            (new-room! rooms-ratom alert-ratom @form-cursor))}})))

;;
;; Cards
;;
(defn room-card
  [{:keys                          [id title]
    {{:keys [width height]} :size} :board :as room}
   rooms-ratom
   alert-ratom
   modal-ratom
   page-ratom
   board-ratom]
  ^{:key id}
  [:div.col-sm-4
   [:div.card.text-white.bg-dark.mb-2
    [:div.card-header "Simulation Room"
     [:div [:small.id.badge.badge-secondary.text-monospace "ID " id]]]
    [:div.card-body.text-center
     [:h5.card-title.mb-4 title]
     [:p.card-text.text-monospace width " x " height]
     [:div.list-group
      [:button.btn.btn-lg.btn-primary.mb-2
       {:on-click #((reset! board-ratom {:id id})
                    (reset! page-ratom :board))}
       "Join »"]
      [:button.btn.btn-sm.btn-secondary
       {:data-toggle "modal"
        :data-target "#modal"
        :on-click    #(modal-delete-room modal-ratom rooms-ratom alert-ratom room)}
       "Delete"]]]]])

(defn room-card-new
  [rooms-ratom alert-ratom modal-ratom forms-ratom]
  ^{:key "new"}
  [:div.col-sm-4.order-last
   [:div.card.mb-2
    [:div.card-body.text-center
     [:h5.card-title.mb-4 "New Simulation room"]
     [:p.card-text "Click below to create a new simulation room"]
     [:div.list-group
      [:button.btn.btn-lg.btn-primary.mb-2
       {:data-target "#modal"
        :data-toggle "modal"
        :on-click    #(modal-new-room modal-ratom rooms-ratom alert-ratom forms-ratom)}
       "New"]]]]])

(defn room-cards
  [rooms-ratom modal-ratom forms-ratom alert-ratom page-ratom board-ratom]
  ^{:key :room-cards}
  [:div.container-fluid
   [:div.row
    (room-card-new rooms-ratom alert-ratom modal-ratom forms-ratom)
    (for [room @rooms-ratom]
      (room-card room rooms-ratom alert-ratom modal-ratom page-ratom board-ratom))]])

;;
;; Buttons
;;
(defn button-small-secondary
  [text click-handler]
  [:button.btn.btn-sm.btn-secondary.m-2 {:on-click click-handler} text])

(defn button-refresh-rooms
  [rooms-ratom alert-ratom]
  (button-small-secondary
    "Refresh"
    (fn [_evt]
      (reset! rooms-ratom nil)
      (fetch-rooms! rooms-ratom alert-ratom))))

(defn button-refresh-board
  [board-ratom alert-ratom]
  (let [{:keys [id]} @board-ratom]
    (button-small-secondary
      "Refresh"
      (fn [_evt]
        (fetch-room! board-ratom alert-ratom id)))))

(defn button-page-rooms
  [page-ratom]
  (button-small-secondary
    "Back"
    (fn [_evt]
      (reset! page-ratom :home))))

;;
;; Spinner
;;
(defn spinner-loading
  [loading-ratom]
  (when-not @loading-ratom
    ^{:key :spinner-loading}
    [:div.d-flex.justify-content-center
     [:div.spinner-border.text-primary.m-5 {:role "status"}
      [:span.sr-only "Loading..."]]]))

;;
;; Jumbotron
;;
(defn jumbotron
  [header-ratom]
  (let [{:keys [title description subtitle children]} @header-ratom]
    ^{:key :jumbotron}
    [:div.jumbotron.jumbotron-fluid.overflow-hidden.text-center.mb-4.p-4
     [:header.mb-2
      [:h1.display-5 title]
      [:p description]
      [:hr.my-4]
      [:h3 subtitle]]
     children]))

;;
;; Container
;;
(defn container
  [children]
  [:div.container-fluid
   (filter identity children)])

(defn board-table
  [board-ratom]
  (let [{:keys                                        [id title]
         {:keys [total]}                              :scoreboard
         {:keys [units] {:keys [width height]} :size} :board} @board-ratom]
    [:div
     [:div.dinosaur.pixelated]
     [:div.robot.pixelated]
     [:div.robot.robot-right.pixelated]
     [:div.robot.robot-left.pixelated]
     [:div.robot.robot-up.pixelated]
     [:div.robot.robot-down.pixelated]
     ]
    ;[ 
    ; [:div.container
    ; [:table.table.table-dark.table-bordered.table-hover.table-sm.table-responsive-sm
    ;  [:tbody.board
    ;   (repeat
    ;     height
    ;     [:tr
    ;      (repeat
    ;        width
    ;        [:td ""])])]]]]
    ))



;;
;; Pages
;;
(defn board-page
  [page-ratom board-ratom header-ratom rooms-ratom alert-ratom modal-ratom forms-ratom]
  (let [{:keys [id title]} @board-ratom]
    (fetch-room! board-ratom alert-ratom id)

    (merge-ratom!
      header-ratom
      {:subtitle (str title " Board")
       :children [:div
                  ;(button-refresh-board board-ratom alert-ratom)
                  ;(button-page-rooms page-ratom)
                  ]})

    (container [(alert alert-ratom)
                (modal modal-ratom)
                (jumbotron header-ratom)
                (spinner-loading board-ratom)
                (board-table board-ratom)])))

(defn rooms-page
  "Show available rooms."
  [page-ratom board-ratom header-ratom rooms-ratom alert-ratom modal-ratom forms-ratom]

  (merge-ratom!
    header-ratom
    {:subtitle "Choose a Room"
     :children (button-refresh-rooms rooms-ratom alert-ratom)})

  (container [(alert alert-ratom)
              (modal modal-ratom)
              (jumbotron header-ratom)
              (spinner-loading rooms-ratom)
              (room-cards rooms-ratom modal-ratom forms-ratom alert-ratom page-ratom board-ratom)]))

;;
;; State
;;
(def initial-state
  {:header {:title       "Robots vs Dinosaurs"
            :description "Run simulations on remote-controlled robots that fight dinosaurs"
            :subtitle    ""
            :children    nil}
   :alert  nil
   :rooms  nil
   :board  {:id 3}
   :page   :board
   :forms  {:new-room {:title ""
                       :size  {:width  "50"
                               :height "50"}}}
   :modal  {:title  "Modal"
            :body   ""
            :button {:text     "Ok"
                     :classes  "btn btn-primary"
                     :on-click nil}}})

;;
;; App
;;
(defn app
  "The app."
  []
  (let [state-ratom (r/atom initial-state)
        page-cursor (r/cursor state-ratom [:page])
        header-cursor (r/cursor state-ratom [:header])
        alert-cursor (r/cursor state-ratom [:alert])
        rooms-cursor (r/cursor state-ratom [:rooms])
        board-cursor (r/cursor state-ratom [:board])
        modal-cursor (r/cursor state-ratom [:modal])
        forms-ratom (r/cursor state-ratom [:forms])]

    (fetch-rooms! rooms-cursor alert-cursor)

    (fn []
      (let [page @page-cursor]
        (case page
          :home (rooms-page
                  page-cursor
                  board-cursor
                  header-cursor
                  rooms-cursor
                  alert-cursor
                  modal-cursor
                  forms-ratom)
          :board (board-page
                   page-cursor
                   board-cursor
                   header-cursor
                   rooms-cursor
                   alert-cursor
                   modal-cursor
                   forms-ratom))))))
;;
;; Bootstrap
;;
(defn mount-root
  "Bootstrap the app."
  []
  (r/render
    [app]
    (.getElementById js/document "app")))

(defn init!
  "Init the app."
  []
  (mount-root))
