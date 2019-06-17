(ns robots-vs-dinosaurs-reagent.core
  (:require
    [reagent.core :as r]
    [ajax.core :refer [GET POST DELETE]]
    [cljs.core.async :as a])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

;(def uri "http://localhost:8080/api")
(def uri "https://robots-vs-dinosaurs.herokuapp.com/api")

(defn merge-ratom!
  [ratom m]
  (r/rswap! ratom merge m))

(defn assoc-in-ratom!
  [ratom ks m]
  (r/rswap! ratom assoc-in ks m))

(defn assoc-in-ratom-evt
  "`assoc-in` the ratom with the `evt` target value."
  ([ratom ks f]
   (fn [evt]
     (r/rswap! ratom assoc-in ks (f (some-> evt .-target .-value)))
     (.preventDefault evt)
     nil))
  ([ratom ks]
   (assoc-in-ratom-evt ratom ks partial)))

;;
;; Alert
;;
;(defn alert-timeout
;  [alert-cursor]
;  (r/rswap! alert-cursor assoc :timer false)
;  (r/rswap! alert-cursor update :time + 4000)
;
;  (go
;    (a/<! (a/timeout (:time @alert-cursor)))
;    (when @alert-cursor
;      (r/rswap! alert-cursor update :time - 3000))
;    (when (>= 0 (:time @alert-cursor))
;      (reset! alert-cursor nil))))

(defn alert
  [alert-cursor]
  (let [{:keys [title status message alert-class badge-class timer]} @alert-cursor]
    ^{:key :alert}
    [:div.fixed-bottom.pointer-events-none
     ;{:class (when (and status timer)
     ;          (alert-timeout alert-cursor)
     ;          "show")}
     [:div.alert.alert-dismissible.m-0.p-1.rounded-0 {:class alert-class :role "alert"}
      [:small.id.badge.text-monospace.mr-2 {:class badge-class} status]
      [:strong.mr-1 title]
      [:span message]]]))

;;
;; AJAX Handlers
;;
(defn error-handler
  [alert-ratom]
  (fn [{:keys [response status]}]
    (merge-ratom!
      alert-ratom
      {:timer       true
       :title       "Holy guacamole!"
       :status      status
       :badge-class "badge-danger"
       :alert-class "alert-danger"
       :message     (-> response :error :message)})))

(defn success-handler
  ([alert-room message]
   (success-handler alert-room message "Yeah!" 201))
  ([alert-ratom message title status]
   (reset!
     alert-ratom
     {:timer       true
      :title       title
      :status      status
      :badge-class "badge-success"
      :alert-class "alert-success"
      :message     message})))

;;
;; AJAX
;;
;; TODO: refactor
(defn ajax-get-room!
  [board-ratom alert-ratom room-id]
  (GET
    (str uri "/simulations/" room-id)
    {:handler         #(reset! board-ratom
                               (merge % {:id room-id}))
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-get-rooms!
  [rooms-ratom alert-ratom]
  (GET
    (str uri "/simulations")
    {:handler         #(reset! rooms-ratom %)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-delete-room!
  [rooms-ratom alert-ratom room-id]
  (DELETE
    (str uri "/simulations/" room-id)
    {:handler         #(ajax-get-rooms! rooms-ratom alert-ratom)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-new-room!
  [rooms-ratom alert-ratom params]
  (POST
    (str uri "/simulations")
    {:params          params
     :handler         #((success-handler alert-ratom "Room created.")
                        (ajax-get-rooms! rooms-ratom alert-ratom)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-new-dinosaur!
  [board-ratom alert-ratom room-id params]
  (POST
    (str uri "/simulations/" room-id "/dinosaurs")
    {:params          params
     :handler         #((success-handler alert-ratom "Dinosaur created." "Roar!" 201)
                        (ajax-get-room! board-ratom alert-ratom room-id)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-new-robot!
  [board-ratom alert-ratom room-id params]
  (POST
    (str uri "/simulations/" room-id "/robots")
    {:params          params
     :handler         #((success-handler alert-ratom "Robot created." "Beep Bop!" 201)
                        (ajax-get-room! board-ratom alert-ratom room-id)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-robot-turn-left!
  [board-ratom alert-ratom room-id robot-id]
  (GET
    (str uri "/simulations/" room-id "/robots/" robot-id "/turn-left")
    {:handler         #((success-handler alert-ratom "Robot turned left." "Beeeeeeeep!" 200)
                        (ajax-get-room! board-ratom alert-ratom room-id)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-robot-turn-right!
  [board-ratom alert-ratom room-id robot-id]
  (GET
    (str uri "/simulations/" room-id "/robots/" robot-id "/turn-right")
    {:handler         #((success-handler alert-ratom "Robot turned right." "Boooooooop!" 200)
                        (ajax-get-room! board-ratom alert-ratom room-id)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-robot-move-forward!
  [board-ratom alert-ratom room-id robot-id]
  (GET
    (str uri "/simulations/" room-id "/robots/" robot-id "/move-forward")
    {:handler         #((success-handler alert-ratom "Robot moved forward." "Bzzzzzzz!" 200)
                        (ajax-get-room! board-ratom alert-ratom room-id)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-robot-move-backward!
  [board-ratom alert-ratom room-id robot-id]
  (GET
    (str uri "/simulations/" room-id "/robots/" robot-id "/move-backward")
    {:handler         #((success-handler alert-ratom "Robot moved backward." "Buzzzz!" 200)
                        (ajax-get-room! board-ratom alert-ratom room-id)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

(defn ajax-robot-attack!
  [board-ratom alert-ratom room-id robot-id]
  (GET
    (str uri "/simulations/" room-id "/robots/" robot-id "/attack")
    {:handler         #((success-handler alert-ratom "Robot attacked the Dinosaurs!" "Beep Bop Boom!" 200)
                        (ajax-get-room! board-ratom alert-ratom room-id)
                        nil)
     :error-handler   (error-handler alert-ratom)
     :format          :transit
     :response-format :transit, :keywords? true}))

;;
;; Modal
;;
(defn modal
  [modal-ratom]
  (let [{:keys                           [title body]
         {:keys [text on-click classes]} :button} @modal-ratom]
    ^{:key :modal}
    [:div#modal.modal.fade.rounded-0 {:tabIndex "-1" :role "dialog" :aria-hidden "true"}
     [:div.modal-dialog.modal-dialog-centered.rounded-0
      [:div.modal-content.rounded-0
       [:div.modal-header.bg-dark.text-white.rounded-0
        [:h5.modal-title title]]
       [:div.modal-body body]
       [:div.modal-footer
        [:button.btn.btn-secondary.rounded-0 {:data-dismiss "modal" :aria-label "close"} "Cancel"]
        [:button.rounded-0 {:class classes :on-click on-click :data-dismiss "modal" :aria-label "close"} text]]]]]))

(defn modal-delete-room
  [modal-ratom rooms-ratom alert-ratom {:keys [title id]}]
  (merge-ratom!
    modal-ratom
    {:title  "Confirm Deletion"
     :body   (str "Delete the room \"" title "\"?")
     :button {:text     "Delete"
              :classes  "btn btn-danger"
              :on-click (fn [evt]
                          (ajax-delete-room! rooms-ratom alert-ratom id)
                          (ajax-get-rooms! rooms-ratom alert-ratom)
                          (.preventDefault evt)
                          nil)}}))

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
                :classes  "btn btn-purple"
                :on-click (fn [evt]
                            (ajax-new-room! rooms-ratom alert-ratom @form-cursor)
                            (.preventDefault evt)
                            nil)}})))

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
   [:div.card.text-white.bg-dark.mb-2.rounded-0
    [:div.card-header "Simulation Room"
     [:div
      [:small.id.badge.badge-black.text-monospace.rounded-0 "ID " id]
      [:small.id.badge.badge-black.text-monospace.rounded-0.ml-2 width " x " height]
      ]]
    [:div.card-body.text-center
     [:h5.card-title.mb-4 title]
     ;[:p.card-text.text-monospace width " x " height]
     [:div.list-group
      [:button.btn.btn-lg.btn-purple.mb-2.rounded-0
       {:on-click (fn [evt]
                    ;(r/rswap! board-ratom assoc :id id)
                    (reset! page-ratom :board)
                    (ajax-get-room! board-ratom alert-ratom id)
                    (.preventDefault evt)
                    nil)}
       "Join »"]
      [:button.btn.btn-sm.btn-secondary.rounded-0
       {:data-toggle "modal"
        :data-target "#modal"
        :on-click    (fn [evt]
                       (modal-delete-room modal-ratom rooms-ratom alert-ratom room)
                       (.preventDefault evt)
                       nil)}
       "Delete"]]]]])

(defn room-card-new
  [rooms-ratom alert-ratom modal-ratom forms-ratom]
  ^{:key "new"}
  [:div.col-sm-4.order-last
   [:div.card.mb-2.rounded-0
    [:div.card-body.text-center
     [:h5.card-title.mb-4 "New Simulation room"]
     [:p.card-text "Click below to create a new simulation room"]
     [:div.list-group
      [:button.btn.btn-lg.btn-purple.mb-2.rounded-0
       {:data-target "#modal"
        :data-toggle "modal"
        :on-click    (fn [evt]
                       (modal-new-room modal-ratom rooms-ratom alert-ratom forms-ratom)
                       (.preventDefault evt)
                       nil)}
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
  [:button.btn.btn-sm.btn-black.m-2.rounded-0 {:on-click click-handler} text])

(defn button-refresh-rooms
  [rooms-ratom alert-ratom]
  (button-small-secondary
    "Refresh"
    (fn [evt]
      (reset! rooms-ratom nil)
      (ajax-get-rooms! rooms-ratom alert-ratom)
      (.preventDefault evt)
      nil)))

(defn button-refresh-board
  [board-ratom alert-ratom]
  (let [{:keys [id]} @board-ratom]
    (button-small-secondary
      "Refresh"
      (fn [evt]
        (reset! board-ratom nil)
        (ajax-get-room! board-ratom alert-ratom id)
        (.preventDefault evt)
        nil))))

(defn button-page-rooms
  [board-ratom page-ratom]
  (button-small-secondary
    "Back"
    (fn [evt]
      (reset! board-ratom nil)
      (reset! page-ratom :home)
      (.preventDefault evt)
      nil)))

;;
;; Spinner
;;
(defn spinner-loading
  [loading-ratom]
  (when-not @loading-ratom
    ^{:key :spinner-loading}
    [:div.d-flex.justify-content-center
     [:div.spinner-border.text-purple.m-5 {:role "status"}
      [:span.sr-only "Loading..."]]]))

;;
;; Jumbotron
;;
(defn jumbotron
  [header-ratom]
  (let [{:keys [title description subtitle children]} @header-ratom]
    ^{:key :jumbotron}
    [:div.jumbotron.jumbotron-fluid.bg-white.overflow-hidden.text-center.mt-2.mb-2.pt-2.pb-2
     [:header
      [:h1.display-5.mb-1.text-purple title]
      [:p.text-muted.text-gray description]
      [:hr.my-2]
      [:h5.text-gray subtitle]]
     children]))

;;
;; Container
;;
(defn container
  [children]
  [:div.container.mb-4
   (filter identity children)])

;;
;; Units
;;
(defn get-unit-class
  [units col row]
  (some->
    (fn [{{:keys [x y]} :point}]
      (and (= row y) (= col x)))
    (filter units)
    (first)
    (as->
      $
      (let
        [{:keys                 [id type subtype]
          {:keys [orientation]} :direction} $]
        {:class (str
                  "pixelated "
                  (name type)
                  (when orientation
                    (str " " (name type) "-" (name orientation)))
                  (when subtype
                    (str " " (name type) "-" (name subtype))))
         :id    id
         :type  (clojure.string/capitalize (name type))
         :subtype subtype}))))

;;
;; Adapters
;;
(defn point->str
  [x y]
  (if x
    (str "x " x " y " y)
    "x - y -"))

(defn unit-type->str
  [type subtype]
  (if type
    (if subtype
      (str (name type) " " (clojure.string/capitalize (name subtype)))
      (name type))
    "Empty"))

;;
;; Robot Control
;;

(defn robot-remote-control
  [board-ratom alert-ratom room-id robot-id x y]
  [:div.btn-group-vertical.btn-group-sm {:role "group" :aria-label "Remote Control"}

   [:button.btn.btn-dark.rounded-0
    {:type     "button"
     :on-click #(ajax-robot-turn-left!
                  board-ratom
                  alert-ratom
                  room-id
                  robot-id)}
    "Turn left"]

   [:button.btn.btn-dark.rounded-0
    {:type     "button"
     :on-click #(ajax-robot-turn-right!
                  board-ratom
                  alert-ratom
                  room-id
                  robot-id)}
    "Turn right"]

   [:button.btn.btn-dark.rounded-0
    {:type     "button"
     :on-click #(ajax-robot-move-forward!
                  board-ratom
                  alert-ratom
                  room-id
                  robot-id)}
    "Move forward"]

   [:button.btn.btn-dark.rounded-0
    {:type     "button"
     :on-click #(ajax-robot-move-backward!
                  board-ratom
                  alert-ratom
                  room-id
                  robot-id)}
    "Move backward"]

   [:button.btn.btn-dark.rounded-0
    {:type     "button"
     :on-click #(ajax-robot-attack!
                  board-ratom
                  alert-ratom
                  room-id
                  robot-id)}
    "Attack!"]])

(defn empty-remote-control
  [board-ratom alert-ratom x y room-id]
  [:div.btn-group-vertical.btn-group-sm {:role "group" :aria-label "Remote Control"}

   [:div.dropdown
    [:button#new-robot.btn.btn-dark.btn-sm.rounded-0.dropdown-toggle
     {:type          "button"
      :data-toggle   "dropdown"
      :aria-haspopup true
      :aria-expanded false}
     "New Robot"]
    (letfn
      [(new-robot
         [orientation]
         (fn [evt]
           (r/rswap! board-ratom assoc :remote-control nil)
           (ajax-new-robot!
             board-ratom
             alert-ratom
             room-id
             {:point {:x x :y y} :orientation orientation})
           (.preventDefault evt)))]
      [:div.dropdown-menu {:aria-labelledby "new-robot"}
       [:h6.dropdown-header "What's the facing direction?"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-robot "up")} "Up"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-robot "right")} "Right"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-robot "down")} "Down"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-robot "left")} "Left"]])]

   [:div.dropdown
    [:button#new-robot.btn.btn-dark.btn-sm.rounded-0.dropdown-toggle
     {:type          "button"
      :data-toggle   "dropdown"
      :aria-haspopup true
      :aria-expanded false}
     "New Dinosaur"]
    (letfn
      [(new-dinosaur
         [subtype]
         (fn [evt]
           (r/rswap! board-ratom assoc :remote-control nil)
           (ajax-new-dinosaur!
             board-ratom
             alert-ratom
             room-id
             {:point   {:x x
                        :y y}
              :subtype subtype})
           (.preventDefault evt)))]
      [:div.dropdown-menu {:aria-labelledby "new-dinosaur"}
       [:h6.dropdown-header "What Dinosaur?"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-dinosaur "vita")} "Vita"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-dinosaur "doux")} "Doux"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-dinosaur "tard")} "Tard"]
       [:button.dropdown-item.btn-sm
        {:type "button" :on-click (new-dinosaur "mort")} "Mort"]])
    ]])

;;
;; Remote Control
;;
(defn remote-control
  [board-ratom alert-ratom]
  (let [{room-id                                  :id
         {{:keys [id x y class type subtype]} :selection} :remote-control} @board-ratom]
    ^{:key :remote-control}
    [:div.remote-control.col-auto
     [:div.card
      [:div.card-body.text-center.p-2
       [:span.badge.badge-light.text-monospace (unit-type->str type subtype)]
       [:div.selection.border {:class class}]
       [:span.badge.badge-light.text-monospace (point->str x y)]]
      (when x
        (case type
          "Robot" (robot-remote-control
                    board-ratom
                    alert-ratom
                    room-id
                    id
                    x y)
          "Dinosaur" nil
          (empty-remote-control board-ratom alert-ratom x y room-id)))]]))

;;
;; Point
;;
(defn mouse-point
  [board-ratom]
  (let [{{:keys [x y]} :mouse-point} @board-ratom]
    ^{:key :mouse-point}
    [:div.row.justify-content-center.fixed-bottom.pointer-events-none
     [:div.col-auto.mb-4.pb-4.pointer-events-none
      [:span.badge.badge-dark.btn-purple.text-monospace
       (point->str x y)]]]))

;;
;; Scoreboard
;;
(defn scoreboard
  [board-ratom]
  (let [{{:keys [total]} :scoreboard} @board-ratom]
    ^{:key :scoreboard}
    [:div.row.justify-content-center
     [:div.col-auto.mb-3.mt-2
      [:div.coin.text-monospace
       [:span.badge.badge-warning.badge-lg.text-monospace.text-white (str "x " total)]]]]))

;;
;; Board
;;
(defn board-table
  [board-ratom alert-ratom]
  (let [{{:keys [units] {:keys [width height]} :size} :board} @board-ratom]
    ^{:key :board-table}
    [:div.row.justify-content-center.board-container.mb-4
     [:div.col-auto.mb-lg-4
      [:table.board.table.table-bordered.table-hover.table-sm.table-responsive-sm
       [:tbody
        ^{:key :board-table-body}
        (for [y (range 0 width)]
          ^{:key (str "tr-" y)}
          [:tr
           (for [x (range 0 height)]
             ^{:key (str "td-" x "-" y)}
             [:td.d-inline-block.p-0
              [:a.pointer
               (let [{:keys [class type id subtype]} (get-unit-class units x y)]
                 {:on-mouse-enter (fn [evt]
                                    (merge-ratom!
                                      board-ratom
                                      {:mouse-point {:x x :y y}})
                                    (.preventDefault evt)
                                    nil)
                  :on-mouse-leave (fn [evt]
                                    (merge-ratom!
                                      board-ratom
                                      {:mouse-point nil})
                                    (.preventDefault evt)
                                    nil)
                  :on-click       (fn [evt]
                                    (assoc-in-ratom!
                                      board-ratom
                                      [:remote-control :selection]
                                      {:id id :x x :y y :class class :type type :subtype subtype})
                                    (.preventDefault evt)
                                    nil)
                  :class          class})]])])]]]
     (remote-control board-ratom alert-ratom)]))


;;
;; Pages
;;
(defn board-page
  [page-ratom board-ratom header-ratom rooms-ratom alert-ratom modal-ratom forms-ratom]
  (let [{:keys [id title board]} @board-ratom]
    ;(when-not board
    ;  (ajax-get-room! board-ratom alert-ratom id))

    (merge-ratom!
      header-ratom
      {:subtitle (str "Board: " title)
       :children [:div
                  (button-refresh-board board-ratom alert-ratom)
                  (button-page-rooms board-ratom page-ratom)]})

    (container [(alert alert-ratom)
                (modal modal-ratom)
                (jumbotron header-ratom)
                (spinner-loading board-ratom)
                (scoreboard board-ratom)
                (board-table board-ratom alert-ratom)
                (mouse-point board-ratom)])))

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
   :board  {:id 16}
   :page   :home
   :forms  {:new-room {:title ""
                       :size  {:width  "50"
                               :height "50"}}}
   :modal  {:title  "Modal"
            :body   ""
            :button {:text     "Ok"
                     :classes  "btn btn-purple"
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

    (ajax-get-rooms! rooms-cursor alert-cursor)

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

(defn ^:export init!
  "Init the app."
  []
  (mount-root))
