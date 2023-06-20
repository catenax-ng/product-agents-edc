//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import jakarta.json.JsonObject;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

public class ContractAgreement extends JsonLdObject {

    public ContractAgreement(JsonObject node) {
        super(node);
    }

    public String getAssetId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/assetId");
    }

    public long getContractSigningDate() {
        return object.getInt("https://w3id.org/edc/v0.0.1/ns/contractSigningDate");
    }
}