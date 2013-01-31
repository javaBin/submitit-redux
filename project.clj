(defproject submitit "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :repositories [["central-proxy" "http://repository.sonatype.org/content/repositories/central/"]]
  :dependencies [
  	[org.clojure/clojure "1.3.0"] 
  	[org.apache.commons/commons-email "1.2"]
    [commons-codec "1.7"]
  	[noir "1.3.0-alpha10"]
  	[ring-middleware-format "0.1.1"]
    [cheshire "4.0.3"]
    [clj-http "0.6.3"]
    [org.clojure/data.codec "0.1.0"]
    [net.sf.jlue/jlue-core "1.3"]
  	]
  :dev-dependencies [[lein-ring "0.4.3"]]
  :ring {:handler submitit.core/handler}  
  :main ^{:skip-aot true} submitit.core
  :plugins [[lein-ring "0.8.2"]]
  )