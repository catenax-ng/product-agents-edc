package org.eclipse.tractusx.agents.edc.model;

import jakarta.json.JsonObject;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

public class DcatDataset extends JsonLdObject {

    OdrlPolicy policy;

    public DcatDataset(JsonObject node) {
        super(node);
        policy=new OdrlPolicy(node.getJsonObject("http://www.w3.org/ns/odrl/2/hasPolicy"));
    }

    public OdrlPolicy hasPolicy() {
        return policy;
    }
}



