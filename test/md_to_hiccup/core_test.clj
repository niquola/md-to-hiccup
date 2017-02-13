(ns md-to-hiccup.core-test
  (:require [clojure.test :refer :all]
            [md-to-hiccup.core :refer :all]
            [clojure.java.io :as io]
            [hiccup.core :as hiccup]
            [hickory.core :as hick]
            [md-to-hiccup.core :as sut]
            [clojure.string :as str]
            [clojure.walk :as walk]))



(defn clear [form]
  (walk/postwalk (fn [xs]
                  (if (vector? xs)
                    (filterv (fn [x] (not
                                      (or
                                       (= "\r\n\r\n" x)
                                       (= "\r\r" x)
                                       (= "" x) (= "\n" x) (= "\n\n" x)))) xs)
                    xs))
                form))


(deftest test-parse
  (doseq [f (file-seq (io/file (io/resource "tests")))]
    (when (and (str/ends-with? (.getPath f) ".md")
               (not
                (or
                 (re-matches #".*idref.*" (.getPath f))
                 (re-matches #".*extensions/.*" (.getPath f)))))
      (let [out-path (str/replace (.getPath f) #"\.md" ".out")
            in (slurp f)
            out (when (.exists (io/file out-path)) (slurp out-path))]
        (when out
          (let [parsed (clear (sut/parse in))
                expected (clear (into [:div.md] (map hick/as-hiccup (hick/parse-fragment out))))
                ]

            (println "================")
            (println "==" (.getName f) "==")
            (println "================")
            (println in)
            (println "---EXPECTED-----")
            (println (hiccup/html expected))
            (println "---RESULT-----")
            (println (hiccup/html parsed))
            (is (= parsed expected) (str in "\n" out-path "\n"))

            )
          
          ))
      )
    ))
