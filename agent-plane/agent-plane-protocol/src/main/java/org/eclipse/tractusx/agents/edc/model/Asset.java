//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
package org.eclipse.tractusx.agents.edc.model;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

import java.util.Map;

/**
 * represents an asset
 */
public class Asset extends JsonLdObject {

    Map<String, JsonValue> publicProperties;
    Map<String, JsonValue> privateProperties;

    public Asset(JsonObject node) {
        super(node);
        this.publicProperties=node.getJsonObject("https://w3id.org/edc/v0.0.1/ns/properties");
        this.privateProperties=node.getJsonObject("https://w3id.org/edc/v0.0.1/ns/privateProperties");
    }

    public Map<String, JsonValue> getPrivateProperties() {
        return privateProperties;
    }

    public Map<String, JsonValue> getPublicProperties() {
        return publicProperties;
    }

}
