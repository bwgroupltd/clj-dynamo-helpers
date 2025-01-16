(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'audiogum/clj-dynamo-helpers)
(def version "0.0.4")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
      (b/delete {:path "target"}))

(defn uber [_]
      (clean nil)
      (b/write-pom {:class-dir class-dir
                    :lib lib
                    :version version
                    :basis basis
                    :src-dirs ["src"]
                    :scm {:url "https://github.com/bwgroupltd/clj-dynamo-helpers"}
                    :pom-data  [[:licenses
                                 [:license
                                  [:name "Eclipse Public License 1.0"]
                                  [:url "https://opensource.org/license/epl-1-0/"]]]]})
      (b/copy-dir {:src-dirs ["src" "resources"]
                   :target-dir class-dir})
      (b/compile-clj {:basis basis
                      :src-dirs ["src"]
                      :class-dir class-dir})
      (b/uber {:class-dir class-dir
               :uber-file jar-file
               :basis basis
               :main 'clj-dynamo-helpers.core}))

(defn install [_]
      (b/install {:basis      basis
                  :lib        lib
                  :version    version
                  :jar-file   jar-file
                  :class-dir  class-dir}))

(defn deploy [_]
      (dd/deploy {:repository {"releases" {:url "s3p://repo.bowerswilkins.net/releases/"}}
                  :installer :remote
                  :artifact jar-file
                  :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))
