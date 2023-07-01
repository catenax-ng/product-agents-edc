//
// EDC Data Plane Agent Extension Test
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.jsonld;

import jakarta.json.*;
import org.eclipse.tractusx.agents.edc.model.*;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * base facility to deal with EDC specific JSONLD structures
 */
public class JsonLd {

    public static DcatCatalog processCatalog(String cat) {
        return processCatalog(Json.createReader(new StringReader(cat)).readObject());
    }

    public static DcatCatalog processCatalog(JsonObject cat) {
        return new DcatCatalog(processJsonLd(cat,null));
    }

    public static IdResponse processIdResponse(String response) {
        return processIdResponse(Json.createReader(new StringReader(response)).readObject());
    }

    public static IdResponse processIdResponse(JsonObject response) {
        return new IdResponse(processJsonLd(response,null));
    }

    public static ContractNegotiation processContractNegotiation(String response) {
        return processContractNegotiation(Json.createReader(new StringReader(response)).readObject());
    }

    public static ContractNegotiation processContractNegotiation(JsonObject response) {
        return new ContractNegotiation(processJsonLd(response,null));
    }

    public static ContractAgreement processContractAgreement(String response) {
        return processContractAgreement(Json.createReader(new StringReader(response)).readObject());
    }

    public static ContractAgreement processContractAgreement(JsonObject response) {
        return new ContractAgreement(processJsonLd(response,null));
    }

    public static TransferProcess processTransferProcess(String response) {
        return processTransferProcess(Json.createReader(new StringReader(response)).readObject());
    }

    public static TransferProcess processTransferProcess(JsonObject response) {
        return new TransferProcess(processJsonLd(response,null));
    }

    public static List<Asset> processAssetList(String response) {
        return processAssetList(Json.createReader(new StringReader(response)).readArray());
    }
    public static List<Asset> processAssetList(JsonArray response) {
        return response.stream().map( responseObject ->
                new Asset(processJsonLd(responseObject.asJsonObject(),null))
        ).collect(Collectors.toList());
    }

    public static String asString(JsonValue value) {
        if(value==null) {
            return "null";
        }
        switch(value.getValueType())  {
            case STRING:
                return ((JsonString) value).getString();
            case NUMBER:
                return ((JsonNumber) value).numberValue().toString();
            default:
                return value.toString();
        }
    }

    public static <JsonType extends JsonValue> JsonType processJsonLd(JsonType source, Map<String,String> context) {
        switch (source.getValueType()) {
            case ARRAY:
                final JsonArrayBuilder array = Json.createArrayBuilder();
                source.asJsonArray().forEach(value -> array.add(processJsonLd(value, context)));
                return (JsonType) array.build();
            case OBJECT:
                JsonObject sourceObject = source.asJsonObject();
                Map<String, String> namespaces = new HashMap<>();
                if (context != null) {
                    namespaces.putAll(context);
                }
                if (sourceObject.containsKey("@context")) {
                    for (Map.Entry<String, JsonValue> ns : sourceObject.getJsonObject("@context").entrySet()) {
                        namespaces.put(ns.getKey(), JsonLd.asString(ns.getValue()));
                    }
                }
                final JsonObjectBuilder object = Json.createObjectBuilder();
                sourceObject.forEach((prop, value) -> {
                    int colonIndex = prop.indexOf(":");
                    if (colonIndex > 0) {
                        String prefix = prop.substring(0, colonIndex);
                        if (namespaces.containsKey(prefix)) {
                            prefix = namespaces.get(prefix);
                        } else {
                            prefix = prefix + ":";
                        }
                        prop = prefix + prop.substring(colonIndex + 1);
                    }
                    object.add(prop, processJsonLd(value, namespaces));
                });
                return (JsonType) object.build();
            default:
                return source;
        }
    }

}
