# agent-connector

![Version: 1.9.2](https://img.shields.io/badge/Version-1.9.2-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.9.2](https://img.shields.io/badge/AppVersion-1.9.2-informational?style=flat-square)

A Helm chart for Tractus-X Agent-Enabled Eclipse Data Space Connector

**Homepage:** <https://github.com/catenax-ng/product-agents-edc/tree/main/charts/agent-connector>

## TL;DR

```shell
helm repo add tractusx-edc https://eclipse-tractusx.github.io/charts/dev
helm install my-release tractusx-edc/tractusx-connector --version 1.9.2
```

## Source Code

* <https://github.com/catenax-ng/product-agents-edc/tree/main/charts/agent-connector>

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| backendService.httpProxyTokenReceiverUrl | string | `""` |  |
| controlplane.affinity | object | `{}` |  |
| controlplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| controlplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| controlplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| controlplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| controlplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| controlplane.businessPartnerValidation.log.agreementValidation | bool | `true` |  |
| controlplane.debug.enabled | bool | `false` |  |
| controlplane.debug.port | int | `1044` |  |
| controlplane.debug.suspendOnStart | bool | `false` |  |
| controlplane.endpoints | object | `{"control":{"path":"/control","port":8083},"default":{"path":"/api","port":8080},"management":{"authKey":"","path":"/management","port":8081},"metrics":{"path":"/metrics","port":9090},"observability":{"insecure":true,"path":"/observability","port":8085},"protocol":{"path":"/api/v1/dsp","port":8084}}` | endpoints of the control plane |
| controlplane.endpoints.control | object | `{"path":"/control","port":8083}` | control api, used for internal control calls. can be added to the internal ingress, but should probably not |
| controlplane.endpoints.control.path | string | `"/control"` | path for incoming api calls |
| controlplane.endpoints.control.port | int | `8083` | port for incoming api calls |
| controlplane.endpoints.default | object | `{"path":"/api","port":8080}` | default api for health checks, should not be added to any ingress |
| controlplane.endpoints.default.path | string | `"/api"` | path for incoming api calls |
| controlplane.endpoints.default.port | int | `8080` | port for incoming api calls |
| controlplane.endpoints.management | object | `{"authKey":"","path":"/management","port":8081}` | data management api, used by internal users, can be added to an ingress and must not be internet facing |
| controlplane.endpoints.management.authKey | string | `""` | authentication key, must be attached to each 'X-Api-Key' request header |
| controlplane.endpoints.management.path | string | `"/management"` | path for incoming api calls |
| controlplane.endpoints.management.port | int | `8081` | port for incoming api calls |
| controlplane.endpoints.metrics | object | `{"path":"/metrics","port":9090}` | metrics api, used for application metrics, must not be internet facing |
| controlplane.endpoints.metrics.path | string | `"/metrics"` | path for incoming api calls |
| controlplane.endpoints.metrics.port | int | `9090` | port for incoming api calls |
| controlplane.endpoints.observability | object | `{"insecure":true,"path":"/observability","port":8085}` | observability api with unsecured access, must not be internet facing |
| controlplane.endpoints.observability.insecure | bool | `true` | allow or disallow insecure access, i.e. access without authentication |
| controlplane.endpoints.observability.path | string | `"/observability"` | observability api, provides /health /readiness and /liveness endpoints |
| controlplane.endpoints.observability.port | int | `8085` | port for incoming API calls |
| controlplane.endpoints.protocol | object | `{"path":"/api/v1/dsp","port":8084}` | ids api, used for inter connector communication and must be internet facing |
| controlplane.endpoints.protocol.path | string | `"/api/v1/dsp"` | path for incoming api calls |
| controlplane.endpoints.protocol.port | int | `8084` | port for incoming api calls |
| controlplane.env | object | `{}` |  |
| controlplane.envConfigMapNames | list | `[]` |  |
| controlplane.envSecretNames | list | `[]` |  |
| controlplane.envValueFrom | object | `{}` |  |
| controlplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| controlplane.image.repository | string | `""` | Which derivate of the control plane to use. when left empty the deployment will select the correct image automatically |
| controlplane.image.tag | string | `"0.4.1"` | Overrides the image tag whose default is the chart appVersion |
| controlplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[0].enabled | bool | `false` |  |
| controlplane.ingresses[0].endpoints | list | `["protocol"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[0].hostname | string | `"edc-control.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.ingresses[1].annotations | object | `{}` | Additional ingress annotations to add |
| controlplane.ingresses[1].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| controlplane.ingresses[1].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| controlplane.ingresses[1].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| controlplane.ingresses[1].enabled | bool | `false` |  |
| controlplane.ingresses[1].endpoints | list | `["management","control"]` | EDC endpoints exposed by this ingress resource |
| controlplane.ingresses[1].hostname | string | `"edc-control.intranet"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| controlplane.ingresses[1].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| controlplane.ingresses[1].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| controlplane.ingresses[1].tls.secretName | string | `""` | If present overwrites the default secret name |
| controlplane.initContainers | list | `[]` |  |
| controlplane.internationalDataSpaces.catalogId | string | `"TXDC-Catalog"` |  |
| controlplane.internationalDataSpaces.curator | string | `""` |  |
| controlplane.internationalDataSpaces.description | string | `"Tractus-X Eclipse IDS Data Space Connector"` |  |
| controlplane.internationalDataSpaces.id | string | `"TXDC"` |  |
| controlplane.internationalDataSpaces.maintainer | string | `""` |  |
| controlplane.internationalDataSpaces.title | string | `""` |  |
| controlplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| controlplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| controlplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.logging | string | `".level=INFO\norg.eclipse.edc.level=ALL\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.ConsoleHandler.level=ALL\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| controlplane.nodeSelector | object | `{}` |  |
| controlplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| controlplane.podAnnotations | object | `{}` | additional annotations for the pod |
| controlplane.podLabels | object | `{}` | additional labels for the pod |
| controlplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| controlplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| controlplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| controlplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| controlplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| controlplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| controlplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| controlplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| controlplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a readiness check every 10 seconds |
| controlplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| controlplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| controlplane.replicaCount | int | `1` |  |
| controlplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| controlplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| controlplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| controlplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| controlplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| controlplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| controlplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| controlplane.service.annotations | object | `{}` |  |
| controlplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| controlplane.tolerations | list | `[]` |  |
| controlplane.url.ids | string | `""` | Explicitly declared url for reaching the ids api (e.g. if ingresses not used) |
| controlplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| controlplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| customLabels | object | `{}` |  |
| daps.clientId | string | `""` |  |
| daps.paths.jwks | string | `"/jwks.json"` |  |
| daps.paths.token | string | `"/token"` |  |
| daps.url | string | `""` |  |
| dataplanes.dataplane.affinity | object | `{}` |  |
| dataplanes.dataplane.agent | object | `{"connectors":[],"default":["dataspace.ttl","https://raw.githubusercontent.com/catenax-ng/product-knowledge/main/ontology/ontology.ttl"],"synchronization":-1}` | Agent-Specific Settings |
| dataplanes.dataplane.agent.connectors | list | `[]` | The list of remote connector IDS URLs to synchronize with |
| dataplanes.dataplane.agent.default | list | `["dataspace.ttl","https://raw.githubusercontent.com/catenax-ng/product-knowledge/main/ontology/ontology.ttl"]` | A list of local or remote graph descriptions to build the default meta-graph/federated data catalogue |
| dataplanes.dataplane.agent.synchronization | int | `-1` | The synchronization interval in ms to update the federated data catalogue |
| dataplanes.dataplane.autoscaling.enabled | bool | `false` | Enables [horizontal pod autoscaling](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/) |
| dataplanes.dataplane.autoscaling.maxReplicas | int | `100` | Maximum replicas if resource consumption exceeds resource threshholds |
| dataplanes.dataplane.autoscaling.minReplicas | int | `1` | Minimal replicas if resource consumption falls below resource threshholds |
| dataplanes.dataplane.autoscaling.targetCPUUtilizationPercentage | int | `80` | targetAverageUtilization of cpu provided to a pod |
| dataplanes.dataplane.autoscaling.targetMemoryUtilizationPercentage | int | `80` | targetAverageUtilization of memory provided to a pod |
| dataplanes.dataplane.aws.accessKeyId | string | `""` |  |
| dataplanes.dataplane.aws.endpointOverride | string | `""` |  |
| dataplanes.dataplane.aws.secretAccessKey | string | `""` |  |
| dataplanes.dataplane.configs | object | `{"dataspace.ttl":"#################################################################\n# Catena-X Agent Bootstrap Graph in TTL/RDF/OWL FORMAT\n#################################################################\n@prefix : <urn:cx:Graph:local:Dataspace> .\n@base <urn:cx:Graph:local:Dataspace> .\n"}` | A set of additional configuration files |
| dataplanes.dataplane.configs."dataspace.ttl" | string | `"#################################################################\n# Catena-X Agent Bootstrap Graph in TTL/RDF/OWL FORMAT\n#################################################################\n@prefix : <urn:cx:Graph:local:Dataspace> .\n@base <urn:cx:Graph:local:Dataspace> .\n"` | An example of an empty graph in ttl syntax |
| dataplanes.dataplane.debug.enabled | bool | `false` |  |
| dataplanes.dataplane.debug.port | int | `1044` |  |
| dataplanes.dataplane.debug.suspendOnStart | bool | `false` |  |
| dataplanes.dataplane.destinationTypes | string | `"HttpProxy"` | a comma-separated list of supported transfer types |
| dataplanes.dataplane.endpoints.callback.path | string | `"/callback"` |  |
| dataplanes.dataplane.endpoints.callback.port | int | `8087` |  |
| dataplanes.dataplane.endpoints.control.path | string | `"/api/dataplane/control"` |  |
| dataplanes.dataplane.endpoints.control.port | int | `8083` |  |
| dataplanes.dataplane.endpoints.default.path | string | `"/api"` |  |
| dataplanes.dataplane.endpoints.default.port | int | `8080` |  |
| dataplanes.dataplane.endpoints.metrics.path | string | `"/metrics"` |  |
| dataplanes.dataplane.endpoints.metrics.port | int | `9090` |  |
| dataplanes.dataplane.endpoints.observability.insecure | bool | `true` | allow or disallow insecure access, i.e. access without authentication |
| dataplanes.dataplane.endpoints.observability.path | string | `"/observability"` | observability api, provides /health /readiness and /liveness endpoints |
| dataplanes.dataplane.endpoints.observability.port | int | `8085` | port for incoming API calls |
| dataplanes.dataplane.endpoints.public.path | string | `"/api/public"` |  |
| dataplanes.dataplane.endpoints.public.port | int | `8081` |  |
| dataplanes.dataplane.env | object | `{}` |  |
| dataplanes.dataplane.envConfigMapNames | list | `[]` |  |
| dataplanes.dataplane.envSecretNames | list | `[]` |  |
| dataplanes.dataplane.envValueFrom | object | `{}` |  |
| dataplanes.dataplane.image.pullPolicy | string | `"IfNotPresent"` | [Kubernetes image pull policy](https://kubernetes.io/docs/concepts/containers/images/#image-pull-policy) to use |
| dataplanes.dataplane.image.repository | string | `""` | Which derivate of the data plane to use. when left empty the deployment will select the correct image automatically |
| dataplanes.dataplane.image.tag | string | `""` | Overrides the image tag whose default is the chart appVersion |
| dataplanes.dataplane.ingresses[0].annotations | object | `{}` | Additional ingress annotations to add |
| dataplanes.dataplane.ingresses[0].certManager.clusterIssuer | string | `""` | If preset enables certificate generation via cert-manager cluster-wide issuer |
| dataplanes.dataplane.ingresses[0].certManager.issuer | string | `""` | If preset enables certificate generation via cert-manager namespace scoped issuer |
| dataplanes.dataplane.ingresses[0].className | string | `""` | Defines the [ingress class](https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-class)  to use |
| dataplanes.dataplane.ingresses[0].enabled | bool | `false` |  |
| dataplanes.dataplane.ingresses[0].endpoints | list | `["public"]` | EDC endpoints exposed by this ingress resource |
| dataplanes.dataplane.ingresses[0].hostname | string | `"edc-data.local"` | The hostname to be used to precisely map incoming traffic onto the underlying network service |
| dataplanes.dataplane.ingresses[0].tls | object | `{"enabled":false,"secretName":""}` | TLS [tls class](https://kubernetes.io/docs/concepts/services-networking/ingress/#tls) applied to the ingress resource |
| dataplanes.dataplane.ingresses[0].tls.enabled | bool | `false` | Enables TLS on the ingress resource |
| dataplanes.dataplane.ingresses[0].tls.secretName | string | `""` | If present overwrites the default secret name |
| dataplanes.dataplane.initContainers | list | `[]` |  |
| dataplanes.dataplane.livenessProbe.enabled | bool | `true` | Whether to enable kubernetes [liveness-probe](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| dataplanes.dataplane.livenessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| dataplanes.dataplane.livenessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first liveness check |
| dataplanes.dataplane.livenessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| dataplanes.dataplane.livenessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| dataplanes.dataplane.livenessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| dataplanes.dataplane.logging | string | `".level=INFO\norg.eclipse.edc.level=ALL\nhandlers=java.util.logging.ConsoleHandler\njava.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter\njava.util.logging.ConsoleHandler.level=ALL\njava.util.logging.SimpleFormatter.format=[%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS] [%4$-7s] %5$s%6$s%n"` | configuration of the [Java Util Logging Facade](https://docs.oracle.com/javase/7/docs/technotes/guides/logging/overview.html) |
| dataplanes.dataplane.name | string | `"dataplane"` |  |
| dataplanes.dataplane.nodeSelector | object | `{}` |  |
| dataplanes.dataplane.opentelemetry | string | `"otel.javaagent.enabled=false\notel.javaagent.debug=false"` | configuration of the [Open Telemetry Agent](https://opentelemetry.io/docs/instrumentation/java/automatic/agent-config/) to collect and expose metrics |
| dataplanes.dataplane.podAnnotations | object | `{}` | additional annotations for the pod |
| dataplanes.dataplane.podLabels | object | `{}` | additional labels for the pod |
| dataplanes.dataplane.podSecurityContext | object | `{"fsGroup":10001,"runAsGroup":10001,"runAsUser":10001,"seccompProfile":{"type":"RuntimeDefault"}}` | The [pod security context](https://kubernetes.io/docs/tasks/configure-pod-container/security-context/#set-the-security-context-for-a-pod) defines privilege and access control settings for a Pod within the deployment |
| dataplanes.dataplane.podSecurityContext.fsGroup | int | `10001` | The owner for volumes and any files created within volumes will belong to this guid |
| dataplanes.dataplane.podSecurityContext.runAsGroup | int | `10001` | Processes within a pod will belong to this guid |
| dataplanes.dataplane.podSecurityContext.runAsUser | int | `10001` | Runs all processes within a pod with a special uid |
| dataplanes.dataplane.podSecurityContext.seccompProfile.type | string | `"RuntimeDefault"` | Restrict a Container's Syscalls with seccomp |
| dataplanes.dataplane.readinessProbe.enabled | bool | `true` | Whether to enable kubernetes [readiness-probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/) |
| dataplanes.dataplane.readinessProbe.failureThreshold | int | `6` | when a probe fails kubernetes will try 6 times before giving up |
| dataplanes.dataplane.readinessProbe.initialDelaySeconds | int | `30` | seconds to wait before performing the first readiness check |
| dataplanes.dataplane.readinessProbe.periodSeconds | int | `10` | this fields specifies that kubernetes should perform a liveness check every 10 seconds |
| dataplanes.dataplane.readinessProbe.successThreshold | int | `1` | number of consecutive successes for the probe to be considered successful after having failed |
| dataplanes.dataplane.readinessProbe.timeoutSeconds | int | `5` | number of seconds after which the probe times out |
| dataplanes.dataplane.replicaCount | int | `1` |  |
| dataplanes.dataplane.resources | object | `{}` | [resource management](https://kubernetes.io/docs/concepts/configuration/manage-resources-containers/) for the container |
| dataplanes.dataplane.securityContext.allowPrivilegeEscalation | bool | `false` | Controls [Privilege Escalation](https://kubernetes.io/docs/concepts/security/pod-security-policy/#privilege-escalation) enabling setuid binaries changing the effective user ID |
| dataplanes.dataplane.securityContext.capabilities.add | list | `[]` | Specifies which capabilities to add to issue specialized syscalls |
| dataplanes.dataplane.securityContext.capabilities.drop | list | `["ALL"]` | Specifies which capabilities to drop to reduce syscall attack surface |
| dataplanes.dataplane.securityContext.readOnlyRootFilesystem | bool | `true` | Whether the root filesystem is mounted in read-only mode |
| dataplanes.dataplane.securityContext.runAsNonRoot | bool | `true` | Requires the container to run without root privileges |
| dataplanes.dataplane.securityContext.runAsUser | int | `10001` | The container's process will run with the specified uid |
| dataplanes.dataplane.service.port | int | `80` |  |
| dataplanes.dataplane.service.type | string | `"ClusterIP"` | [Service type](https://kubernetes.io/docs/concepts/services-networking/service/#publishing-services-service-types) to expose the running application on a set of Pods as a network service. |
| dataplanes.dataplane.sourceTypes | string | `"cx-common:Protocol?w3c:http:SPARQL,HttpData"` | a comma-separated list of supported asset types |
| dataplanes.dataplane.tolerations | list | `[]` |  |
| dataplanes.dataplane.url.public | string | `""` | Explicitly declared url for reaching the public api (e.g. if ingresses not used) |
| dataplanes.dataplane.volumeMounts | list | `[]` | declare where to mount [volumes](https://kubernetes.io/docs/concepts/storage/volumes/) into the container |
| dataplanes.dataplane.volumes | list | `[]` | [volume](https://kubernetes.io/docs/concepts/storage/volumes/) directories |
| fullnameOverride | string | `""` |  |
| imagePullSecrets | list | `[]` | Existing image pull secret to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| nameOverride | string | `""` |  |
| partnerid | string | `"anonymous"` | Determines the business partner id |
| postgresql | object | `{"enabled":false,"jdbcUrl":null,"username":null}` | Standard settings for persistence, "jdbcUrl", "username" and "password" need to be overridden |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.create | bool | `true` |  |
| serviceAccount.imagePullSecrets | list | `[]` | Existing image pull secret bound to the service account to use to [obtain the container image from private registries](https://kubernetes.io/docs/concepts/containers/images/#using-a-private-registry) |
| serviceAccount.name | string | `""` |  |
| vault.azure.certificate | string | `nil` |  |
| vault.azure.client | string | `""` |  |
| vault.azure.enabled | bool | `false` |  |
| vault.azure.name | string | `""` |  |
| vault.azure.secret | string | `nil` |  |
| vault.azure.tenant | string | `""` |  |
| vault.hashicorp.enabled | bool | `false` |  |
| vault.hashicorp.healthCheck.enabled | bool | `true` |  |
| vault.hashicorp.healthCheck.standbyOk | bool | `true` |  |
| vault.hashicorp.paths.health | string | `"/v1/sys/health"` |  |
| vault.hashicorp.paths.secret | string | `"/v1/secret"` |  |
| vault.hashicorp.timeout | int | `30` |  |
| vault.hashicorp.token | string | `""` |  |
| vault.hashicorp.url | string | `""` |  |
| vault.secretNames.dapsPrivateKey | string | `"daps-private-key"` |  |
| vault.secretNames.dapsPublicKey | string | `"daps-public-key"` |  |
| vault.secretNames.transferProxyTokenEncryptionAesKey | string | `"transfer-proxy-token-encryption-aes-key"` |  |
| vault.secretNames.transferProxyTokenSignerPrivateKey | string | `"transfer-proxy-token-signer-private-key"` |  |
| vault.secretNames.transferProxyTokenSignerPublicKey | string | `"transfer-proxy-token-signer-public-key"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)
