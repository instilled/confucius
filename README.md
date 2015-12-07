# Confucius

A Clojure library for declarative configuration.

*NOTE* This is pre-alpha software. Expect bugs and
frequent api changes!

Suggestions on api, bugfixes, feature request, etc are
all very welcome!

# Motivation

TBD

* https://github.com/levand/immuconf
* https://github.com/jarohen/nomad
* http://12factor.net/config


# TODO

* for better error messages for parser
* support printing of configuration map (and intermediate
  changes)
* warn/log when config properties are being overwritten by
  other files


# Quick Start

Given three configuration files

```yaml
# file: awesome-service.yml (on classpath)
http:
  bind-address: "0.0.0.0"
  port: 80

# Include configuration from `sub-system.json`.
# Supports relative file urls.
subsys: "@:file://./subsys.json"

# Reference (in that order) either
# another value in the configuration,
# a java system property or the native
# enrivonment
audit-log: "${install-dir:/tmp}/audit"
```

```json
# file: subsys.json (file system)
{ "id" : "subsys1" }
```

```yaml
# file: host.yml
http:
  port: 8080
```

and loading it as follows

```clojure
(ns my.amazing.ns
 (:require
  [confucius.core  :as c]
  [clojure.java.io :as io))

(c/load-config
 (io/resource "awesome-service.yml")
 (c/->url "host.yml"))
```

will result in the configuration map

```clojure
{:http
 {:bind-address "0.0.0.0"
  :port 8080}

 :audit-log-dir
 "/opt/service/tmp/audit"  ;; assuming ${install-dir} was
                           ;; set to `/opt/service` in the
                           ;; native environment

 :subsys
 {:id "subsys1"}}
```

More to come... for an extensive example see the test ns
`confucius.example_test.clj`

# Documentation

TBD


## Extending

TBD

`confucius.proto/from-url`

`confucius.proto/ToUrl`

`confucius.proto/ValueReader`


* Other projects

* https://github.com/levand/immuconf
* https://github.com/jarohen/nomad


# Features? Suggesions? Issues?

Submit bug reports/patches etc through the GitHub repository
in the usual way. Cheers!

# License

Copyright © 2015 Fabio Bernasconi

Distributed under the Eclipse Public License, the same as Clojure.
