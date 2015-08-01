(ns ^:figwheel-always cljs-codemirror-repl.core
    (:require [cljs.js :as cljs]
              [cljs.tagged-literals :as tags]
              [cljs.tools.reader :as r]
              [cljs.analyzer :as ana]
              [cljs.compiler :as comp]
              [cljs-codemirror-repl.sandbox]))

(enable-console-print!)

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
                       (when value
                         (log value))
                       (prn value)))
      (catch :default e
        (js/console.error e)
        (log e)))))

(defn read-and-eval! []
  (let [source (.getValue codemirror-editor)]
    (eval-cljs source)))

(defonce main-container
  (let [el (js/document.createElement "div")]
    (set! (.-id el) "cljs-container")
    (js/document.body.appendChild el)
    el))

(defonce editor-element
  (let [el (js/document.createElement "div")]
    (set! (.-id el) "editor")
    (.appendChild main-container el)
    el))

(defonce log-element
  (let [el (js/document.createElement "div")]
    (set! (.-id el) "log")
    (.appendChild main-container el)
    el))

(defonce button-element
  (let [el (js/document.createElement "input")]
    (set! (.-type el) "button")
    (set! (.-value el) "Eval")
    (set! (.-id el) "eval-button")
    (set! (.-onclick el) read-and-eval!)
    (.appendChild main-container el)
    el))

(defonce codemirror-editor
  (js/CodeMirror. editor-element
                  #js {:value "(def test-var 1) \n(prn test-var)"
                       :mode "clojure"
                       :theme "zenburn"}))

(defonce codemirror-log
  (js/CodeMirror. log-element
                  #js {:value ""
                       :mode "clojure"
                       :theme "zenburn"}))
