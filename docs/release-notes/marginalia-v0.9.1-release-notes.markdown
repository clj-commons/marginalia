Marginalia v0.9.1 Release Notes
===============================

Marginalia is an ultra-lightweight literate programming tool for Clojure and ClojureScript inspired by [docco](http://jashkenas.github.com/docco/)*.

To get a quick look at what the output looks like, [visit the official Marginalia website](http://fogus.me/fun/marginalia/).

**Usage notes and examples are found on the [Marginalia Github page](http://github.com/gdeer81/marginalia).**

Places
------

* [Source code](https://github.com/gdeer81/marginalia)
* [Ticket system](https://github.com/gdeer81/marginalia/issues)
* [manifesto](http://blog.fogus.me/2011/01/05/the-marginalia-manifesto/)

Changes from v0.8.0
-------------------

### lein-marginalia

As always, the prefered way to use Marginalia to generate your documentation is via the [lein-marginalia](http://github.com/fogus/lein-marginalia) Leiningen plugin, like so:

    :dev-dependencies [[lein-marginalia "0.9.1"]]

To run Marginalia, simply run `lein marg <options> <files>` in your project's directory.

### Multidoc Generation

Marginalia has long supported the generation of documentation where each namespace is contained in its own HTML file. This feature is finally exposed via the command-line/Lein interface and executed as `lein marg --multi <more options> <files>`.

### Bug fixes

* Fix no newline at the end of parser.clj.
* Fix no namespace extracted from .cljx files.
* Add support for cljx reader tags #+clj and #+cljs to make it possible
* handle (ignore) extra args from LispReader 
* fixed regex helper


Plans
-----

The following capabilities are under design, development, or consideration for future versions of Marginalia:

* Nicer looking `toc.html` generation in `--multi` mode output.
* protocol docstring support
* Explore the possibility of leveraging the [ClojureScript](http://github.com/clojure/clojurescript) analyzer.
* Explore the possibility of leveraging [sjacket](https://github.com/cgrand/sjacket)
* More documentation and examples

More planning is needed around capabilities not listed nor thought of.
