Marginalia
==========
*ultra-lightweight literate programming[1] for clojure inspired by [docco](http://jashkenas.github.com/docco/)*

Marginalia is a source documentation too that parses Clojure code and outputs an side-by-side source view with appropriate comments and docstrings aligned.  

To get a quick look at what marginalia output looks like:

1. `git clone https://github.com/fogus/marginalia.git`
2. `open ./marginalia/example-output/uberdoc.html` (or [look here](http://fogus.me/fun/marginalia/))

Usage
-----

Currently Marginalia can be used in a number of ways as described below.

### Command Line

You can download the [Marginalia 0.3.0 jar including packaged dependencies from Github](https://github.com/downloads/fogus/marginalia/marginalia-0.3.0-standalone.jar).

Running Marginalia given the jar file linked above is as easy as:

    java -jar marginalia-0.3.0-standalone.jar

This will search the `PWD` for a `src` directory which it will then traverse looking for Clojure source files to parse and generate documentation for.  Marginalia also takes specific locations and files to generate docs for:

    java -jar marginalia-0.3.0-standalone.jar <file1> <file2> ... <filen>

Arguments can be specific files or directories.

### Leiningen

To use Marginalia in your own projects simply add the following to your `project.clj` file in the `:dev-dependencies` section:

    [marginalia "0.3.0"]

After executing `lein deps` you can generate your complete source documentation with the following command:

    lein marg

Marginalia accepts other options as outlined in the *Command Line* section above.

### Cake

TBD

### Maven

Not yet supported.

Contributors and thanks
-----------------------

I would like to thank Zachary Kim for taking a pile of incoherant code and making it something worth using.  Marginalia would be nothing without his hard work and vision.

I would also like to thank Justin Balthrop and Brenton Ashworth for their support and code contributions.

TODO
----
* paragraph anchors
* options for non-uber-docs
* new docstring/comment reader
* Maven generation support
* POM parsing

License
-------

Copyright (C) 2010 Fogus

Distributed under the Eclipse Public License, the same as Clojure.

Notes
-----

[1] While the phrase *ultra-lightweight literate programming* is used to describe Marginalia, it is in no way a tool for classical literate programming.  That is, Marginalia is a linear documentation generator allowing no out-of-order reassembly of source.
