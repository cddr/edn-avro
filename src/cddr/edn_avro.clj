(ns cddr.edn-avro
  (:require
   [jsonista.core :as json])
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
    (.parse parser (json/write-value-as-string schema))))

(defn as-avro
  "Converts EDN into a `GenericDatum` using a JSONDecoder"
  [object {:keys [schema]}]
  (let [input (ByteArrayInputStream. (-> (json/write-value-as-string object)
                                         (.getBytes)))
        din (DataInputStream. input)]

    (let [decoder (.jsonDecoder (DecoderFactory/get) schema din)
          reader (GenericDatumReader. schema)]
      (.read reader nil decoder))))

(defn as-edn
  "Converts a `GenericDatum` into EDN using a JsonEncoder"
  ([avro]
   (as-edn avro nil))
  ([avro {:keys [schema mapper]
          :or {mapper json/keyword-keys-object-mapper}}]
   (let [write-schema (.getSchema avro)
         read-schema schema]

     ;; either way, we first need to write out the avro object
     ;; to a ByteArray using the json encoder
     (let [output (ByteArrayOutputStream.)
           writer (GenericDatumWriter. write-schema)
           encoder (.jsonEncoder (EncoderFactory/get) write-schema output)]

       (.write writer avro encoder)
       (.flush encoder)
       (.flush output)

       (if read-schema
         ;; if we're using a custom reader schema, we need to
         ;; read it back in again using the GenericDatumReader
         ;; configured with both the write and read schemas
         (let [json-in (-> output
                           .toByteArray
                           ByteArrayInputStream.
                           DataInputStream.)
               decoder (.jsonDecoder (DecoderFactory/get) read-schema json-in)
               reader (GenericDatumReader. write-schema read-schema)]
           (as-edn (.read reader nil decoder)))
         ;; if we're not using a custom read-schema, we can just
         ;; parse the encoded json
         (json/read-value (String. (.toByteArray output))
                          mapper))))))

(comment
  ;; roundtrip a record (optionally using a custom reader schema)
  (let [s1 (avro-schema {:type "record"
                         :name "Foo"
                         :fields [{:name "yolo"
                                   :type ["string" "null"]}]})
        s2 (avro-schema {:type "record"
                         :name "Foo"
                         :fields [{:name "yolo"
                                   :type ["string" "null"]}
                                  {:name "abc"
                                   :type ["null" "long"]
                                   :default nil}]})

        avit (as-avro {:yolo {:string "yolo"}} s1)]

    (-> avit
        (as-edn s2))
    ;; {"yolo" {"string" "yolo"}, "abc" nil}

    (-> avit
        (as-edn))
    ;; {"yolo" {"string" "yolo"}}
    )


  (let [s1 (avro-schema "string")
        avit (as-avro "yolo" s1)]
    (-> avit
        (as-edn)))
  )
