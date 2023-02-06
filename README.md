# edn-avro

[![Tests](https://github.com/cddr/edn-avro/actions/workflows/kaocha.yml/badge.svg) |
[![Current Release](https://img.shields.io/clojars/v/cddr/edn-avro.svg)](https://clojars.org/cddr/edn-avro)

Conversion between EDN and Avro GenericDatums (and back)

## Rationale

Clojure has an excellent library for serializing Clojure objects to avro in
the form of abracad.avro. It works great for scenarios where you're writing
the schema once in the header section of a file or something, and then for
the rest of the records you just write out the raw bytes.

However when working with the confluent schema registry, you need to produce
avro values that come in a container that includes the schema (the serializer
can be configured to fail if the provided schema is not compatible with
those that have been registered already on the same kafka topic).

This library make it easy to wrap your clojure values in just such a container.

Armed with a utility such as this, there is no longer a need to use custom
implementations of KafkaAvroSerializer and Serde interfaces. Instead we just
use the standard implmentations and convert our EDN data into a GenericDatum
instance before sending it to a kafka producer or returning it in a kafka stream.

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

Copyright © 2020 Andy Chambers


Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
