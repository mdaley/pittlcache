(ns pittlcache.core
  (:require [clojure.core.cache :refer :all]))

(defn- valid-data
  "Data to be stored in the cache must be a map with a :value and an optional :ttl in milliseconds."
  [value]
  (and (:value value)
       (empty? (dissoc value :value :ttl))))

(defn- valid-base
  "Base data must contain valid data items or be empty."
  [base]
  (every? valid-data (vals base)))

(defn- kill-old
  "Find all the expired items and remove them."
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
            (:value (get cache item))
            not-found))
  (has? [_ item]
        (when-let [e (get expiry item)]
          (> e (System/currentTimeMillis))))
  (hit [this item]
       (if-let [e (get expiry item)]
         (if (> e (System/currentTimeMillis))
           (PITTLCache. cache
                        (assoc expiry item (+ (get expiry item) (:ttl (get cache item))))
                        default-ttl)
           this)
         this))
  (miss [this item result]
        (assert (valid-data result) "value must be a map containing :value and an optional :ttl")
        (let [now (System/currentTimeMillis)
              [updated-cache updated-expiry] (kill-old cache expiry now)]
          (PITTLCache. (assoc updated-cache item (merge {:ttl default-ttl} result))
                       (assoc updated-expiry item (+ now (or (:ttl result) default-ttl)))
                       default-ttl)))
  (seed [_ base]
        (assert (valid-base base)
                "All values must be maps containing a :value and an optional :ttl")
        (let [now (System/currentTimeMillis)]
          (PITTLCache. base
                       (into {} (for [x base] [(key x) (+ now (or (:ttl (val x)) default-ttl))]))
                       default-ttl)))
  (evict [_ key]
         (PITTLCache. (dissoc cache key)
                      (dissoc expiry key)
                      default-ttl))
  Object
  (toString [_]
            (str cache \, \space expiry \, \space default-ttl)))

(defn pittl-cache-factory
  "Returns a Per Item TTL cache with the cache and expiration table initialised to the value
  of `base`. An optional `:ttl` argument defines the default TTL in milliseconds for cache
  items that don't specify their own TTL."
  [base & {ttl :ttl :or {ttl 2000}}]
  {:pre [(number? ttl) (<= 0 ttl)
         (map? base)]}
  (seed (PITTLCache. {} {} ttl) base))
