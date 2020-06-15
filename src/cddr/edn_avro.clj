(ns cddr.edn-avro
  (:require
   [clojure.data.json :as json])
  (:import
   (java.io ByteArrayInputStream DataInputStream)
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
  "Converts EDN into avro using a JSONDecoder"
  [object {:keys [schema]}]
  (let [input (ByteArrayInputStream. (-> (json/write-str object)
                                         (.getBytes)))
        din (DataInputStream. input)]

    (let [decoder (.jsonDecoder (DecoderFactory/get) schema din)
          reader (GenericDatumReader. schema)
          datum (.read reader nil decoder)]

      (let [writer (GenericDatumWriter. schema)
            output (ByteArrayOutputStream.)
            encoder (.binaryEncoder (EncoderFactory/get) output nil)]

        (.write writer datum encoder)
        (.flush encoder)
        (.toByteArray output)))))

(defn as-edn
  "Converts avro into EDN using a JsonEncoder"
  [avro {:keys [schema]}]
  (let [input (ByteArrayInputStream. avro)
        output (ByteArrayOutputStream.)
        reader (GenericDatumReader. schema)
        writer (GenericDatumWriter. schema)]

    (let [bin-decoder (.binaryDecoder (DecoderFactory/get) input nil)
          encoder (.jsonEncoder (EncoderFactory/get) schema output)
          datum (Object.)
          datum (.read reader datum bin-decoder)]
      (.write writer datum encoder)
      (.flush encoder)
      (.flush output))

    (json/read-str (String. (.toByteArray output)))))
