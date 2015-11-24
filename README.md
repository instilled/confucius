# Confucius

A Clojure library for declarative configurations.


# Motivation

* https://github.com/levand/immuconf
* https://github.com/jarohen/nomad
* http://12factor.net/config


# Quick Start

Given the configuration files

```yaml
# cool.yml (on the classpath)

do-awesome-thing:
  every-sec: "${COOL.PROP:10}"

# replace value of `some-other-config` and
# `yet-some-other-config` with the data
# from `soc.json`
some-other-config: "@:cp://soc.json"
yet-some-other-config: "@:file://ysoc.yml"

```

the following code will load the configuration:

```clojure
(ns my.amazing.ns
 (:require
  [confucius.core  :as c]
  [clojure.java.io :as io))

(c/load-config
 (io/resource "cool.yml")
 (c/->url "platform.yml")   ;; Helper function to build urls
 (c/->url "host.json"))     ;; see doc for deatils.

=> {:global
     {:option 1}
    :amazing
     {:http
       {:bind-address "localhost"
        :port 1}}}
```

`load-config` load urls from left-to-right by (deep-)
merging each resulting map into a single configuration map.
This makes it possible to freely compose and override configuration
based. Values in the configuration may in turn again reference
other configuration (on the classpath, the filesystem or
by url) or reference environment vars or java system properties
with optional default values.


# Documentation




## Extending

`confucius.proto/from-url`

`confucius.proto/ToUrl`

`confucius.proto/ValueReader`


# Features? Suggesions? Issues?

Submit bug reports/patches etc through the GitHub repository
in the usual way. Cheers!

# License

Copyright © 2015 Fabio Bernasconi

Distributed under the Eclipse Public License, the same as Clojure.
