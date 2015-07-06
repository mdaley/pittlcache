(ns pittlcache.core-test
  (:require [pittlcache.core :refer :all]
            [clojure.core.cache :as cache]
            [midje.sweet :refer :all])
  (:import [java.lang AssertionError]))

(defn- snooze
  ([ms]
   (Thread/sleep ms))
  ([v ms]
   (Thread/sleep ms)
   v))

(fact-group

 (fact "value can be stored and retrieved from cache"
       (-> (pittl-cache-factory {} :ttl 100)
           (cache/miss :a {:value "a"})
           (cache/lookup :a)) => "a")

 (fact "value can be stored but not retrieved once it has expired"
       (-> (pittl-cache-factory {} :ttl 100)
           (cache/miss :a {:value "a"})
           (snooze 150)
           (cache/lookup :a)) => nil)

 (fact "value can be stored with own ttl and retrieved from cache with smaller ttl"
       (-> (pittl-cache-factory {} :ttl 100)
           (cache/miss :a {:value "a" :ttl 1000})
           (snooze 150)
           (cache/lookup :a)) => "a")

 (fact "value can be stored with own ttl and can't be retrieved once its ttl has expired"
       (-> (pittl-cache-factory {} :ttl 100)
           (cache/miss :a {:value "a" :ttl 200})
           (snooze 250)
           (cache/lookup :a)) => nil)

 (fact "has? returns true before expiry and false after expiry"
       (let [c (-> (pittl-cache-factory {:a {:value "a" :ttl 100}}))]
         (cache/has? c :a) => true
         (snooze 150)
         (cache/has? c :a) => falsey))

 (fact "multiple values can be stored and retrieved"
       (let [c (-> (pittl-cache-factory {} :ttl 100)
                   (cache/miss :a {:value "a" :ttl 1000})
                   (cache/miss :b {:value 123 :ttl 1000})
                   (cache/miss :c {:value {:some "map"} :ttl 1000}))]
         (cache/lookup c :a) => "a"
         (cache/lookup c :b) => 123
         (cache/lookup c :c) => {:some "map"}))

 (fact "multiple values can be stored and the expire according to their own ttls"
       (let [c (-> (pittl-cache-factory {} :ttl 10)
                   (cache/miss :a {:value "a" :ttl 100})
                   (cache/miss :b {:value 123 :ttl 200})
                   (cache/miss :c {:value {:some "map"} :ttl 300}))]
         (cache/lookup c :a) => "a"
         (cache/lookup c :b) => 123
         (cache/lookup c :c) => {:some "map"}
         (snooze 150)
         (cache/lookup c :a) => nil
         (cache/lookup c :b) => 123
         (cache/lookup c :c) => {:some "map"}
         (snooze 100)
         (cache/lookup c :a) => nil
         (cache/lookup c :b) => nil
         (cache/lookup c :c) => {:some "map"}
         (snooze 100)
         (cache/lookup c :a) => nil
         (cache/lookup c :b) => nil
         (cache/lookup c :c) => nil))

 (fact "hitting an item extends its ttl"
       (-> (pittl-cache-factory {} :ttl 10)
           (cache/miss :a {:value "a" :ttl 100})
           (snooze 80)
           (cache/hit :a)
           (snooze 100)
           (cache/lookup :a)) => "a")

 (fact "cache can be seeded with some values which then expire as expected"
       (let [c (-> (pittl-cache-factory {:a {:value "a"} :b {:value "b" :ttl 100}} :ttl 200))]
         (cache/lookup c :a) => "a"
         (cache/lookup c :b) => "b"
         (snooze 150)
         (cache/lookup c :a) => "a"
         (cache/lookup c :b) => nil
         (snooze 100)
         (cache/lookup c :a) => nil))

 (fact "evict immediately removes a value"
       (-> (pittl-cache-factory {} :ttl 10000)
           (cache/miss :a {:value "a"})
           (cache/evict :a)
           (cache/lookup :a)) => nil)

 (fact "attempt to cache data fails when data is not a map"
       (-> (pittl-cache-factory {})
           (cache/miss :a "a")) => (throws AssertionError))

 (fact "attempt to cache data fails when data contains too many fields"
       (-> (pittl-cache-factory {})
           (cache/miss :a {:value "a" :ttl 100 :bad "field"})) => (throws AssertionError))

 (fact "attempt to construct cache with invalid base data fails"
       (pittl-cache-factory {:a "a"}) => (throws AssertionError)))
