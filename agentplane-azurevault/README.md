# Catena-X Knowledge Agents (Hey Catena!) EDC Agent Plane

This folder hosts the [Agent (Data) Plane for the Eclipse Dataspace Connector (EDC)](https://projects.eclipse.org/projects/technology.dataspaceconnector).

## Architecture

The Agent Data Plane is a variant of the Http (Proxy) Data Plane which
- has a direct endpoint for submitting (federated) queries in the supported inference languages (currently: SparQL)
- may negotiate further agreements for delegating sub-queries on the fly
- implements special Sources and Sinks for dealing/validating federated tokens.

## Notice

* see copyright notice in the top folder
* see license file in the top folder
* see authors file in the top folder
