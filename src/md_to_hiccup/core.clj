(ns md-to-hiccup.core
  (:require [clojure.string :as str]))

(declare parse-inline)

(defn inline-transformers [s]
  (-> s
      (str/replace #"&" "&amp;")
      (str/replace #"&amp;copy;" "©")
      (str/replace #"<" "&lt;")
      (str/replace #">" "&gt;")))

(def escapable #{\\ \` \* \_ \{ \} \[ \] \( \) \# \+ \- \. \!})

(defn tag-inline-char [f s]
  (cond
    (and (= f \\) (contains? escapable s)) :escaped-symbol
    (and (= f \*) (not= s \*)) :emph
    (and (= f \_) (not= s \_)) :emph
    (and (= f \h) (= s \t)) :inline-link
    (and (= f \<) (= s \h)) :auto-link
    (and (= f \*) (= s \*)) :bold
    (and (= f \`) (= s \`))  :inline-dcode
    (and (= f \`) (not= s \`))  :inline-code
    (and (= f \ ) (= s \ ))  :line-break
    (and (= f \[)) :link
    (and (= f \!) (= s \[)) :img
    :else :norm))

(def link-regx #"^(\[([^\]]+)\]\(([^)]*)\)).*" )
(def img-regx #"^(!\[([^\]]+)\]\(([^)]*)\)).*" )
(def bold-regx #"^(\*\*([^*]+)\*\*).*" )
(def emph-regx #"^((\*|_)([^*]+)(\*|_)).*" )
(def inline-code-regx #"^(`([^`]+)`).*" )
(def inline-dcode-regx #"^(``([^`]+)``).*" )
(def inline-link-regx #"^(https?://[^ ]+).*")
(def auto-link-regx #"^(<(https?://[^ ]+)>).*")
(def line-break-regx #"^(\s{5}|\s{2})$")


(def inline-rules
  [
   {:name :escapes
    :regex #"^(\\\\|\\`|\\\*|\\_|\\\{|\\}|\\\[|\\]|\\\(|\)|\\#|\\\+|\\-|\\\.|\\!)"
    :f (fn [[_ txt]]
         (println "HERE" (pr-str txt))
         (subs txt 1))}

   {:name :empth
    :regex #"^((\*|_)([^*]+)(\*|_)).*"
    :f (fn [[_ _ _ txt]] [:em txt])}

   {:name :inline-link
    :regex #"^(https?://[^ ]+).*"
    :f (fn [[_ txt]]
         (into [:a {:href (str/replace txt "&amp;" "&")} txt]))}


   :auto-link
   :auto-link {:regex auto-link-regx
               :build build-auto-link
               :inc 2}
   :bold
   :bold {:regex bold-regx
          :build build-bold
          :inc 1}

   ;; :inline-dcode

   :inline-code
   :inline-code {:regex inline-code-regx
                 :build build-inline-code
                 :inc 1}

   :line-break
   :line-break {:regex line-break-regx
                :build (fn [& _] [:br {}])
                :inc 0}

   :link
   :link {:regex link-regx
          :build build-link
          :inc 1}
   :img

   :img {:regex img-regx
         :build build-image
         :inc 1}

   ])


(defn apply-rules [txt]
  (loop [[{:keys [regex f] :as rule} & rules] inline-rules]
    (when rule
      (println "RULE:" rule)
      (if-let [re-gr (re-find regex txt)]
        (do
          (println "MATCH:" re-gr 
                   [(count (second re-gr)) (f re-gr)])
          [(count (second re-gr)) (f re-gr)])
        (recur rules)))))

(apply-rules "*aaa* bb ddd")

(defn *parse-inline [txt]
  (loop [acc [] char-idx 0 from-char-idx 0]
    (let [txt-tail (subs txt char-idx)
          [move-to new-elem] (apply-rules txt-tail)]
      (cond
        (= "" txt-tail) (if (> char-idx from-char-idx)
                          (conj acc (subs txt from-char-idx char-idx))
                          acc)

        new-elem (let [next-char-idx (+ char-idx move-to)]
                   (recur 
                    (if (> char-idx from-char-idx)
                      (conj acc (subs txt from-char-idx char-idx) new-elem)
                      (conj acc new-elem))
                    next-char-idx next-char-idx))
        :else (recur acc (inc char-idx) from-char-idx)))))

(comment

  (*parse-inline "Hello *amigo* how are you")

  (re-find (:regex (first inline-rules)) "\\*")
  (re-find (:regex (first inline-rules)) "\\\\")
  (re-find (:regex (first inline-rules)) "\\.")
  (re-find (:regex (second inline-rules)) "* some text * and some more text")
  )

(def link-regx #"^(\[([^\]]+)\]\(([^)]*)\)).*" )
(def img-regx #"^(!\[([^\]]+)\]\(([^)]*)\)).*" )
(def bold-regx #"^(\*\*([^*]+)\*\*).*" )
(def emph-regx #"^((\*|_)([^*]+)(\*|_)).*" )
(def inline-code-regx #"^(`([^`]+)`).*" )
(def inline-dcode-regx #"^(``([^`]+)``).*" )
(def inline-link-regx #"^(https?://[^ ]+).*")
(def auto-link-regx #"^(<(https?://[^ ]+)>).*")
(def line-break-regx #"^(\s{5}|\s{2})$")

(def escaped-symbol-regx #"^(\\(.)).*" )


(defn build-link [[_ _ txt ref]]
  (into [:a {:href (-> ref (str/replace "&amp;" "&"))}]
        (parse-inline txt)))

(defn build-inline-link [[_ txt]]
  (into [:a {:href (str/replace txt "&amp;" "&")} txt]))

(defn build-auto-link [[_ _ txt]]
  (into [:a {:href (str/replace txt "&amp;" "&")} txt]))

(defn build-image [[_ _ txt ref]]
  (let [[ref title & _] (str/split ref #"\s\"")
        attrs (if title {:src ref :alt txt :title (str/replace title #"\"$\s*" "")}
                 {:src ref :alt txt})]
    [:img attrs]))

(defn build-bold [[_ _ txt]]
  [:strong {} txt])

(defn build-inline-code [[_ _ txt]]
  [:code {} (inline-transformers txt)])

(defn build-emph [[a b c txt]]
  [:em {} txt])

(defn process-inline [txt i {regx :regex build-fn :build incr :inc}]
  (if-let [re-gr (re-seq regx (subs txt i))]
    (let [e-txt (second (first re-gr))
          next-i (+ i (dec (count e-txt)) incr)]
      [next-i (build-fn (first re-gr))])
    [(inc i) nil]))

(def inline-starts
  {
   :bold {:regex bold-regx
          :build build-bold
          :inc 1}

   :emph {:regex emph-regx
          :build build-emph
          :inc 1}

   :escaped-symbol {:regex escaped-symbol-regx
                    :build (fn [[_ _ a]]  a)
                    :inc 1}

   :link {:regex link-regx
          :build build-link
          :inc 1}

   :line-break {:regex line-break-regx
                :build (fn [& _] [:br {}])
                :inc 0}

   :inline-dcode {:regex inline-dcode-regx
                 :build build-inline-code
                 :inc 2}

   :inline-code {:regex inline-code-regx
                 :build build-inline-code
                 :inc 1}

   :inline-link {:regex inline-link-regx
                 :build build-inline-link
                 :inc 0}

   :auto-link {:regex auto-link-regx
               :build build-auto-link
               :inc 2}

   :amp {:regex #"^(&)"
         :build (fn [& args]
                  "&amp;")
          :inc 2}

   :img {:regex img-regx
         :build build-image
         :inc 1}})

(defn parse-inline [txt]
  (loop [acc []
         i 0 from-i 0]
    (let [char (nth txt i nil)
          next-char (nth txt (inc i) nil)
          tag (tag-inline-char char next-char)
          push-state (fn [acc] 
                       (if (and (not (= from-i (dec i))))
                         (conj acc (inline-transformers (subs txt from-i (min i (.length txt)))))
                         acc))]
      (cond
        (nil? char) (push-state acc)

        (get inline-starts tag)
        (let [opts (get inline-starts tag)
              [next-i new-elem] (process-inline txt i opts)]
          (if new-elem
            (recur (conj (push-state acc) new-elem) next-i next-i)
            (recur acc next-i from-i)))

        :else (recur acc (inc i) from-i)))))


;; (parse-inline "text **bold** [my super **link** !!](the-url) ![image](http://theurl)")
;; (parse-inline "text **bold** *emph* [my super *emph-link* !!](the-url) ![image](http://theurl)")
;; (parse-inline "text _emph_ [my super _link_ !!](the-url) ![image](http://theurl)")

(declare *parse)

(def ^:private olist-pattern #"^\d\.\s+(.*)")

(defn- string-tag [ln]
  (cond
    (nil? ln) :end-of-file
    (re-matches #"^\s*$" ln) :empty-line
    (re-matches #"^\=(=)+$" ln) :old-header-1
    (= ln "- - -") :hr
    (= ln "---") :hr
    (= ln "-------") :hr
    (re-matches #"^\--(-)+$" ln) :old-header-2
    (str/starts-with? ln "#") :header
    (str/starts-with? ln "* ") :ulist
    (str/starts-with? ln "```") :code
    (str/starts-with? ln ">") :blockquote
    (str/starts-with? ln "__") :hr
    (str/starts-with? ln "***") :hr
    (re-matches olist-pattern ln) :olist
    (re-matches #"^\t.*" ln) :pre
    (re-matches #"^\s\s+.*" ln) :pre
    (re-matches #"^ [^ ]+" ln) :text
    :else :text))

(defn- parse-header [txt]
  (let [header-txt (str/replace txt #"^\s*#{1,6}\s+" "")
        header-txt (str/replace header-txt #"\s+(#+)?$" "")
        header-marker (-> #"^(#{1,6})[^#]+.*"
                          (re-seq txt)
                          first
                          second)
        header-tag (->> header-marker
                        count
                        (str "h")
                        keyword)]
    (into [header-tag {}] (parse-inline header-txt))))

(defn- parse-old-header [tag lis]
  [tag {} (reduce (fn [acc l] (str acc l)) "" lis)])

(defn concat-strings [[i & is]]
  (loop [start true
         [x & xs] is
         prev i
         acc []]
    (cond
      (and (nil? x) (nil? prev)) acc 
      (and (nil? x) prev) (conj acc (if (and start (string? prev)) (str/triml prev) prev)) 
      (and prev (string? prev) (string? x))
      (recur false xs (str (str/triml prev) x) acc)
      :else
      (recur false xs x (conj acc prev)))))

(concat-strings ["  a" "b" [:b "ups"] "c"])

(defn- parse-paragraph [lis]
  (let [inline-parsed (mapcat parse-inline 
                              (if (< 1 (count lis))
                                (conj (mapv #(str % "\n") (butlast (filterv identity lis))) (last (filterv identity lis)))
                                lis))
        res (concat-strings inline-parsed)]
    (println "parse paragraph" (pr-str res))
    (into [:p {}] res)))

(defn- parse-list [tag lis]
  (let [[acc tmp-acc] (reduce (fn [[acc tmp-acc] ln]
                                (if (str/starts-with? ln "*")
                                  (let [ln (str/replace ln #"^\* " "")]
                                    (if (empty? tmp-acc)
                                      [acc [ln]]
                                      [(conj acc tmp-acc) [ln]]))

                                  [acc (conj tmp-acc (subs ln 2))])
                                ) [[] []] lis)
        acc (if (not (empty? tmp-acc)) (conj acc tmp-acc) acc)]
    (into [tag] (mapv (fn [lns] (conj (into [:li]
                                            (let [res (*parse lns)]
                                              (if (and (= 1 (count res)) (= :p (first res)))
                                                  (rest res)
                                                  res))
                                            ) "\n")) acc))))

(defn- parse-olist [tag lis]
  (let [[acc tmp-acc] (reduce (fn [[acc tmp-acc] ln]
                                (if (re-matches olist-pattern ln)
                                  (let [ln (second (first (re-seq olist-pattern ln)))]
                                    (if (empty? tmp-acc)
                                      [acc [ln]]
                                      [(conj acc tmp-acc) [ln]]))

                                  [acc (conj tmp-acc (subs ln 2))])
                                ) [[] []] lis)
        acc (if (not (empty? tmp-acc)) (conj acc tmp-acc) acc)]
    (into [tag] (mapv (fn [lns]
                        (let [res (*parse lns)]
                          (if (and (= 1 (count res)) (= (ffirst res) :p))
                            (into [:li] (rest (first res)))
                            (into [:li] res))))
                      acc))))


(defn- parse-code [lis]
  (into [:code.block {}] (str/join "\n" lis)))


(defn- parse-pre [lis]
  (let [[_ rep] (re-matches #"^(\s*).*" (first lis))]
    [:pre {} (into [:code {}]
                   (concat-strings
                    (mapv (fn [x]
                            (-> x
                                (str/replace (re-pattern (str "^" rep)) "")
                                inline-transformers
                                (str "\n")))
                          lis)))]))

(defn- parse-blockquote [lis]
  (let [[_ rep] (re-matches #"^(>\s*).*" (first lis))
        sp-lengh (- (count rep) 1)]
    (-> [:blockquote {} ]
        (into (*parse (mapv #(str/replace % (re-pattern (str "^> {0," sp-lengh "}"))  "") lis))))))



(defn- *parse [lns]
  (println "*parse" (pr-str lns))
  (loop [state :default
         [ln & lns :as prev-lns] lns
         acc []
         block-acc []]

    (let [transition (string-tag ln)]
      (println "..." [state  transition] ln "; block acc: " block-acc "; acc " acc)
      (let [with-paragraph (fn [acc]
                             (if (not (empty? block-acc))
                               (conj acc (parse-paragraph block-acc))
                               acc))]
        (cond

          ;; header
          (= [state transition] [:default :header])
          (recur :default lns (conj acc (parse-header ln)) [])

          (= [state transition] [:default :hr])
          (recur :default lns (conj acc [:hr {}]) [])


          ;; blockquote
          (and (not (or (= state :blockquote-em) (= state :blockquote))) (= transition :blockquote))
          (recur :blockquote lns (with-paragraph acc) [ln])

          (= state :blockquote)
          (cond
            (= transition :blockquote)   (recur :blockquote lns acc (conj block-acc ln))
            (= transition :empty-line)   (recur :blockquote-em lns acc (conj block-acc ln))
            (= transition :text)         (recur :blockquote lns acc (conj block-acc ln))
            (= transition :end-of-file)  (conj acc (parse-blockquote block-acc))
            :else (recur :default prev-lns (conj acc (parse-blockquote block-acc)) []))

          (= state :blockquote-em)
          (cond
            (= transition :empty-line)   (recur :blockquote-em lns acc block-acc)
            (= transition :blockquote)   (recur :blockquote lns acc (conj block-acc ln))
            :else (recur :default prev-lns (conj acc (parse-blockquote block-acc)) []))


          ;; ulist
          (= [state transition] [:default :ulist])
          (recur :ulist lns acc [ln])

          (= state :ulist)
          (cond
            (contains? #{:text :ulist} transition) (recur :ulist lns acc (conj block-acc ln))
            :else (recur :default prev-lns (conj acc (parse-list :ul block-acc)) []))

          ;; olist
          (= [:default :pre] [state transition])
          (recur :pre lns acc [ln])

          (= state :pre)
          (cond
            (= transition :pre) (recur :pre lns acc (conj block-acc ln))
            :else (recur :default prev-lns (conj acc (parse-pre block-acc)) []))

          ;; olist
          (= [:default :olist] [state transition])
          (recur :olist lns acc [ln])

          (= state :olist)
          (cond
            (contains? #{:text :olist} transition) (recur :olist lns acc (conj block-acc ln))
            :else (recur :default prev-lns (conj acc (parse-olist :ol block-acc)) []))


          ;; code
          (= [:default :code] [state transition])
          (recur :code lns acc [])

          (= [:default :empty-line] [state transition])
          (recur :default lns acc [])

          (= state :code)
          (cond
            (= transition :code) (recur :default lns (conj acc (parse-code block-acc)) [])
            :else (recur :code lns acc (conj block-acc ln)))


          ;; paragraph
          (= [:default :text] [state transition])
          ;; (and (not= state :blockquote) (= transition :blockquote))
          (recur :paragraph lns acc [ln])

          (= state :paragraph)
          (cond
            (= :old-header-1 transition) (recur :default lns (conj acc (parse-old-header :h1 block-acc)) [])
            (= :old-header-2 transition) (recur :default lns (conj acc (parse-old-header :h2 block-acc)) [])
            (= :text transition) (recur :paragraph lns acc (conj block-acc ln))
            (= :end-of-file transition)  (conj acc (parse-paragraph block-acc))
            (= :empty-line transition) (recur :default lns (conj acc (parse-paragraph block-acc)) [])
            :else (recur :default prev-lns (conj acc (parse-paragraph (conj block-acc ln))) []))

          (= transition :empty-line)
          (recur state lns (with-paragraph acc) block-acc)

          ;; alles
          (= transition :end-of-file) acc
          :else (recur :default lns (with-paragraph acc) []))))))

(defn parse [s]
  (into [:div.md] (*parse (str/split s #"(\r\n|\n|\r)"))))

; (parse "## Header [Some link](/url)")

;; (parse "1. Hello
;; 2. Allow
;; 3. Ballow")

(println "==================")

(parse "

heelo     
humka
")



(parse "

heelo  
humka
")
