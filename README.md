# Confucius

A Clojure library for declarative configurations.


# Motivation

TBD

* https://github.com/levand/immuconf
* https://github.com/jarohen/nomad
* http://12factor.net/config


# Usage

Example usage:

    (ns my.amazing.ns
     (:require
      [confucius.core :as c]))

    (c/load-config
      (clojure.java.io/resource "config-default.yml")
      (c/->url "platform.yml")
      (c/->url "host.json"))

`load-config` loads the configuration files from
any url and finally deep merges the results into a
single configuration map. Keys will be keywordized.

TBD


# Features? Suggesions? Issues?

Submit bug reports/patches etc through the GitHub repository
in the usual way. Cheers!

# License

Copyright © 2015 Fabio Bernasconi

Distributed under the Eclipse Public License, the same as Clojure.
