# edn-avro

[![CircleCI](https://circleci.com/gh/cddr/edn-avro.svg?style=svg)](https://circleci.com/gh/cddr/edn-avro) |
[![Current Release](https://img.shields.io/clojars/v/cddr/edn-avro.svg)](https://clojars.org/cddr/edn-avro)

Lossless conversion between EDN and Avro (and back)

## Rationale

In [jackdaw](https://github.com/fundingcircle/jackdaw), we made the
mistake of getting into the business of maintaining a custom mapping
between EDN and Avro. This meant we ended up duplicating a lot of code
in order to marshall data from one format into another

https://github.com/FundingCircle/jackdaw/blob/master/src/jackdaw/serdes/avro.clj

This means there have been (and likely remain) subtle bugs in the implementation
which do not exist in the upstream implementation of avro. For example...

  * https://github.com/FundingCircle/jackdaw/commit/26ec1e2fd14716ab923b17c43422dca5e6383484#diff-6ea591afdb2bef0213a7d11717398160
  * https://github.com/FundingCircle/jackdaw/commit/eede5cc29b475bc2d24e4f1e9a8264a9c607438a#diff-6ea591afdb2bef0213a7d11717398160
  * https://github.com/FundingCircle/jackdaw/commit/5b0661ec5f8a552c27de0e388db09abedea6f52a#diff-6ea591afdb2bef0213a7d11717398160

This library aims to resolve these problems by using the ClojureDatumReader
and ClojureDatumWriter defined in the abracad.avro project to define two
methods `as-avro` and `as-edn` to translate between edn and avro.

There might still be bugs in that library (and this one) but it leverages
the Avro API a bit better so that there's less logic to implement.

Armed with a utility such as this, there is no longer a need to use custom
implementations of KafkaAvroSerializer and Serde interfaces. Instead we just
use the standard implmentations and convert our EDN data into a GenericDatum
instance before returning sending it to a kafka producer or returning it in
a kafka stream.

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
