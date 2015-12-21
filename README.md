# Confucius [![Build Status][badge]][build]

A Clojure library for composable and declarative configuration.

*NOTE* This is pre-alpha software. Expect bugs and frequent api changes!

Suggestions on api, bugfixes, feature request, etc are all very welcome!


# Rationale

Although zero configuration is awesome, in most cases it is not achieavable
(sometimes though it is: [elasticsearch][elasticsearch]).
Applications that require configuration should read the values from the
environment they live in (see [12factor][12factor]). Unfortunately this may
clutter the application with `(Sytem/getenv)` calls that return un-typed data.
Having all the configuration related data in one handy map and provide sensible default values wherever possible should be the the way to go. This makes it easy
to add configuration to version control too!

`confucius` aims at solving exactly these problems. It builds on some of the
ideas from other libraries such as [immuconf][immuconf] (composability),
[nomad][nomad], or [environ][environ].


# Quick Start

The following clojure code will, given the configuration files
`awesome-service.yml`, `host.edn`, `subsys.json` (file contents
listed further down), yield

```clojure
(ns my.amazing.ns
 (:require
  [confucius.core  :as c]

  ;; If you want support for json and/or yaml you'll need to add
  ;; clojure.data.json or snakeyaml (or both) to your dependencies.
  ;;[confucius.ext.yaml]
  ;;[confucius.ext.json]
  [confucius.ext.all] ; this requires both

  [clojure.java.io :as io]))

(c/load-config
 ;; Why not calling prismatic's schema for validation & complex
 ;; coercion in postprocess?
 {:postprocess-fn identity} ; optional
 [{:global {:tmp-dir "${tmp.dir:/tmp}"}}
  (io/resource "awesome-service.yml")
  "host.edn"])

;; => {:global
;;      {:tmp-dir "/tmp"}
;;
;;     :http
;;      {:bind-address "0.0.0.0"
;;       :port 8080}
;;
;;      :audit-log-dir
;;      "/var/log/service/audit"  ;; assuming ${log-dir} was set to
;;                                ;; `/var/log` in the native environment
;;
;;      :subsys
;;      {:id "subsys1"}}
```

File contents:

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
audit-log: "${log-dir}/service/audit"
```

```json
# file: subsys.json (file system)
{ "id" : "subsys1" }
```

```edn
# file: host.edn
{:http {:port 8080}}
```

More to come... for an extensive example see the test ns
`confucius.example_test.clj`

See `confucius.cli` for an example how to integrate
command line argument parsing.

# Documentation

TODO


## Extending

TODO

`confucius.proto/ValueReader`

`confucius.proto/ConfigSource`

`confucius.proto/from-url`

# TODO

* support printing of configuration map
* integration with `tools.cli`? -> overwriting on the command line
* example integration with ``prismatic/schema`
* warn/log when config properties are being overwritten by
  other files. use metadata to show where the value came from?


# Features? Suggesions? Issues?

Submit bug reports/patches etc through the GitHub repository
in the usual way. Cheers!

# License

Copyright © 2015 Fabio Bernasconi

Distributed under the Eclipse Public License, the same as Clojure.

[badge]: https://travis-ci.org/instilled/confucius.svg?branch=master
[build]: https://travis-ci.org/instilled/confucius
[12factor]: http://12factor.net/config
[elasticsearch]: https://www.elastic.co/
[immuconf]: https://github.com/levand/immuconf
[nomad]: https://github.com/jarohen/nomad
[environ]: https://github.com/weavejester/environ
[schema]: https://github.com/Prismatic/schema
