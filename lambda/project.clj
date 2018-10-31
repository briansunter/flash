(defproject lambda "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure       "1.9.0"]
                 [org.clojure/clojurescript "1.10.312"]
                 [io.nervous/cljs-lambda    "0.3.5"]]
  :plugins [[lein-npm                    "0.6.2"]
            [io.nervous/lein-cljs-lambda "0.6.6"]]
  :npm {:dependencies [[serverless-cljs-plugin "0.1.2"]]}
  :cljs-lambda {:compiler
                {:inputs  ["src"]
                 :options {:output-to     "target/lambda/lambda.js"
                           :output-dir    "target/lambda"
                           :target        :nodejs
                           :language-in   :ecmascript5
                           :optimizations :simple}}})
