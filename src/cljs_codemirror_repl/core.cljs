(ns ^:figwheel-always cljs-codemirror-repl.core
    (:require-macros [cljs-codemirror-repl.macros :refer [read-resource]])
    (:require [cljs.js :as cljs]
              [cljs.tagged-literals :as tags]
              [cljs.tools.reader :as r]
              [cljs.analyzer :as ana]
              [cljs.compiler :as comp]))

(def styles (str (read-resource "resources/public/css/style.css")
                 (read-resource "resources/public/css/codemirror.css")
                 (read-resource "resources/public/css/theme/zenburn.css")))

(enable-console-print!)

(def localstorage-key "cljs-tilda-editor-last-snippet")

(defn on-js-reload [])

(defonce st (cljs/empty-state))
(defonce current-ns (atom 'cljs.user))
(defonce app-env (atom nil))

(declare codemirror-editor
         codemirror-log)

(defn log [s]
  (let [v (.getValue codemirror-log)]
    (.setValue codemirror-log (str s "\n" v))))

(defn eval-cljs [source]
  (binding [*ns* (create-ns @current-ns)
            ana/*cljs-ns* (create-ns @current-ns)]
    (try
      (cljs/eval-str st
                     source
                     source
                     {:ns @current-ns
                      :eval cljs/js-eval
                      :source-map false
                      :verbose true
                      :context :expr
                      :def-emits-var true
                      }
                     (fn [{:keys [ns value error] :as ret}]
                       (when error
                         (js/console.error (str error))
                         (log (.-cause error)))
                       (log value)
                       (prn value)))
      (catch :default e
        (js/console.error e)
        (log e)))))

(defn store-in-localstorage! [source]
  (js/localStorage.setItem localstorage-key source))

(defn get-from-localstorage! []
  (js/localStorage.getItem localstorage-key))

(defn read-and-eval! []
  (let [source (.getValue codemirror-editor)
        stdout (with-out-str (eval-cljs source))]
    (log stdout)
    (js/console.log stdout)
    (store-in-localstorage! source)))

(defonce main-container
  (let [el (js/document.createElement "div")]
    (set! (.-id el) "cljs-container")
    (js/document.body.appendChild el)
    el))

(defonce editor-element
  (let [el (js/document.createElement "div")]
    (set! (.-id el) "cljs-editor")
    (.appendChild main-container el)
    el))

(defonce log-element
  (let [el (js/document.createElement "div")]
    (set! (.-id el) "cljs-log")
    (.appendChild main-container el)
    el))

(defn initial-editor-value! []
  (or (get-from-localstorage!)
      "(defn test-function [arg] (println arg))\n\n(test-function \"Hello From ClojureScript!\")"))

(defonce codemirror-editor
  (js/CodeMirror. editor-element
                  #js {:value (initial-editor-value!)
                       :mode "clojure"
                       :theme "zenburn"}))

(defonce codemirror-log
  (js/CodeMirror. log-element
                  #js {:value "; log output goes here"
                       :mode "clojure"
                       :theme "zenburn"}))

(defn inline-styles! []
  (let [el (js/document.createElement "style")
        text-node (js/document.createTextNode styles)]
    (set! (.-type el) "text/css")
    (set! (.-rel el) "stylesheet")
    (set! (.-id el) "inlined-styles")
    (.appendChild el text-node)
    (.appendChild main-container el)))

(inline-styles!)

(.setOption codemirror-editor "extraKeys"
            #js {"Ctrl-Enter" read-and-eval!})

(defonce container-expanded (atom true))

(defn toggle-container-class! []
  (swap! container-expanded not)
  (if @container-expanded
    (set! (.-className main-container) "")
    (set! (.-className main-container) "collapsed")))

(defn bind-keypress! []
  (set! (.-onkeypress js/document)
        (fn [event]
          (when (= 36 (.-keyCode event))
            (toggle-container-class!)))))

(bind-keypress!)
