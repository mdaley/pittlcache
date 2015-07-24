# pittlcache

A per-item TTL cache based on clojure/core.cache.

## Usage

As per the TTL cache in core.cache with the addition that when setting the value of an item in the cache a map with :value and :ttl (in milliseconds) needs to be used. If only the value is specified in the map, the default TTL of the cache is used as the TTL for the item. Seeding with TTLed values is possible as well. 

Example from the tests:

```clojure
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
  ```
  
  ## License
  
Copyright © 2015 Matthew Daley

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
