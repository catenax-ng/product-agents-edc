# Dependencies of Tractus-X Knowledge Agents EDC Extensions

The following is a simple type of single-level Software-BOM for all official open source products of Catena-X Knowledge Agents. 

* Product - The name of the Epic/Product (* for all)
* Component - The specific sub-component of the Epic/Product (* for all)
* Library/Module - The library or module that the Product/Component is depending on
* Stage - The kind of dependency 
  * Compile - The library is needed to compile the source code of the component into the target artifact (runtime)
  * Test - The library is needed to test the target artifact
  * Packaging - The library is needed to test the target artifact before, while and/or after packaging it
  * Runtime - The library is shipped as a part of the target artifact (runtime)
  * Provided - The library is not shipped as a part of the target artifact, but needed in it runtime
  * All - The library is needed at all Stages
* Version - the version of the library that the component is dependant upon
* License - the license identifier
* Comment - any further remarks on the kind of dependency

| Component | Library/Module  | Version | Stage | License | Comment |
| -- | --- | --- | --- | --- | ---| 
| * | [Apache Maven](https://maven.apache.org) | >=3.8 | Compile + Test + Packaging | Apache License 2.0 |     |
| * | Docker Engine | >=20.10.17 | Packaging + Provided | Apache License 2.0 |     |
| * | [kubernetes](https://kubernetes.io/de/)/[helm](https://helm.sh/) | >=1.20/3.9 | Provided | Apache License 2.0 |     |
| * | [Python](https://www.python.org/) | >=3.9 | Test + Packaging + Provided | Zero Clause BSD |     |
| * | [Java Runtime Environment (JRE)](https://de.wikipedia.org/wiki/Java-Laufzeitumgebung) | >=11 | Test + Provided | * | License (GPL, BCL, ...) depends on choosen runtime. |
| * | [Java Development Kit (JDK)](https://de.wikipedia.org/wiki/Java_Development_Kit) | >=11 | Compile + Packaging | * | License (GPL, BCL, ...) depends on choosen kit. |
| * | [Junit Jupiter](https://junit.org) | >=5 | Test | MIT |     |
| * | [EDC](https://github.com/eclipse-edc) | >=0.0.1-20230220.patch1 | All | Apache License 2.0 |     |
| * | [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc) | >=0.3.3 | All | Apache License 2.0 |     |
| EDC Agent Plane | [Apache Jena Fuseki](https://jena.apache.org/) | >=2.0.0 | All | Apache License 2.0 |     |
| EDC Agent Plane | [Jakarta RESTful Web Services](https://projects.eclipse.org/projects/ee4j.rest) | >=3.1.0 | All | Eclipse Public License (2.0) |     |
| EDC Agent Plane | [Javax Servlet API](https://de.wikipedia.org/wiki/Jakarta_Servlet) | >=4.0.1 | All | Common Development & Distribution License |     |
| EDC Agent Plane | [Jakarta Servlet API](https://projects.eclipse.org/projects/ee4j.servlet) | >=5.0.2 | All | Eclipse Public License (2.0) |     |
| EDC Agent Plane | [Java JWT](https://github.com/auth0/java-jwt) | >=4.0.0 | All | MIT |     |
| EDC Agent Plane | [Azure SDK for Java](https://github.com/Azure/azure-sdk-for-java) | >=1.2.2 | All | MIT |     |
| EDC Agent Plane | [Failsafe](https://failsafe.dev/) | >=3.2.4 | All | Apache License 2.0 |     |
| EDC Agent Plane | [Mockito](https://site.mockito.org/) | >=4.6.1 | Test | MIT |     | 
