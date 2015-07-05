(ns pittlcache.core
  (:require [clojure.core.cache :refer :all]))

(defn- valid-base
  [base]
  (every? #(and (:value %)
                (empty? (dissoc % :value :ttl)))
          (vals base)))

(defn- kill-old
  [cache expiry now]
  (let [ks (map key (filter #(< (val %) now) expiry))]
    [(apply dissoc cache ks)
     (apply dissoc expiry ks)]))

(defcache PITTLCache [cache expiry default-ttl]
  CacheProtocol
  (lookup [this item]
          (let [ret (lookup this item ::nope)]
            (when-not (= ret ::nope) ret)))
  (lookup [this item not-found]
          (if (has? this item)
            (get cache item)
            not-found))
  (has? [_ item]
        (when-let [e (get expiry item)]
          (println "HAS?" item e (System/currentTimeMillis))
          (> e (System/currentTimeMillis))))
  (hit [this item] this)
  (miss [this item result]
        (assert (and (:value result)
                     (nil? (dissoc result :value :ttl)))
                "value must be a map containing :value and an optional :ttl")
        (println "MISS" this item result)
        (let [now (System/currentTimeMillis)
              [updated-cache updated-expiry] (kill-old cache expiry now)]
          (PITTLCache. (assoc updated-cache item (:value result))
                       (assoc updated-expiry item (+ now (or (:ttl result) default-ttl)))
                       default-ttl)))
  (seed [_ base]
        (assert (valid-base base)
                "All values must be maps containing a :value and an optional :ttl")
        (let [now (System/currentTimeMillis)]
          (PITTLCache. base
                       (into {} (for [x base] [(key x) (+ now (or (:ttl x) default-ttl))]))
                       default-ttl)))
  (evict [_ key]
         (PITTLCache. (dissoc cache key)
                      (dissoc expiry key)
                      default-ttl))
  Object
  (toString [_]
            (str cache \, \space expiry \, \space default-ttl)))

(defn pittl-cache-factory
  "Returns a Per Item TTL cache with the cache and expiration-table initialied to `base`
   -- each with the same default time-to-live.
   This function also allows an optional `:ttl` argument that defines the default
   time in milliseconds that entries are allowed to reside in the cache."
  [base & {ttl :ttl :or {ttl 2000}}]
  {:pre [(number? ttl) (<= 0 ttl)
         (map? base)]}
  (seed (PITTLCache. {} {} ttl) base))
