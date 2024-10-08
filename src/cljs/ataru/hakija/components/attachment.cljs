(ns ataru.hakija.components.attachment
  (:require [re-frame.core :refer [subscribe dispatch]]
            [ataru.application-common.application-field-common
             :as application-field]
            [ataru.hakija.components.question-hakukohde-names-component :as hakukohde-names-component]
            [ataru.hakija.components.generic-label-component :as generic-label-component]
            [ataru.util :as util]
            [ataru.translations.translation-util :as tu]
            [reagent.core :as r]
            [ataru.feature-config :as fc]
            [clojure.string]))

(defonce autocomplete-off "new-password")

(defonce max-attachment-size-bytes
         (get (js->clj js/config) "attachment-file-max-size-bytes" (* 10 1024 1024)))

(defn- upload-attachment [field-descriptor question-group-idx event]
  (.preventDefault event)
  (let [file-list (or (some-> event .-dataTransfer .-files)
                      (.. event -target -files))
        files     (->> (.-length file-list)
                       (range)
                       (map #(.item file-list %)))]
    (dispatch [:application/add-attachments field-descriptor question-group-idx files])))

(defn- deadline-info [deadline]
  (let [lang @(subscribe [:application/form-language])]
    (prn "deadline" deadline)
    [:div.application__form-upload-attachment--deadline
     (str (tu/get-hakija-translation :deadline-in lang) " " deadline)]))

(defn- attachment-upload [field-descriptor component-id attachment-count question-group-idx]
  (let [id (str component-id (when question-group-idx (str "-" question-group-idx)) "-upload-button")
        lang @(subscribe [:application/form-language])]
    [:div.application__form-upload-attachment-container
     [:input.application__form-upload-input.visually-hidden
      {:id           id
       :type         "file"
       :multiple     "multiple"
       :key          (str "upload-button-" component-id "-" attachment-count)
       :on-change    (partial upload-attachment field-descriptor question-group-idx)
       :required     (application-field/is-required-field? field-descriptor)
       :aria-invalid (not (:valid @(subscribe [:application/answer id question-group-idx nil])))
       :autoComplete autocomplete-off}]
     [:label.application__form-upload-label
      {:for id}
      [:i.zmdi.zmdi-cloud-upload.application__form-upload-icon]
      [:span.application__form-upload-button-add-text (tu/get-hakija-translation :add-attachment lang)]]
     [:span.application__form-upload-button-info
      [:div
       (tu/get-hakija-translation :file-size-info lang (util/size-bytes->str max-attachment-size-bytes))]
      (when-let [deadline @(subscribe [:application/attachment-deadline field-descriptor])]
        (deadline-info deadline))]]))

(defn- attachment-filename
  [id question-group-idx attachment-idx show-size?]
  (let [file @(subscribe [:application/answer
                          id
                          question-group-idx
                          attachment-idx])
        link @(subscribe [:application/attachment-download-link (:value file)])]
    [:div
     (if (and (:final file)
              (fc/feature-enabled? :attachment-download-allowed))
       [:a.application__form-attachment-filename
        {:href link}
        (:filename file)]
       [:span.application__form-attachment-filename
        (:filename file)])
     (when (and (some? (:size file)) show-size?)
       [:span (str " (" (util/size-bytes->str (:size file)) ")")])]))

(defn- attachment-remove-button
  [field-descriptor _ _]
  (let [id       (keyword (:id field-descriptor))
        confirm? (r/atom false)]
    (fn [field-descriptor question-group-idx attachment-idx]
      (let [lang         @(subscribe [:application/form-language])
            cannot-edit? @(subscribe [:application/cannot-edit? id])]
        [:div.application__form-attachment-remove-button-container
         (when-not cannot-edit?
           [:button.application__form-attachment-remove-button
            {:on-click #(swap! confirm? not)}
            (if @confirm?
              (tu/get-hakija-translation :cancel-remove lang)
              (tu/get-hakija-translation :remove lang))])
         (when @confirm?
           [:button.application__form-attachment-remove-button.application__form-attachment-remove-button__confirm
            {:on-click (fn [_]
                         (reset! confirm? false)
                         (dispatch [:application/remove-attachment
                                    field-descriptor
                                    question-group-idx
                                    attachment-idx]))}
            (tu/get-hakija-translation :confirm-remove lang)])]))))

(defn- cancel-attachment-upload-button
  [_ _ _]
  (let [confirm? (r/atom false)
        lang     (subscribe [:application/form-language])]
    (fn [field-descriptor question-group-idx attachment-idx]
      [:div.application__form-attachment-remove-button-container
       [:button.application__form-attachment-remove-button
        {:on-click #(swap! confirm? not)}
        (if @confirm?
          (tu/get-hakija-translation :cancel-cancel-upload @lang)
          (tu/get-hakija-translation :cancel-upload @lang))]
       (when @confirm?
         [:button.application__form-attachment-remove-button.application__form-attachment-remove-button__confirm
          {:on-click (fn [_]
                       (reset! confirm? false)
                       (dispatch [:application/cancel-attachment-upload
                                  field-descriptor
                                  question-group-idx
                                  attachment-idx]))}
          (tu/get-hakija-translation :confirm-cancel-upload @lang)])])))

(defn- attachment-view-file [field-descriptor component-id question-group-idx attachment-idx]
  [:div.application__form-attachment-list-item-container
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container.application__form-attachment-filename-container__success
    [attachment-filename component-id question-group-idx attachment-idx true]]
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-check-mark-container
    [:i.zmdi.zmdi-check.application__form-attachment-check-mark]]
   [:div.application__form-attachment-list-item-sub-container
    [attachment-remove-button field-descriptor question-group-idx attachment-idx]]])

(defn- attachment-view-file-error [field-descriptor component-id question-group-idx attachment-idx]
  (let [attachment @(subscribe [:application/answer
                                component-id
                                question-group-idx
                                attachment-idx])
        lang       @(subscribe [:application/form-language])]
    [:div.application__form-attachment-list-item-container
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container.application__form-attachment-filename-container__error
      [attachment-filename component-id question-group-idx attachment-idx true]]
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-error-container
      (doall
        (map-indexed (fn [i [error params]]
                       ^{:key (str "attachment-error-" i)}
                       [:span.application__form-attachment-error
                        (tu/get-hakija-translation error lang params)])
                     (:errors attachment)))]
     [:div.application__form-attachment-list-item-sub-container
      [attachment-remove-button field-descriptor question-group-idx attachment-idx]]]))

(defn- attachment-deleting-file [_ component-id question-group-idx attachment-idx]
  [:div.application__form-attachment-list-item-container
   [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container
    [attachment-filename component-id question-group-idx attachment-idx true]]])

(defn- attachment-uploading-file
  [field-descriptor component-id question-group-idx attachment-idx]
  (let [attachment       @(subscribe [:application/answer component-id question-group-idx attachment-idx])
        size             (:size attachment)
        uploaded-size    (:uploaded-size attachment)
        upload-complete? (<= size uploaded-size)
        percent          (int (* 100 (/ uploaded-size size)))
        lang             @(subscribe [:application/form-language])]
    [:div.application__form-attachment-list-item-container
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-filename-container
      [attachment-filename component-id question-group-idx attachment-idx false]]
     [:div.application__form-attachment-list-item-sub-container.application__form-attachment-uploading-container
      [:i.zmdi.zmdi-spinner.application__form-upload-uploading-spinner]
      [:span (str (tu/get-hakija-translation
                    (if upload-complete? :processing-file :uploading)
                    lang)
                  "... ")]
      [:span (str percent " % "
                  "(" (util/size-bytes->str uploaded-size false)
                  "/"
                  (util/size-bytes->str size) ")")]]
     [:div.application__form-attachment-list-item-sub-container
      [cancel-attachment-upload-button field-descriptor question-group-idx attachment-idx]]]))

(defn- attachment-row [field-descriptor component-id attachment-idx question-group-idx status]
  [:li.application__attachment-filename-list-item
   [(case status
      :ready attachment-view-file
      :error attachment-view-file-error
      :uploading attachment-uploading-file
      :deleting attachment-deleting-file)
    field-descriptor component-id question-group-idx attachment-idx]])

(defn attachment [{:keys [id] :as field-descriptor} question-group-idx]
  (let [languages              @(subscribe [:application/default-languages])
        text                   (util/non-blank-val (get-in field-descriptor [:params :info-text :value]) languages)
        attachments            @(subscribe [:application/attachments id question-group-idx])
        visible-attachments    @(subscribe [:application/visible-attachments id question-group-idx])
        attachment-count       (count attachments)
        application-identifier @(subscribe [:application/application-identifier])
        extra-class            (when (:original-question field-descriptor)
                                 ".application__form-field--attachment-with-address")]
    [(keyword (str "div.application__form-field" extra-class))
     [generic-label-component/generic-label field-descriptor question-group-idx]
     (when (application-field/belongs-to-hakukohde-or-ryhma? field-descriptor)
       [hakukohde-names-component/question-hakukohde-names field-descriptor :liitepyynto-for-hakukohde])
     (when-not (clojure.string/blank? text)
       [application-field/markdown-paragraph text (-> field-descriptor :params :info-text-collapse) application-identifier])
     (when (not-empty visible-attachments)
       [:ol.application__attachment-filename-list
        (doall (map (fn [[attachment-idx attachment]]
                      ^{:key (str "attachment-" (when question-group-idx (str question-group-idx "-")) id "-" attachment-idx)}
                      [attachment-row field-descriptor id attachment-idx question-group-idx (:status attachment)])
                    visible-attachments))])
     (if (get-in field-descriptor [:params :mail-attachment?])
       [:<>
        (when-let [address @(subscribe [:application/attachment-address field-descriptor])]
          [application-field/markdown-paragraph address])
        (when-let [deadline @(subscribe [:application/attachment-deadline field-descriptor])]
          [:div.application__mail-attachment--deadline
           [deadline-info deadline]])]
       (when-not @(subscribe [:application/cannot-edit? (keyword id)])
         [attachment-upload field-descriptor id attachment-count question-group-idx]))]))

(defn- attachment-list-readonly [attachments]
  (let [visible-attachments (filter #(not= (:status %) :deleting) attachments)]
    [:div
     (map (fn [value]
            ^{:key (str "attachment-" (:value value))}
            [:ul.application__form-field-list (str (:filename value) " (" (util/size-bytes->str (:size value)) ")")])
       visible-attachments)]))

(defn attachment-readonly [field-descriptor application lang question-group-index]
  (let [answer-key (keyword (application-field/answer-key field-descriptor))
        values     (if question-group-index
                     (-> application
                       :answers
                       answer-key
                       :values
                       (nth question-group-index nil))
                     (-> application :answers answer-key :values))]
    [:div.application__form-field
     [:div.application__form-field-label
      [:span (util/from-multi-lang (:label field-descriptor) lang)
        [:span.application__form-field-label.application__form-field-label--required (application-field/required-hint field-descriptor lang)]]]
     (when-let [address @(subscribe [:application/attachment-address field-descriptor])]
       [application-field/markdown-paragraph address])
     (when-let [deadline @(subscribe [:application/attachment-deadline field-descriptor])]
       [:div.application__mail-attachment--deadline
        [deadline-info deadline]])
     [attachment-list-readonly values]]))
