(ns masques.view.main.group-tab
  (:require [clj-internationalization.term :as term]
            [masques.controller.actions.utils :as action-utils]
            [masques.model.friend :as friend-model]
            [seesaw.core :as seesaw-core]
            [seesaw.table :as seesaw-table])
  (:import [javax.swing DefaultListModel]
           [javax.swing.event ListSelectionListener]))

(def tab-name (term/group))

(def member-table-columns [ { :key :name :text (term/handle) } ])

(defn create-member-list-buttons []
  (seesaw-core/horizontal-panel :items 
    [ (seesaw-core/button :id :add-member-button :text (term/add))
      [:fill-h 3]
      (seesaw-core/button :id :remove-member-button :text (term/remove))]))

(defn create-member-list-header-panel []
  (seesaw-core/border-panel
    :west (term/group-members)
    :east (create-member-list-buttons)))

(defn create-member-list-table []
  (seesaw-core/scrollable
    (seesaw-core/table :id :member-table :preferred-size [600 :by 300]
      :model [ :columns member-table-columns ])))

(defn create-member-table-panel []
  (seesaw-core/border-panel
    :vgap 5
    :north (create-member-list-header-panel)
    :center (create-member-list-table)))

(defn create-group-list-buttons []
  (seesaw-core/horizontal-panel :items 
    [ (seesaw-core/button :id :new-group-button :text (term/new))
      [:fill-h 3]
      (seesaw-core/button :id :delete-group-button :text (term/delete)) ]))

(defn create-group-list-header-panel []
  (seesaw-core/border-panel
    :west (term/groups)
    :east (create-group-list-buttons)))

(defn group-list-cell-renderer
  "A cell renderer for the group list. This function simply converts the group given in the arg list to a label containing the name of the group."
  [renderer arg-map]
  (seesaw-core/config! renderer :text (:name (:value arg-map))))

(defn create-group-list []
  (seesaw-core/scrollable
    (seesaw-core/listbox
      :id :group-listbox
      :model (new DefaultListModel)
      :renderer group-list-cell-renderer)))

(defn create-group-list-panel []
  (seesaw-core/border-panel
    :vgap 5
    :north (create-group-list-header-panel)
    :center (create-group-list)))

(defn create []
  (seesaw-core/vertical-panel
    :id :group-tab-panel
    :border 9
    :items [(create-group-list-panel) [:fill-v 9] (create-member-table-panel)]))

(defn group-panel [main-frame]
  (seesaw-core/select main-frame ["#group-tab-panel"]))

(defn find-member-table [main-frame]
  (seesaw-core/select main-frame ["#member-table"]))

(defn find-group-list [main-frame]
  (seesaw-core/select main-frame ["#group-listbox"]))

(defn find-add-member-button [main-frame]
  (seesaw-core/select main-frame ["#add-member-button"]))

(defn find-remove-member-button [main-frame]
  (seesaw-core/select main-frame ["#remove-member-button"]))

(defn find-new-group-button [main-frame]
  (seesaw-core/select main-frame ["#new-group-button"]))

(defn find-delete-group-button [main-frame]
  (seesaw-core/select main-frame ["#delete-group-button"]))

(defn attach-listener-to-delete-group-button
  "Attaches the given listener to the delete-group-button."
  [main-frame listener]
  (action-utils/attach-listener (find-delete-group-button main-frame) listener)
  main-frame)

(defn convert-to-table-friend [friend]
  { :id (:id friend) :name (or (:name friend) (friend-model/friend-name friend)) })

(defn reset-member-table
  "Resets the member table to only include the given friends."
  [main-frame friends]
  (seesaw-core/config! (find-member-table main-frame)
      :model [:columns member-table-columns
              :rows (map convert-to-table-friend friends)]))

(defn add-member
  "Adds the given friend to the member table."
  [main-frame friend]
  (seesaw-table/insert-at! (find-member-table main-frame) 0 (convert-to-table-friend friend)))

(defn member-count
  "Returns the number of rows in the member table."
  [main-frame]
  (seesaw-table/row-count (find-member-table main-frame)))

(defn all-members
  "Returns all of the members from the member table."
  [main-frame]
  (seesaw-table/value-at (find-member-table main-frame) (range (member-count main-frame))))

(defn all-member-pairs
  "Returns all the members from the member table as pairs with the index of the member in the table."
  [main-frame]
  (map #(list %1 %2) (range) (all-members main-frame)))

(defn find-member-pair
  "Returns the friend pair for the given friend if it is in the table."
  [main-frame friend]
  (when-let [friend-id (if (map? friend) (:id friend) friend)]
    (first (filter #(= friend-id (:id (second %))) (all-member-pairs main-frame)))))

(defn find-member-index
  "Returns the index of the given friend."
  [main-frame friend]
  (when-let [found-member-pair (find-member-pair main-frame friend)]
    (first found-member-pair)))

(defn delete-member [main-frame friend]
  (seesaw-table/remove-at! (find-member-table main-frame) (find-member-index main-frame friend)))

(defn find-selected-member
  "Returns the selected member in the member table."
  [main-frame]
  (when-let [member-table (find-member-table main-frame)]
    (let [selected-row (.getSelectedRow member-table)]
      (when (>= selected-row 0)
        (seesaw-table/value-at member-table selected-row)))))

(defn set-selected-member
  "Selects the given member in the member table."
  [main-frame friend]
  (when-let [friend-index (find-member-index main-frame friend)]
    (when (>= friend-index 0)
      (.setRowSelectionInterval (find-member-table main-frame) friend-index friend-index))))

(defn group-list-model
  "Returns the model of the group list."
  [main-frame]
  (seesaw-core/get-model* (find-group-list main-frame)))

(defn groups
  "Returns a sequence of groups from the list."
  [main-frame]
  (let [group-model (group-list-model main-frame)]
    (doall (map #(.getElementAt group-model %) (range (.getSize group-model))))))

(defn group-index
  "Returns the index of the given group in the group list."
  [main-frame group]
  (some #(when (= (:id (second %1)) (:id group)) (first %1))
        (map #(list %1 %2) (range) (groups main-frame))))

(defn clear-groups
  "Removes all groups from the group list."
  [main-frame]
  (.removeAllElements (group-list-model main-frame)))

(defn add-group
  "Adds the given group to the group list."
  [main-frame group]
  (.addElement (group-list-model main-frame) group))

(defn set-groups
  "Sets the groups list to the given groups."
  [main-frame groups]
  (clear-groups main-frame)
  (doseq [group groups]
    (add-group main-frame group)))

(defn remove-group
  "Adds the given group to the group list."
  [main-frame group]
  (.removeElement (group-list-model main-frame) group))

(defn selected-group
  "Returns the first group selected in the group list."
  [main-frame]
  (.getSelectedValue (find-group-list main-frame)))

(defn set-selected-group-index
  "Sets the selected group to the given index."
  [main-frame index]
  (when (>= index 0)
    (.setSelectedIndex (find-group-list main-frame) index)))

(defn set-selected-group
  "Selects the given group in the group list."
  [main-frame group]
  (set-selected-group-index main-frame (group-index main-frame group)))

(defn group-count
  "Returns the number of rows in the member table."
  [main-frame]
  (.getSize (group-list-model main-frame)))

(defn add-group-list-selection-listener [main-frame listener]
  (.addListSelectionListener (find-group-list main-frame)
    (reify ListSelectionListener
      (valueChanged [this list-selection-event]
                    (listener list-selection-event))))
  main-frame)

(defn click-new-group-button
  "Clicks the new group button."
  [main-frame]
  (.doClick (find-new-group-button main-frame)))

(defn click-delete-group-button
  "Clicks the delete group button."
  [main-frame]
  (.doClick (find-delete-group-button main-frame)))

(defn click-add-member-button
  "Clicks the add member button."
  [main-frame]
  (.doClick (find-add-member-button main-frame)))

(defn click-remove-member-button
  "Clicks the remove member button."
  [main-frame]
  (.doClick (find-remove-member-button main-frame)))