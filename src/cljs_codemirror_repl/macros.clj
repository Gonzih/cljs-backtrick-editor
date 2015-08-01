(ns cljs-codemirror-repl.macros)

(defmacro read-resource [path]
  (slurp path))
