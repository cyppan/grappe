(ns grappe.hooks.default-sort)

(def hooks
  {:pre-fetch (fn [{:keys [config]} resource request query]
                (if (get-in query [:opts :sort?])
                  (let [query-sort (:sort query)
                        resource-sort (:default-sort resource)
                        config-sort (:default-sort config)]
                    (assoc-in query [:sort] (or query-sort resource-sort config-sort)))
                  query))})
