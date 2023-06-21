package org.eclipse.tractusx.agents.edc.model;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLdObject;

import java.util.ArrayList;
import java.util.List;

public class DcatCatalog extends JsonLdObject {

    List<DcatDataset> datasets=new ArrayList<>();

    public DcatCatalog(JsonObject node) {
        super(node);
        JsonValue dataset = node.get("https://www.w3.org/ns/dcat/dataset");
        if(dataset!=null) {
            if(dataset.getValueType()== JsonValue.ValueType.ARRAY) {
                for(JsonValue ds : dataset.asJsonArray()) {
                    datasets.add(new DcatDataset(ds.asJsonObject()));
                }
            } else {
                datasets.add(new DcatDataset(dataset.asJsonObject()));
            }
        }
    }

    public String getParticipantId() {
        return object.getString("https://w3id.org/edc/v0.0.1/ns/participantId","anonymous");
    }

    public List<DcatDataset> getDatasets() {
        return datasets;
    }
}
