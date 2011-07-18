Marginalia 0.6.0
==========
*ultra-lightweight literate programming[1] for clojure inspired by [docco](http://jashkenas.github.com/docco/)*

Marginalia is a source documentation too that parses Clojure code and outputs an side-by-side source view with appropriate comments and docstrings aligned.  

To get a quick look at what marginalia output looks like, then [visit the official site](http://fogus.me/fun/marginalia/).

Usage
-----

Currently Marginalia can be used in a number of ways as described below.

### Command Line

You can download the [Marginalia 0.6.0 jar including packaged dependencies from Github](https://github.com/downloads/fogus/marginalia/marginalia-0.6.0-standalone.jar).

Running Marginalia given the jar file linked above is as easy as:

    java -jar marginalia-0.6.0-standalone.jar

This will search the `PWD` for a `src` directory which it will then traverse looking for Clojure source files to parse and generate documentation for.  Marginalia also takes specific locations and files to generate docs for:

    java -jar marginalia-0.6.0-standalone.jar <file1> <file2> ... <filen>

Arguments can be specific files or directories.

### Leiningen

[http://github.com/fogus/lein-marginalia](http://github.com/fogus/lein-marginalia)

To use Marginalia in your own projects simply add the following to your `project.clj` file in the `:dev-dependencies` section:

    [lein-marginalia "0.6.0"]

After executing `lein deps` you can generate your complete source documentation with the following command:

    lein marg

Marginalia accepts other options as outlined in the *Command Line*
section above.

### Cake

[http://github.com/fogus/cake-marginalia](http://github.com/fogus/cake-marginalia)

Add the following to your project's `:dev-dependencies`:

    [cake-marginalia "0.6.0"]

Also, you need to add it to your task list:

     :tasks [cake-marginalia.tasks]

Here's a sample `project.clj`:

    (defproject my-project "0.1.0
      :description "Cake plugin for Marginalia."
      :dependencies [[org.clojure/clojure "1.2.1"]
                     [org.clojure/clojure-contrib "1.2.0"]]
      :dev-dependencies [[marginalia "0.6.0"]
                         [cake-marginalia "0.6.0"]]
      :tasks [cake-marginalia.tasks])

After that, you should be able to use it like any other cake task:

    cake marg

### Maven

The [zi plugin](https://github.com/pallet/zi) supports marginalia.

Add the folowing to your `pom.xml`, and run `mvn zi:marginalia`.

```xml
    <plugin>
      <groupId>org.cloudhoist.plugin</groupId>
      <artifactId>zi</artifactId>
      <version>0.3.1</version>
      <configuration>
        <marginaliaTargetDirectory>autodoc/marginalia</marginaliaTargetDirectory>
      </configuration>
    </plugin>
```

And the following to your `settings.xml`.

```xml
    <pluginGroups>
      <pluginGroup>org.cloudhoist.plugin</pluginGroup>
    </pluginGroups>

    <profiles>
      <profile>
        <id>clojure-dev</id>
        <pluginRepositories>
          <pluginRepository>
            <id>sonatype-snapshots</id>
            <url>http://oss.sonatype.org/content/repositories/releases</url>
          </pluginRepository>
        </pluginRepositories>
      </profile>
    </profiles>

    <activeProfiles>
      <activeProfile>clojure-dev</activeProfile>
    </activeProfiles>
```

Contributors and thanks
-----------------------

I would like to thank Zachary Kim for taking a pile of incoherant code and making it something worth using.  Marginalia would be nothing without his hard work and vision.

I would also like to thank Justin Balthrop and Brenton Ashworth for their support and code contributions.

TODO
----
* paragraph anchors
* options for non-uber-docs
* Maven generation support
* POM parsing

License
-------

Copyright (C) 2010 Fogus

Distributed under the Eclipse Public License, the same as Clojure.

Notes
-----

[1] While the phrase *ultra-lightweight literate programming* is used to describe Marginalia, it is in no way a tool for classical literate programming.  That is, Marginalia is a linear documentation generator allowing no out-of-order reassembly of source.

Marginalia is... 
----------------

*sorted by first commit*

- [Fogus](http://fogus.me/fun/)
- [Zachary Kim](https://github.com/zkim)
- [Justin Balthrop](https://github.com/ninjudd)
- [Brenton Ashworth](https://github.com/brentonashworth)
- [Nicolas Buduroi](https://github.com/budu)
- [Michael Harrison](https://github.com/goodmike)
- [Anthony Grimes](https://github.com/Raynes)
- [Hugo Duncan](https://github.com/hugoduncan)

If I've missed your name then please ping me.
