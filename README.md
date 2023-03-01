# in-memory-property-aware-temporal-graph

**in-memory-property-aware-temporal-graph** aims to provide an in-memory graph database that stores nodes and relationships between nodes in a versioned/time-aware manner.

### Motivation

Large datacenters contain tens of thousands of related entities for e.g. a host is a container for potentially hundreds of virtual machine workloads executing on the host. Similarly, a kubernetes service is an abstraction for one or more pod replicas servicing the requests directed to the service.
Also in a data center workloads communicate with each thereby creating an implicit communication graph where the vertices correspond to workloads and the edges correspond to the network traffic between the workloads. Such relations can be naturally modeled as graphs which can then be used for efficient querying based on relationship between entities.

For large data-center environments, it is often beneficial to maintain the history of the attribute values for various relationships. This helps in-depth troubleshooting, forensics and root cause analysis of problems within the data center. This brings forth a critical requirement of maintaining temporal information with respect to the edges and the vertices of the graph along with their properties.

While traditional graph databases such as TinkerGraph, Neo4J and JanusGraph expose a robust graph subsystem along with traversal and query facilities such as TinkerPop Gremlin, they also introduce the following complications:
* Varied support for features such as multi-valued attributes, control over id generation etc. For e.g. JanusGraph does not support multi-valued attributes whereas Neo4j does.
* Each of these databases have varied level of support for TinkerPop standard.
* Have a significantly heavy footprint in terms of deployment, management and resource consumption patterns.
* Consuming these graph database services within other applications would require large number of dependencies in terms of database specific librairies and other software requirement

The above limitations prompted the design pointed to the requirement of a light-weight graph database framework that exposes:
* A simple, compact API interface to consumers to work with time-aware graph entities.
* Rely on the facilties provided by the core Java language as much as possible along with some common third party dependencies whereever required.
* Facilitate consumption of the graph database/storage as a library within the application itself.

It should be noted that while the motivation for the graph database was primarily problems encountered when troubleshooting datacenters, the graph database implemented here is agnostic of the domain and could be used as a generic library whereever maintaining the history of relationships between entities along with the relationship and entity attributes is required.

### Technical details
**in-memory-property-aware-temporal-graph** exposes the following primitives to consumers:
* Property - represents a property (a name-value pair) associated with an edge or a vertex. The values for a property are stored in a time-series aware manner. Hence it is possible to retrieve the values of the property over a time range
* Vertex - represents a traditional graph vertex. Each vertex could have zero or more properties associated with it. Additionally, every vertex could potentially have zero or more edges (incoming or outgoing) associated with itself. The properties and edges associated with a vertex are stored in a time-aware fashion making it possible to retrieve the set of edges or properties for a vertex at a specific point in time or over a time range*
* Edge - represents a traditional edge within a graph. Each edge could have zero or more properties associated with it. The properties of the edge are stored in a time aware fashion, making it possible to retrieve the set of properties associated with an edge either at a specific point in time or over a time range.

### Prerequisites

* jdk11 or above
* gradle

### Build & Run

1. To Build: ./gradlew clean build
2. To run the jmh benchmarking: ./gradlew jmh


## Contributing

The in-memory-property-aware-temporal-graph project team welcomes contributions from the community. Before you start working with in-memory-property-aware-temporal-graph, please
read our [Developer Certificate of Origin](https://cla.vmware.com/dco). All contributions to this repository must be
signed as described on that page. Your signature certifies that you wrote the patch or have the right to pass it on
as an open-source patch. For more detailed information, refer to [CONTRIBUTING.md](CONTRIBUTING_DCO.md).
