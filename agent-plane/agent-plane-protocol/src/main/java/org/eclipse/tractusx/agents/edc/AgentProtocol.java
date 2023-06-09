//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc;

/**
 * lists the various protocols supported
 */
public enum AgentProtocol {

    SPARQL_HTTP("cx-common:Protocol?w3c:http:SPARQL"),
    SKILL_HTTP("cx-common:Protocol?w3c:http:SKILL");

    private final String protocolId;

    AgentProtocol(String protocolId) {
        this.protocolId=protocolId;
    }

    public String getProtocolId() {
        return protocolId;
    }
}
