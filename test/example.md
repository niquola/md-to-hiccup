# An exhibit of Markdown

[![Build Status](https://travis-ci.org/niquola/md-to-hiccup.svg?branch=master)](https://travis-ci.org/niquola/md-to-hiccup)

This note demonstrates some of what [Markdown][1] is capable of doing.

*Note: Feel free to play with this page. Unlike regular notes, this doesn't automatically save itself.*

## Basic formatting

Paragraphs can be written like so. A paragraph is the basic block of Markdown. A paragraph is what text will turn into when there is no reason it should become anything else.

Paragraphs must be separated by a blank line. Basic formatting of *italics* and **bold** is supported. This *can be **nested** like* so.

## Lists

### Ordered list

1. Item 1
2. A second item
3. Number 3
4. Ⅳ

*Note: the fourth item uses the Unicode character for [Roman numeral four][2].*

### Unordered list

* An item
* Another item
* Yet another item
* And there's more...

## Paragraph modifiers

### Code block

    Code blocks are very useful for developers and other people who look at code or other things that are written in plain text. As you can see, it uses a fixed-width font.

You can also make `inline code` to add code into other things.

### Quote

> Here is a quote. What this is should be self explanatory. Quotes are automatically indented when they are used.

## Headings

There are six levels of headings. They correspond with the six levels of HTML headings. You've probably noticed them already in the page. Each level down uses one more hash character.

### Headings *can* also contain **formatting**

### They can even contain `inline code`

Of course, demonstrating what headings look like messes up the structure of the page.

I don't recommend using more than three or four levels of headings here, because, when you're smallest heading isn't too small, and you're largest heading isn't too big, and you want each size up to look noticeably larger and more important, there there are only so many sizes that you can use.

## URLs

URLs can be made in a handful of ways:

* A named link to [MarkItDown][3]. The easiest way to do these is to select what you want to make a link and hit `Ctrl+L`.
* Another named link to [MarkItDown](http://www.markitdown.net/)
* Sometimes you just want a URL like <http://www.markitdown.net/>.

## Horizontal rule

A horizontal rule is a line that goes across the middle of the page.

---

It's sometimes handy for breaking things up.

## Images

Markdown can also contain images. I'll need to add something here sometime.

## Code

```clj
(defn- parse-code [[ln & lns :as prev-lns]]
  (let [lang (second (str/split ln #"```" 2))
        attrs (if (and lang (not (str/blank? lang))) {:lang lang :class lang} {})]
    (loop [acc []
           [ln & lns :as prev-lns] lns]
      (if-not ln
        [[:pre [:code attrs (str/join "\n" acc)]] []]
        (if (str/starts-with? ln "```")
          [[:pre [:code attrs (str/join "\n" acc)]] (or lns [])]
          (recur (conj acc ln) lns))))))
```

## Finally

There's actually a lot more to Markdown than this. See the official [introduction][4] and [syntax][5] for more information. However, be aware that this is not using the official implementation, and this might work subtly differently in some of the little things.


  [1]: http://daringfireball.net/projects/markdown/
  [2]: http://www.fileformat.info/info/unicode/char/2163/index.htm
  [3]: http://www.markitdown.net/
  [4]: http://daringfireball.net/projects/markdown/basics
  [5]: http://daringfireball.net/projects/markdown/syntax

# Sample app tutorial

We are offering this tutorial for learning core concepts of application development with Aidbox. We will develop a sample [AngularJS](https://angularjs.org), Web application which consists of a form for onboarding patients (CRUD) with a search by name function.

![Sample SPA](/imgs/docs/spa_tutorial/spa01.png)

After completing this tutorial you should learn:

* how to create Aidbox Web applications with NodeJS and AngularJS
* how to use aidbox-cli
* how to use REST API and query the FHIR server
* __What else?__

## Get started

To start your development you have to install NodeJS. All the instructions how to install NODE.JS you can find at [nodejs.org/download](https://nodejs.org/download/)
. The next step of preparing your development environment is installation of aidbox-cli and box setup. All the information you need for working with aidbox-cli you can find on [its official page](https://www.npmjs.com/package/aidbox-cli).


```sh
$ npm install -g aidbox-cli
$ aidbox v
```

## Sample app structure

Sample app consists of three main files:

```bsh
├── package.json
└── dist
    ├── app.js
    └── index.html
```

* ``package.json`` - manifest file of your app. It contains the name of your project, its version, dependencies, commands to run etc You can read more about package.json file on [this page](https://docs.npmjs.com/files/package.json) of npm documentation.
* ``index.html`` - the first HTML file of your application.
* ``app.js`` - main part of your app. It contains all business logic, queries to the FHIR server via REST API, authorisation etc

Let’s look at each file more closely

__Package.json__

``package.json`` contains only minimally required settings, one dependency ``http-server`` and one command ``start`` for starting a local Web server on the port 5000.

``package.json``
```js
{
  "name": "Aidbox-sample-spa",
  "version": "0.0.0",
  "dependencies": { },
  "devDependencies": {
    "http-server": "latest"
  },
  "engines": {
    "node": ">=0.12.0"
  },
  "scripts": {
    "start": "`npm bin`/http-server dist -p 5000"
  }
}
```

__index.html__

To simplify the app let’s load all the styles and scripts from the CDN. Please pay attention that   ``angular-aidbox.js`` - is a service for the AngularJS framework providing REST API and authorisation implementation. You can read more details about this project on the [aidboxjs](https://github.com/Aidbox/aidboxjs) official page.

``index.html``
```html
<!doctype html>
<html ng-app="app">
  <head>
    <meta charset="utf-8">
    <title>My sample app</title>
    <link rel="stylesheet" href="//maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css" />
    <script src="//cdnjs.cloudflare.com/ajax/libs/angular.js/1.4.1/angular.min.js" ></script>
    <script src="//cdnjs.cloudflare.com/ajax/libs/angular.js/1.4.1/angular-cookies.min.js" ></script>
   <script src="//aidbox.io/angular-aidbox.js" ></script>
  </head>
  <body>
    <nav class="navbar navbar-default navbar-static-top" role="navigation">
      <div class="container">
        <div id="navbar" class="navbar">
          <ul class="nav navbar-nav navbar-right">
            <li ng-if="user" class="login">
              <a href="#">{{user.email}}</a>
            </li>
            <li ng-if="!user">
              <a ng-click="auth.signin()" href="#">Sign in</a>
            </li>
            <li ng-if="user">
              <a ng-click="auth.signout()" href="#">Sign out</a>
            </li>
          </ul>
        </div>
      </div>
    </nav>
    <div class="container">
      <h3>Sample app</h3>
      <div ng-show="user" class="ng-hide">
      </div>
    </div>
  </body>
</html>
```

__app.js__

``app.js``
```js
(function() {
    var BOX_URL = 'https://myapp.aidbox.io';
    var app = angular.module('app', ['ngCookies', 'ngAidbox']);
    app.run(function($rootScope, $aidbox) {
        $aidbox.init({
            box: BOX_URL,
            onSignIn: function(user) {
                $rootScope.user = user;
            },
            onSignOut: function() {
                $rootScope.user = null;
            }
        });
    });
})();
```

Run

```bsh
npm run start
```

## Create patient form


## Patients list

## Patients CRUD

## Deploy

```bsh
aibox deploy
```
