(ns cddr.edn-avro
  (:require
   [abracad.avro :as avro]
   [abracad.avro.util :refer [coerce]]
   [jsonista.core :as json])
  (:import
   (abracad.avro ClojureDatumWriter ClojureDatumReader)
   (java.nio ByteBuffer)
   (java.io ByteArrayInputStream ByteArrayOutputStream DataInputStream)
   (org.apache.avro Schema Schema$Parser Schema$Type)
   (org.apache.avro.generic GenericData GenericData$Record
                            GenericRecordBuilder GenericDatumWriter GenericDatumReader)
   (org.apache.avro.io
    EncoderFactory DecoderFactory JsonEncoder JsonDecoder)))

(defn avro-schema
  "Parses an avro schema (expressed as edn data)"
  [schema]
  (let [parser (Schema$Parser.)]
    (.parse parser (json/write-value-as-string schema))))

(defn as-avro
  [object {:keys [schema]}]
  (let [schema-w (if (instance? Schema schema)
                   schema
                   (avro-schema schema))
        cdr (ClojureDatumWriter. schema-w)
        out (ByteArrayOutputStream.)
        gdr (GenericDatumReader. schema-w)]
    (.write cdr schema-w object (.directBinaryEncoder (EncoderFactory.) out nil))
    (.read gdr nil (.binaryDecoder (DecoderFactory.) (.toByteArray out) nil))))

(defn as-edn
  [generic-datum {:keys [schema]}]
  (let [schema-w (.getSchema generic-datum)
        schema-r (if (instance? Schema schema)
                   schema
                   (when schema
                     (avro-schema schema)))
        cdr (ClojureDatumReader. (or schema-r schema-w))
        gdw (GenericDatumWriter. schema-w)
        out (ByteArrayOutputStream.)
        df (DecoderFactory.)]

    (.write gdw generic-datum (.directBinaryEncoder (EncoderFactory.) out nil))
    (let [gr-bytes (.toByteArray out)
          decoder (cond->> (.directBinaryDecoder df (ByteArrayInputStream.
                                                     (.toByteArray out))
                                                 nil)
                    schema (.resolvingDecoder df schema-w schema-r))]
      (.read cdr nil decoder))))

(comment
  ;; roundtrip a record (optionally using a custom reader schema)
  (let [s1 {:type "record"
            :name "Foo"
            :fields [{:name "yolo"
                      :type ["string" "null"]}]}
        s2 {:type "record"
            :name "Foo"
            :fields [{:name "yolo"
                      :type ["string" "null"]}
                     {:name "abc"
                      :type ["null" "long"]
                      :default nil}]}

        avit (as-avro {:yolo "yolo"} {:schema s1})]

    (-> avit
        (as-edn {:schema s2})))
    ;; {"yolo" {"string" "yolo"}, "abc" nil}

    (-> avit
        (as-edn {}))
    ;; {"yolo" {"string" "yolo"}}

  (let [s1 (avro-schema "string")
        avit (as-avro "yolo" {:schema s1})]
    avit)
    ;; (-> avit
  ;;     (as-
  (prn :fini)

  )
