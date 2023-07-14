# Tractus-X Knowledge Agents EDC Extensions (KA-EDC)

[![Contributors][contributors-shield]][contributors-url]
[![Stargazers][stars-shield]][stars-url]
[![Apache 2.0 License][license-shield]][license-url]
[![Latest Release][release-shield]][release-url]

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=eclipse-tractusx_knowledge-agents-edc&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=eclipse-tractusx_knowledge-agents-edc)

KA-EDC is a product of the [Catena-X Knowledge Agents Kit](https://catenax-ng.github.io/product-knowledge) implementing the core modules of the CX-0084 standard (Federated Queries in Dataspaces).

* See the [documentation](docs/README.md)
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


