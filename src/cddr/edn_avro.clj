(ns cddr.edn-avro
  (:require
   [clojure.data.json :as json])
  (:import
   (java.io ByteArrayInputStream ByteArrayOutputStream DataInputStream)
   (org.apache.avro Schema$Parser)
   (org.apache.avro.generic GenericDatumWriter GenericDatumReader)
   (org.apache.avro.io
    EncoderFactory DecoderFactory JsonEncoder JsonDecoder)))

(defn avro-schema
  "Parses an avro schema (expressed as edn data)"
  [schema]
  (let [parser (Schema$Parser.)]
    (.parse parser (json/write-str schema))))

(defn as-avro
  "Converts EDN into a `GenericDatum` using a JSONDecoder"
  [object {:keys [schema]}]
  (let [input (ByteArrayInputStream. (-> (json/write-str object)
                                         (.getBytes)))
        din (DataInputStream. input)]

    (let [decoder (.jsonDecoder (DecoderFactory/get) schema din)
          reader (GenericDatumReader. schema)]
      (.read reader nil decoder))))

(defn as-edn
  "Converts a `GenericDatum` into EDN using a JsonEncoder"
  ([avro]
   (as-edn avro {}))
  ([avro {:keys [schema]}]
   (let [schema (or schema
                    (.getSchema avro))
         output (ByteArrayOutputStream.)
         writer (GenericDatumWriter. schema)
         encoder (.jsonEncoder (EncoderFactory/get) schema output)]

     (.write writer avro encoder)
     (.flush encoder)
     (.flush output)

     (json/read-str (String. (.toByteArray output))))))


(comment
  (let [s (avro-schema {:type "record"
                        :name "Foo"
                        :fields [{:name "yolo"
                                  :type ["string" "null"]}]})]
    (-> (as-avro {:yolo {:string "yolo"}} {:schema s})
        (as-edn)))
  )
