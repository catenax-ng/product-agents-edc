# Tractus-X Knowledge Agents EDC Extensions

The Tractus-X Knowledge Agents EDC Extensions repository creates runnable applications out of EDC extensions from
the [Eclipse DataSpace Connector](https://github.com/eclipse-edc/Connector) and [Tractus-X EDC](https://github.com/eclipse-tractusx/tractusx-edc) 
repositories.

When running an EDC connector from the Tractus-X Knowledge Agents EDC Extensions repository there are three setups to choose from. They only vary by
using different extensions for

- Resolving of Connector-Identities
- Persistence of the Control-Plane-State
- Persistence of Secrets (Vault)

## Connector Setup

The three supported setups are.

- Setup 1: Pure in Memory & Azure Vault **Not intended for production use!**
    - [Agent-Enabled Control Plane](../control-plane/controlplane-memory/README.md)
        - [Control Plane](https://github.com/eclipse-tractusx/edc-controlplane/edc-runtime-memory/README.md)
    - [Agent Plane](../agent-plane/agentplane-azure-vault/README.md)
        - [Data Plane](https://github.com/eclipse-tractusx/edc-dataplane/edc-dataplane-azure-vault/README.md)
        - [JWT Auth Extension](../common/jwt-auth/README.md)
- Setup 2: Pure in Memory & Hashicorp Vault **Not intended for production use!**
    - [Agent-Enabled Control Plane](../control-plane/controlplane-memory-hashicorp/README.md)
        - [Control Plane](https://github.com/eclipse-tractusx/edc-controlplane/edc-runtime-memory/README.md)
    - [Agent Plane](../agent-plane/agentplane-hashicorp/README.md)
        - [Data Plane](https://github.com/eclipse-tractusx/edc-dataplane/edc-dataplane-hashicorp-vault/README.md)
        - [JWT Auth Extension](../common/jwt-auth/README.md)
- Setup 3: PostgreSQL & HashiCorp Vault
    - [Agent-Enabled Control Plane](../control-plane/controlplane-postgresql-hashicorp/README.md)
        - [Control Plane](https://github.com/eclipse-tractusx/dc-controlplane/edc-controlplane-postgresql-hashicorp-vault/README.md)
    - [Agent Plane](../agent-plane/agentplane-hashicorp/README.md)
        - [Data Plane](https://github.com/eclipse-tractusx/edc-dataplane/edc-dataplane-hashicorp-vault/README.md)
        - [JWT Auth Extension](../common/jwt-auth/README.md)

## Recommended Documentation

### This Repository

- [Update EDC Version from 0.0.x - 0.1.x](migration/Version_0.0.x_0.1.x.md)
- [Application: Agent-Enabled Control Plane](../control-plane)
- [Application: Agent Plane](../agent-plane)
- [Extension: JWT Authentication](../common/auth-jwt/README.md)

### Tractus-X EDC

- [Tractus-X EDC Documentation](https://github.com/eclipse-tractusx/docs/Readme.md)

### Eclipse Dataspace Connector

- [EDC Domain Model](https://github.com/eclipse-edc/Connector/blob/main/docs/developer/architecture/domain-model.md)
- [EDC Open API Spec](https://github.com/eclipse-edc/Connector/blob/main/resources/openapi/openapi.yaml)
- [HTTP Receiver Extension](https://github.com/eclipse-edc/Connector/tree/main/extensions/control-plane/http-receiver)
