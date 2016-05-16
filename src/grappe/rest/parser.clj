(ns grappe.rest.parser
  (:require [cheshire.core :refer :all]
            [plumbing.core :refer :all]))

;; Ensure compatibility with the Eve python framework query format
;; http://python-eve.org
(defn parse-eve-params [request]
  (let [{:strs [where sort max_results page projection embedded]} (:query-params request)
        where (parse-string where true)
        sort-fields (if sort
                      (->> (clojure.string/split (or sort "") #",")
                           (map (fn [field] (if (.startsWith field "-") [(keyword (clojure.string/replace-first field "-" "")) -1] [(keyword field) 1])))
                           flatten
                           (apply array-map)))
        projection (if-let [p projection]
                     (->> (parse-string p)
                          keys
                          (into [])))
        relations (if embedded
                    (->> (parse-string embedded true)
                         (map (fn [[k v]] [k {}]))
                         (into {})))
        page (if page (read-string page))
        per-page (if max_results (read-string max_results))]
    ;; where is the query that is to be transmitted to the mongoDB data layer directly
    ;; it is validated in the validate-mongo-find phase (query.clj)
    ;; for authorized resources an and clause is added in order to restrict access
    {:find      where
     :sort      sort-fields
     :fields    projection
     :paginate  {:page page :per-page per-page}
     :relations relations}))

(defn parse-query [request]
  (let [query (if-let [query (get-in request [:query-params "query"])]
                (parse-string query true)
                (parse-eve-params request))]
    (update-in query [:find] #(merge % (:route-params request {})))))

(defn format-eve-response [response]
  (let [body (:body response)
        meta (if (:_count body)
               {:total (:_count body)
                :max_results (get-in body [:query :paginate :per-page])
                :page (get-in body [:query :paginate :page])})]
    (-> body
        (?> (:_documents body) (clojure.set/rename-keys {:_documents :_items}))
        (?> meta (assoc :_meta meta)))))
