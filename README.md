# Tractus-X Knowledge Agents EDC Extensions (KA-EDC)

[![Contributors][contributors-shield]][contributors-url]
[![Stargazers][stars-shield]][stars-url]
[![Apache 2.0 License][license-shield]][license-url]
[![Latest Release][release-shield]][release-url]

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-tractusx_knowledge-agents-edc&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-tractusx_knowledge-agents-edc)

KA-EDC is a product of the [Catena-X Knowledge Agents Kit](https://catenax-ng.github.io/product-knowledge) implementing the core modules of the CX-0084 standard (Federated Queries in Dataspaces).

* See the [user documentation](docs/README.md)
* See the [copyright notice](COPYRIGHT.md)
* See the [authors file](AUTHORS.md)
* See the [license file](LICENSE.md)
* See the [code of conduct](CODE_OF_CONDUCT.md)
* See the [contribution guidelines](CONTRIBUTING.md)
* See the [dependencies and their licenses](DEPENDENCIES.md)

## About the Project 

This repository hosts the relevant reference extensions to the [Eclipse Dataspace Components (EDC)](https://github.com/eclipse-edc/Connector).
It provides container images and deployments for a ready-made KA-enabled [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc).

In particular, KA-EDC consists of

- [Common](common) extensions in order to allow for secure and personalized application access to the EDC infrastructure.
- [Agent (Data) Plane](agent-plane) extensions to ingest, validate, process and delegate federated procedure calls (so-called Skills) on top of data and functional assets. In particular, they implement the [Semantic Web](https://www.w3.org/standards/semanticweb/) [SPARQL](https://www.w3.org/TR/sparql11-query/)_ protocol. 
- [Helm Charts](charts) for umbrella deployments. 

## Source Code Layout

![Source Code](docs/KA-EDC.drawio.svg)

Above is a collaboration map of the main implementation classes found in this repository.

It starts with an application performing a SPARQL call against the AgentController. This call may be handled - depending on the configuration - by a single or a composed AuthenticationService which analyses diverse authorisation features of the incoming request, such as a JWT-based bearer token against an OpenId servers public certificate and its validity period.

The AgentController delegates the call upon preprocessing (e.g. by resolving local Skill Asset references using the EdcSkillStore) to the actual SparQLProcessor (an instance of an Apache Jena SparQL Processor). The SparQLProcessor is backed by an RDF store which hosts the Federated Data Catalogue (and that is regularly synchronized by the DataspaceSynchronizer).

Whenever external SERVICE references are to be executed, the SparQLProcessor will ask the DataspaceServiceExecutor to execute the actual sub-operation. This operation could either point to a tenant-internal or public endpoint. The operation could also hint to a dataspace address. In the latter case, DataspaceServiceExecutor will ask the AgreementController for help.

The AgreementController has a list of already negotiated Dataspace Assets and corresponding EndpointDataReferences to the Data Plane. If such an EDR does not yet exist, it will negotiate one with the help of the DataManagement service. The EDR will then be asynchronously handed out to the AgreementController and returned to DataspaceServiceExecutor to perform the Dataspace Transfer Call.

When the call arrives at the provider data plane, it will hit the AgentSource. Similar to AgentController, AgentSource performs some preprocessing and validity checking before delegating to the SparQLProcessor (and the recursion may go further ...)

## Getting Started

### Build

To compile, package and containerize the binary artifacts (includes running the unit tests)

```shell
mvn package -Pwith-docker-image
```

To publish the binary artifacts (environment variables GITHUB_ACTOR and GITHUB_TOKEN must be set)

```shell
mvn -s settings.xml publish
```

### Deployment

* See the [user documentation](docs/README.md)


