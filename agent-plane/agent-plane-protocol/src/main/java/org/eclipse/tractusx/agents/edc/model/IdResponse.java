//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.model;

import jakarta.json.JsonObject;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

/**
 * represents a response object
 */
public class IdResponse extends JsonLdObject {

    public IdResponse(JsonObject node) {
        super(node);
    }

}
