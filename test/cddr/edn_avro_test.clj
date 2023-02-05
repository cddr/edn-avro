(ns cddr.edn-avro-test
  (:require
   [clojure.test :refer :all]
   [cddr.edn-avro :as ea])
  (:import
   (org.apache.avro.generic GenericData$Record)
   (io.confluent.kafka.serializers KafkaAvroSerializer KafkaAvroDeserializer)
   (io.confluent.kafka.schemaregistry.client MockSchemaRegistryClient)))

(deftest test-kafka-avro-serializer
  (testing "can serialize record"
    (let [schema (ea/avro-schema {:type "record"
                                  :name "Foo"
                                  :fields [{:name "yolo"
                                            :type ["string" "null"]}]})
          mock-reg (MockSchemaRegistryClient.)
          serializer (KafkaAvroSerializer. mock-reg {"schema.registry.url" "test.reg"})
          msg (ea/as-avro {:yolo "yolo"} {:schema schema})]

      (is (.serialize serializer "yolo" msg))))

  (testing "can serialize primitive"
    (let [schema (ea/avro-schema "string")
          mock-reg (MockSchemaRegistryClient.)
          serializer (KafkaAvroSerializer. mock-reg {"schema.registry.url" "test.reg"})
          msg (ea/as-avro "yolo" {:schema schema})]

      (is (.serialize serializer "yolo" msg)))))

(deftest test-kafka-avro-deserializer
  (testing "can deserialize record"
    (let [schema (ea/avro-schema {:type "record"
                                  :name "Foo"
                                  :fields [{:name "yolo"
                                            :type ["string" "null"]}]})
          mock-reg (MockSchemaRegistryClient.)
          serializer (KafkaAvroSerializer. mock-reg {"schema.registry.url" "test.reg"})
          deserializer (KafkaAvroDeserializer. mock-reg {"schema.registry.url" "test.reg"})
          bytes (->> (ea/as-avro {:yolo "yolo"} {:schema schema})
                     (.serialize serializer "yolo"))]

      (is (instance? GenericData$Record (.deserialize deserializer "yolo" bytes)))))

  (testing "can deserialize primitive"
    (let [schema (ea/avro-schema "string")
          mock-reg (MockSchemaRegistryClient.)
          serializer (KafkaAvroSerializer. mock-reg {"schema.registry.url" "test.reg"})
          deserializer (KafkaAvroDeserializer. mock-reg {"schema.registry.url" "test.reg"})
          bytes (->> (ea/as-avro "yolo" {:schema schema})
                     (.serialize serializer "yolo"))]

      (is (= "yolo"
             (.deserialize deserializer "yolo" bytes))))))

(deftest test-roundtrip
  (let [schema (ea/avro-schema {:type "record"
                                :name "Foo"
                                :fields [{:name "yolo"
                                          :type ["string" "null"]}]})
        mock-reg (MockSchemaRegistryClient.)
        serializer (KafkaAvroSerializer. mock-reg {"schema.registry.url" "test.reg"})
        deserializer (KafkaAvroDeserializer. mock-reg {"schema.registry.url" "test.reg"})]

    (let [test-msg {:yolo "yolo"}
          as-avro (ea/as-avro test-msg {:schema schema})
          as-edn (ea/as-edn as-avro {:schema schema})]
      (is (= test-msg as-edn)))))


(deftest test-various-types
  (let [schema (ea/avro-schema
                {:type "record"
                 :name "Foo"
                 :fields [
                          {:name "aNull"
                           :type "null"}

                          {:name "aBool"
                           :type "boolean"}

                          {:name "aInt" :type "int"}
                          {:name "aLong" :type "long"}
                          {:name "aFloat" :type "float"}
                          {:name "aDouble" :type "double"}
                          {:name "aBytes" :type "bytes"}
                          {:name "aString" :type "string"}

                          ]})]

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

        (is (= (:aNull as-edn) nil))
        (is (= (:aBool as-edn) true))
        (is (= (:aLong as-edn) 2147483647))
        (is (= (:aFloat as-edn) (float 3.234)))
        (is (= (:aDouble as-edn) 1.0000000000000002))
        (is (= (:aString as-edn) "yolo"))
        (is (= (map byte (:aBytes as-edn))
               (map byte (.getBytes "yolo"))))))))
