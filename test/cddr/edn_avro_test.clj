(ns cddr.edn-avro-test
  (:require
   [clojure.test :refer :all]
   [cddr.edn-avro :as ea])
  (:import
   (org.apache.avro.generic GenericData$Record GenericDatumWriter GenericDatumReader)
   (org.apache.avro.io EncoderFactory DecoderFactory)
   (io.confluent.kafka.schemaregistry.client MockSchemaRegistryClient)
   (java.io ByteArrayOutputStream)))

;; --- Lean Wire Format Helpers ---

(defn serialize-confluent
  "Direct implementation of Confluent Wire Format: Magic Byte (0) + 4-byte ID + Avro Binary"
  [registry topic schema msg]
  (let [id (.register registry (str topic "-value") schema)
        out (ByteArrayOutputStream.)]
    (.write out 0) ;; Magic Byte
    (.write out (.. (java.nio.ByteBuffer/allocate 4) (putInt id) array))
    (let [writer (GenericDatumWriter. schema)
          factory (EncoderFactory/get)
          encoder (.directBinaryEncoder factory out nil)]
      (.write writer msg encoder)
      (.toByteArray out))))

(defn deserialize-confluent
  "Reads Confluent Wire Format bytes and returns the Avro object"
  [registry topic bytes]
  (let [buf (java.nio.ByteBuffer/wrap bytes)
        magic-byte (.get buf)
        schema-id (.getInt buf)
        schema (.getById registry schema-id)
        ;; The rest of the buffer is the actual Avro data
        data-offset 5
        data-len (- (alength bytes) data-offset)
        factory (DecoderFactory/get)
        decoder (.binaryDecoder factory bytes data-offset data-len nil)
        reader (GenericDatumReader. schema)]
    (.read reader nil decoder)))

;; --- Tests ---

(deftest test-wire-format-compatibility
  (let [mock-reg (MockSchemaRegistryClient.)
        topic "yolo-topic"]

    (testing "can serialize and deserialize record using wire format"
      (let [schema (ea/avro-schema {:type "record"
                                    :name "Foo"
                                    :fields [{:name "yolo" :type ["string" "null"]}]})
            edn-msg {:yolo "yolo"}
            avro-msg (ea/as-avro edn-msg {:schema schema})
            bytes (serialize-confluent mock-reg topic schema avro-msg)
            result (deserialize-confluent mock-reg topic bytes)]

        (is (instance? GenericData$Record result))
        (is (= edn-msg (ea/as-edn result {:schema schema})))))

    (testing "can handle primitives"
      (let [schema (ea/avro-schema "string")
            avro-msg (ea/as-avro "yolo" {:schema schema})
            bytes (serialize-confluent mock-reg topic schema avro-msg)
            result (deserialize-confluent mock-reg topic bytes)]
        (is (= "yolo" (str result)))))))

(deftest test-various-types
  ;; Keep this as is—it tests your core logic (as-avro / as-edn)
  ;; which is independent of Kafka/Confluent anyway.
  (let [schema (ea/avro-schema
                {:type "record"
                 :name "Foo"
                 :fields [{:name "aNull" :type "null"}
                          {:name "aBool" :type "boolean"}
                          {:name "aInt" :type "int"}
                          {:name "aLong" :type "long"}
                          {:name "aFloat" :type "float"}
                          {:name "aDouble" :type "double"}
                          {:name "aBytes" :type "bytes"}
                          {:name "aString" :type "string"}]})]

    (let [test-msg {:aNull nil
                    :aBool true
                    :aInt 42
                    :aLong 2147483647
                    :aFloat (float 3.234)
                    :aDouble 1.0000000000000002
                    :aBytes (.getBytes "yolo")
                    :aString "yolo"}
          as-avro (ea/as-avro test-msg {:schema schema})]

      (is (instance? GenericData$Record as-avro))

      (let [as-edn (ea/as-edn as-avro {:schema schema})]
        (is (nil? (:aNull as-edn)))
        (is (true? (:aBool as-edn)))
        (is (= 2147483647 (:aLong as-edn)))
        (is (= (float 3.234) (:aFloat as-edn)))
        (is (= 1.0000000000000002 (:aDouble as-edn)))
        (is (= "yolo" (:aString as-edn)))
        (is (= (seq (.getBytes "yolo")) (seq (:aBytes as-edn))))))))
