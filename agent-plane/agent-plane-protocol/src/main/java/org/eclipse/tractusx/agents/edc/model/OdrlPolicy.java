//
// EDC Data Plane Agent Extension Test
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.model;

import jakarta.json.JsonObject;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

/**
 * represents a policy
 */
public class OdrlPolicy extends JsonLdObject {

    public OdrlPolicy(JsonObject node) {
        super(node);
    }

}
