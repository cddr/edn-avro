(ns cddr.edn-avro
  (:require
   [jsonista.core :as json])
  (:import
   (java.io ByteArrayInputStream ByteArrayOutputStream)
   (org.apache.avro.util Utf8)
   (org.apache.avro.io EncoderFactory DecoderFactory)
   (org.apache.avro Schema Schema$Type Schema$Parser)
   (org.apache.avro.generic GenericDatumWriter GenericDatumReader IndexedRecord)))

(defn avro-schema
  "Parses an avro schema (expressed as edn data)"
  [schema]
  (let [parser (Schema$Parser.)]
    (.parse parser (json/write-value-as-string schema))))

(defn as-avro
  "Converts an EDN value to an Avro-compatible object."
  [edn-value {:keys [schema]}]
  (let [avro-schema (if (instance? Schema schema)
                      schema
                      (avro-schema schema))
        type (.getType avro-schema)]

    (cond
      ;; 1. Handle Records (The "Pour" logic)
      (= type Schema$Type/RECORD)
      (let [record (org.apache.avro.generic.GenericData$Record. avro-schema)]
        (doseq [field (.getFields avro-schema)]
          (let [field-name (.name field)
                field-key (keyword field-name)
                val (get edn-value field-key)]
            (when-not (nil? val)
              (.put record (.pos field) (as-avro val {:schema (.schema field)})))))
        record)

      ;; 2. Handle Strings (Convert to Utf8 for Avro consistency)
      (= type Schema$Type/STRING)
      (org.apache.avro.util.Utf8. (str edn-value))

      ;; 3. Handle Enums
      (= type Schema$Type/ENUM)
      (org.apache.avro.generic.GenericData$EnumSymbol. avro-schema (name edn-value))

      ;; 4. Handle Unions (e.g., ["string", "null"])
      (= type Schema$Type/UNION)
      (if (nil? edn-value)
        nil
        ;; Find the first non-null branch of the union that matches the value
        ;; This is a simplified version; for complex unions, you might need more logic
        (let [branches (.getTypes avro-schema)
              non-null-branch (first (remove #(= (.getType %) Schema$Type/NULL) branches))]
          (as-avro edn-value {:schema non-null-branch})))

      ;; 5. Passthrough for Primitives (int, long, double, boolean, nil)
      :else edn-value)))

(defn- as-edn-scoop
  "Recursive transformation of Avro-Java types to Clojure EDN."
  [datum]
  (cond
    (instance? org.apache.avro.generic.IndexedRecord datum)
    (let [schema (.getSchema datum)]
      (reduce (fn [m field]
                (let [k (keyword (.name field))
                      v (.get datum (.pos field))]
                  (assoc m k (as-edn-scoop v))))
              {}
              (.getFields schema)))

    (instance? org.apache.avro.generic.GenericEnumSymbol datum)
    (keyword (.toString datum))

    (instance? java.util.Collection datum)
    (mapv as-edn-scoop datum)

    (instance? java.util.Map datum)
    (reduce-kv (fn [m k v] (assoc m (keyword (str k)) (as-edn-scoop v))) {} datum)

    (instance? org.apache.avro.util.Utf8 datum)
    (.toString datum)

    :else datum))

(defn as-edn
  "Version-agnostic recursive conversion with optional Reader Schema resolution."
  ([datum]
   (as-edn-scoop datum))
  ([datum opts]
   (let [write-schema (if (instance? IndexedRecord datum) (.getSchema datum) nil)
         ;; FIX 1: Look for both :schema and :read-schema
         r-spec (or (:read-schema opts) (:schema opts))
         read-schema (cond
                       (instance? Schema r-spec) r-spec
                       r-spec (avro-schema r-spec)
                       :else write-schema)]
     (cond
       ;; If not a record (string, int, etc), just scoop
       (not (instance? IndexedRecord datum))
       (as-edn-scoop datum)

       ;; If no specialized reader schema, scoop the writer schema
       (or (nil? read-schema) (= write-schema read-schema))
       (as-edn-scoop datum)

       ;; Resolution Path
       :else
       (let [out (ByteArrayOutputStream.)
             gen (EncoderFactory/get)
             writer (GenericDatumWriter. write-schema)]
         (.write writer datum (.directBinaryEncoder gen out nil))
         (let [in (ByteArrayInputStream. (.toByteArray out))
               decode (DecoderFactory/get)
               reader (GenericDatumReader. write-schema read-schema)
               ;; This record now contains the data for 'yolo' and 'abc'
               resolved (.read reader nil (.directBinaryDecoder decode in nil))]

           ;; FIX 2: We MUST iterate over the fields in 'read-schema'
           ;; to ensure we pick up the 'abc' field that the reader injected.
           (reduce (fn [m field]
                     (let [k (keyword (.name field))
                           v (.get ^IndexedRecord resolved (.pos field))]
                       (assoc m k (as-edn-scoop v))))
                   {}
                   (.getFields read-schema))))))))

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
        (as-edn {:read-schema s2}))
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
  )
