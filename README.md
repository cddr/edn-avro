# edn-avro

[![Tests](https://github.com/cddr/edn-avro/actions/workflows/kaocha.yml/badge.svg) |
[![Current Release](https://img.shields.io/clojars/v/cddr/edn-avro.svg)](https://clojars.org/cddr/edn-avro)

Conversion between EDN and Avro GenericDatums (and back)

## Rationale

A simple bridge between clojure data and avro

## Usage

```
(ns my.cool.app
  (:require
    [cddr.edn-avro :as avro])
  (:import
    (java.util Properties)
    (org.apache.kafka.clients.producer KafkaProducer)))

(def schema {:type "record"
             :name "Foo"
             :fields [{:name "aString" :type "string"}
                      {:name "aUnion" :type ["string" "null"]}]})

(def cfg (doto (Properties.)
           (.put "bootstrap.servers" "localhost:9092")
           (.put "acks" "all")
           (.put "key.serializer" "io.confluent.kafka.serializers.KafkaAvroSerializer")
           (.put "value.serializer" "io.confluent.kafka.serializers.KafkaAvroSerializer")))

(def p (KafkaProducer. cfg))

@(.send p (as-avro {:aString "test-1"
                    :aUnion "test-2"}
                   {:schema schema}))

@(.send p (as-avro {:aString "test-1"
                    :aUnion nil}
                   {:schema schema}))

```

## License

Copyright © 2026 Andy Chambers


Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
