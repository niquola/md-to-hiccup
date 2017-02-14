# md-to-hiccup

Generate hiccup from markdown in one pass

## Usage

```clj

(require '[md-to-hiccup.core :as mtc])

(mtc/parse "Hello *world*") => [:div.md "Hello " [:em "world"]]

```

## License

Copyright © 2017 niquola

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
