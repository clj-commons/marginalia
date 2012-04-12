![marginalia](http://farm8.staticflickr.com/7057/6828224448_32b51e5784_z_d.jpg "Marginalia")

# Marginalia v0.7.0

*Ultra-lightweight literate programming[1] for Clojure inspired by [docco](http://jashkenas.github.com/docco/)*

Marginalia is a source code documentation tool that parses Clojure code and outputs a side-by-side source view with appropriate comments and docstrings aligned.  

To get a quick look at what marginalia output looks like, [visit the official site](http://fogus.me/fun/marginalia/).

**[View the release notes for this version of Marginalia](https://github.com/fogus/marginalia/blob/master/docs/release-notes/marginalia-v0.7.0-release-notes.markdown)**

Usage
-----

Currently Marginalia can be used in a number of ways as described below.

### Leiningen

[http://github.com/fogus/lein-marginalia](http://github.com/fogus/lein-marginalia)

To use Marginalia with Leiningen add the following code to the project's `project.clj` file, in the `:dev-dependencies` argument of the `defproject` function:

    :dev-dependencies [[lein-marginalia "0.7.0"]]

After executing `lein deps` you can generate your complete source documentation with the command:

    lein marg

Marginalia accepts other options as described in the *Command Line* section above.

### Maven

The [zi plugin](https://github.com/pallet/zi) supports Marginalia.

Add this code to the project's `pom.xml` file, and run the command `mvn zi:marginalia`.

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

And the following to the project's `settings.xml` file.

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

I would like to thank Zachary Kim for taking a pile of incoherent code and making it something worth using.  Marginalia would be nothing without his hard work and vision.

I would also like to thank Justin Balthrop and Brenton Ashworth for their support and code contributions.

License
-------

Copyright (C) 2010, 2011 Fogus and contributors.

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
- [Sam Ritchie](https://github.com/fogus/marginalia/commits/master?author=sritchie)
- [Hugo Duncan](https://github.com/hugoduncan)
- [Vadim](https://github.com/dm3)
- [Meikel Brandmeyer](https://github.com/kotarak)
- [Paul Dorman](https://github.com/pauldorman)
- [Deepak Giridharagopal](https://github.com/grimradical)
- [Tero Parviainen](https://github.com/teropa)

If I've missed your name then please ping me.
