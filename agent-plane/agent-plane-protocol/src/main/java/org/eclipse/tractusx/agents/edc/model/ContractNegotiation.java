//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.deser.std.UntypedObjectDeserializer;
import jakarta.json.JsonObject;
import org.eclipse.edc.connector.contract.spi.types.negotiation.ContractNegotiation.Type;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

/**
 * Result of a contract negotiation
 */
public class ContractNegotiation extends JsonLdObject {

    public ContractNegotiation(JsonObject node) {
        super(node);
    }

    public String getContractAgreementId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/contractAgreementId",null);
    }

    public String getState() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/state");
    }

    public String getErrorDetail() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/errorDetail",null);
    }
}
