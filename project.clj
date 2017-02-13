(defproject md-to-hiccup "0.1.0-SNAPSHOT"
  :description "markdown to hiccup conveter"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha14"]
                 [hickory "0.7.0"]
                 [hiccup "1.0.5"]]

  :profiles
  {:dev {:resource-paths ["resources" "markdown-testsuite"]}})
