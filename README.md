# igraph-loom

This is a port of the [loom](https://github.com/aysylu/loom) API to the IGraph protocol.

Part of the ont-app project.

It is in its very earliest phase of development.


## Installation

Available a clojars as
ont-app/igraph-loom "0.1.0-SNAPSHOT"

## Usage

Require thus:

```
(ns my.ns
 (:require
   [loom.graph :as loom]
   [ont-app.igraph.core :as igraph :refer :all]
   [ont-app.igraph-loom.core :as lgraph]
   ))
```

You can create a new instance of a lgraph/LoomGraph with an arity-0
call, which will instantiate an instance of
`loom.graph.BasicEditableDigraph` as its native representation.


```
> (def g (lgraph/make-loom-graph))
{:loom-graph {:nodeset #{}, :adj {}, :in {}}}
```

You should also be able to add any object that implements all of the following loom protocols:

- loom.graph/Graph
- loom.graph/EditableGraph
- loom.graph/Digraph
- loom.attr/AttrGraph

```
> (def g (lgraph/make-loom-graph (loom/digraph)))
```

Then use the usual igraph immutable methods to add triples and access the contents:

```
> (def g' (add g [:john :loves :mary]))

> (g')
{:john {:loves #{:mary}}}

> (g' :john)
{:loves #{:mary}}

> (g' :john :loves)
#{:mary}

> (g' :john :loves :mary)
:mary ;; truthy
```

The property pertaining to each edge is named using a :label
attribute. At this early stage, best practice is to use keywords to
name each node.

You can access the underlying loom representation directly:

```
> (:loom-graph g')
{:nodeset #{:john :mary},
 :adj {:john #{:mary}},
 :in {:mary #{:john}},
 :attrs {:john #:loom.attr{:edge-attrs {:mary {:label :loves}}}}}
```


## License

Copyright © 2021 Eric D. Scott

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
