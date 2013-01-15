(defproject submitit "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :repositories [["central-proxy" "http://repository.sonatype.org/content/repositories/central/"]]
  :dependencies [[org.clojure/clojure "1.3.0"] [org.apache.commons/commons-email "1.2"]]
  :main ^{:skip-aot true} submitit.core
  )