package org.eclipse.tractusx.agents.edc.jsonld;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.Map;

public class JsonLdObject {

    protected JsonObject object;

    public JsonLdObject(JsonObject object) {
        this.object=object;
    }

    public Map<String, JsonValue> getProperties() {
        return object;
    }

    public String getId() {
        return object.getString("@id");
    }

    public String asString() {
        return JsonLd.asString(object);
    }
}
