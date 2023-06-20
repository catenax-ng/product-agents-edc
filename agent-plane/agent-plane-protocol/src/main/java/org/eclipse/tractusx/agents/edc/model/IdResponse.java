package org.eclipse.tractusx.agents.edc.model;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

import java.util.ArrayList;
import java.util.List;

public class IdResponse extends JsonLdObject {

    public IdResponse(JsonObject node) {
        super(node);
    }

}
