(ns build
  (:require
   [clojure.string :as str]
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as dd]))

(def version
  (let [;; Get the closest tag (lightweight or annotated)
        tag (b/git-process {:git-args "describe --tags --abbrev=0"})
        ;; Get the full description to check for "dirtiness"
        full-desc (b/git-process {:git-args "describe"})
        is-snapshot? (not= tag full-desc)]
    (str tag (when is-snapshot? "-SNAPSHOT"))))

(def lib 'cddr/edn-avro)
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn show-version [_]
  (println "🏷️  Current Git Tag:  " (str/replace version "-SNAPSHOT" ""))
  (println "🛠️  Build Type:      " (if (str/includes? version "SNAPSHOT")
                                     "Construction (SNAPSHOT)"
                                     "Official Release"))
  (println "📦 Final Version:   " version))

(defn jar [_]
  (println "\n─── Building Artifact ──────────────────────────")
  (println "ID:      " lib)
  (println "Version: " version)
  (println "Target:  " jar-file)

  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :pom-data [[:licenses
                            [:license
                             [:name "Eclipse Public License 1.0"]
                             [:url "https://opensource.org/license/epl-1-0/";]
                              [:distribution "repo"]]]]]
                :src-dirs ["src"]})
  (println "✓ Generated pom.xml")
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (println "✓ Source files copied")
  (b/jar {:class-dir class-dir
          :jar-file jar-file})
  (println "📦 JAR created successfully.")
  (println "────────────────────────────────────────────────\n"))

(defn deploy [_]
  (if (str/includes? version "SNAPSHOT")
    (do
      (println "🛑 Stop! You're trying to deploy a SNAPSHOT. Create a tag first")
      (println "🎗️ Tip: Use VERSION=x.y.z git tag -a $VERSION -m \"Version x.y.z\""))
    (do
      (println "🚀 Starting deployment to Clojars...")
      (println "📦 Artifact:" jar-file)
      (println "🏷️ Version: " version)
      (dd/deploy {:installer :remote
                  :sign-releases? true
                  :sign-key-id "DBE58F103C03B26E848ED2272C0EE5D7989BE7CF"
                  :pom-file (b/pom-path {:lib lib
                                         :class-dir "target/classes"})
                  :artifact jar-file})
      (println "✅ Deployment complete!"))))
