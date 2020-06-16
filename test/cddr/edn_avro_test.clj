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
          msg (ea/as-avro {:yolo {:string "yolo"}} schema)]

      (is (.serialize serializer "yolo" msg))))

  (testing "can serialize primitive"
    (let [schema (ea/avro-schema "string")
          mock-reg (MockSchemaRegistryClient.)
          serializer (KafkaAvroSerializer. mock-reg {"schema.registry.url" "test.reg"})
          msg (ea/as-avro "yolo" schema)]

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
          bytes (->> (ea/as-avro {:yolo {:string "yolo"}} schema)
                     (.serialize serializer "yolo"))]

      (is (instance? GenericData$Record (.deserialize deserializer "yolo" bytes)))))

  (testing "can deserealize primitive"
    (let [schema (ea/avro-schema "string")
          mock-reg (MockSchemaRegistryClient.)
          serializer (KafkaAvroSerializer. mock-reg {"schema.registry.url" "test.reg"})
          deserializer (KafkaAvroDeserializer. mock-reg {"schema.registry.url" "test.reg"})
          bytes (->> (ea/as-avro "yolo" schema)
                     (.serialize serializer "yolo"))]

      (is (= "yolo"
             (.deserialize deserializer "yolo" bytes))))))
