(ns clj-dynamo-helpers.core
  (:import (java.nio ByteBuffer)))

(declare map->attributevalue dynamo->clojure)


(defn- value->attributevalue [v]
  (cond
    (map? v) {:M (map->attributevalue v)}
    (vector? v) {:L (mapv value->attributevalue v)}
    (integer? v) {:N (str v)}
    (float? v) {:N (str v)}
    (boolean? v) {:BOOL v}
    (nil? v) {:NULL true}
    (bytes? v) {:B v}
    (instance? ByteBuffer v) {:B v}
    (and (set? v) (every? string? v)) {:SS (vec v)}
    (and (set? v) (every? number? v)) {:NS (vec (map str v))}
    (and (set? v) (every? #(instance? ByteBuffer %) v)) {:BS (vec v)}
    (string? v) {:S v}
    :else (throw (ex-info "Unsupported DynamoDB type"
                          {:value v :type (type v)}))))

(defn map->attributevalue [m]
  (into {}
        (for [[k v] m]
          [(keyword k) (value->attributevalue v)])))

(defn- parse-number [value]
  (if (re-find #"\." value)
    (Double/parseDouble value)
    (Integer/parseInt value)))

(defn- convert-value [[type value]]
  (case type
    :S value
    :N (parse-number value)
    :BOOL value
    :NULL nil
    :B value
    :SS (set value)
    :NS (set (map parse-number value))
    :BS (set value)
    :L (mapv #(convert-value (first %)) value)
    :M (dynamo->clojure value)
    value))

(defn dynamo->clojure [m]
  (into {} (map (fn [[k v]]
                  [k (convert-value (first v))]) m)))