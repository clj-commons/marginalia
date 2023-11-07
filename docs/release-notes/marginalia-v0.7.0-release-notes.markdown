Marginalia v0.7.0 Release Notes
===============================

Marginalia is an ultra-lightweight literate programming tool for Clojure inspired by [docco](http://jashkenas.github.com/docco/)*.

To get a quick look at what the output looks like, [visit the official Marginalia website](http://fogus.me/fun/marginalia/).

**Usage notes and examples are found on the [Marginalia Github page](https://github.com/clj-commons/marginalia).**

Places
------

* [Source code](https://github.com/clj-commons/marginalia)
* [Ticket system](https://github.com/clj-commons/marginalia/issues)
* [manifesto](http://blog.fogus.me/2011/01/05/the-marginalia-manifesto/)

Changes from v6.0.1
-------------------

### lein-marginalia

Version 0.7.0 is an attempt to move toward Marginalia as library only.  Therefore, the Leiningen support has been pulled out and placed into its own plugin named [lein-marginalia](https://github.com/fogus/lein-marginalia).  To use Marginalia to generate documentation for your own projects you should no longer reference Marginalia in your `project.clj`.  Instead, use lein-marginalia in your `:dev-dependencies` section like so:

    :dev-dependencies [[lein-marginalia "0.7.0"]]

Leiningen will pull in the proper Marginalia version.  We will attempt to keep the version numbers in sync.

### ClojureScript support

Marginalia will now discover and parse ClojureScript files.

### def docstrings

Clojure 1.3 allows docstrings in `def` forms that look like:

    (def a-var "The docstring" value)

Marginalia will recognize this pattern and generate the associate documentation.

### Wildcard arguments

Marginalia will accept wildcard arguments in place of absolute source paths.  For example, to generate docs for all source files with an 'r' in the name, you could type:

    lein marg src/**/*r*.clj

You can pass any number of arguments to the `marg` task.

### Bug fixes

* Prefixed keywords (#54 and #87)
* [`project.clj` requirement](https://github.com/clj-commons/marginalia/issues/20)
* [`^:private support`](https://github.com/clj-commons/marginalia/issues/49)
* [Comment code blocks](https://github.com/clj-commons/marginalia/issues/50)
* [`:requires` bug](https://github.com/clj-commons/marginalia/issues/55)


Plans
-----

The following capabilities are under design, development, or consideration for future versions of Marginalia:

* protocol docstring support
* Stand-alone application
* Explore the possibility of leveraging the [ClojureScript](https://github.com/clojure/clojurescript) analyzer.
* More documentation and examples

More planning is needed around capabilities not listed nor thought of.
