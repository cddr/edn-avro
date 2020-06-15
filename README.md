# edn-avro

In jackdaw, we made the mistake of getting into the business of
maintaining a custom mapping between EDN and Avro. The consequence
of this leads in turn to a requirement to maintain custom
KafkaAvroSerializers and KafkaAvroDeserializers (when using
Kafka Consumers and Producers respectively), and also custom Serdes
(when using the Streams API).

All this could have been avoided if we just had a simple way to go
from EDN -> GenericData and back. Then we could just use the
standard implementations of these utilities.

## Usage

```
(ns my.cool.app
  (:require
	  [cddr.edn-avro :as avro]))

(def schema "string")
(def msg "yolo")

(def msg-as-avro (avro/as-avro msg {:schema schema}))

(def msg-as-edn (avro/as-edn msg-as-avro {:schema schema}))

msg-as-edn
=> "yolo"

```

## License

Copyright © 2020 Andy Chambers

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
