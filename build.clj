(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def lib 'cddr/edn-avro)
(def version (b/git-process {:git-args "describe"}))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs ["src"]})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn deploy [_]
  (dd/deploy {:installer :remote
              :sign-releases? true
              :sign-key-id "6BC21F24EC17A8A5BD712E2015C5A307C7C2315D"
              :pom-file (b/pom-path {:lib lib
                                     :class-dir "target/classes"})
              :artifact "edn-avro.jar"}))
