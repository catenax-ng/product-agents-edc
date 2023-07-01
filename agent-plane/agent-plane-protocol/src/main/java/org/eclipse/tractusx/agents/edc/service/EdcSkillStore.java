//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
package org.eclipse.tractusx.agents.edc.service;

import com.fasterxml.jackson.databind.node.TextNode;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.ISkillStore;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLd;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Implements a skill store based on EDC assets
 */
public class EdcSkillStore implements ISkillStore {

    DataManagement management;
    TypeManager typeManager;
    AgentConfig config;

    public EdcSkillStore(DataManagement management, TypeManager typeManager, AgentConfig config) {
        this.management=management;
        this.typeManager=typeManager;
        this.config=config;
    }

    @Override
    public boolean isSkill(String key) {
        return ISkillStore.matchSkill(key).matches();
    }

    @Override
    public String put(String key, String skill) {
        try {
            return management.createOrUpdateSkill(
                    key,
                    "No name given",
                    "No description given",
                    "unknown version",
                    config.getDefaultSkillContract(),
                    "<https://w3id.org/catenax/ontology/core>",
                    "all",
                    true,
                    typeManager.getMapper().writeValueAsString(TextNode.valueOf(skill))
            ).getId();
        } catch (IOException e) {
           return null;
        }
    }

    @Override
    public Optional<String> get(String key) {
        QuerySpec findAsset=QuerySpec.Builder.newInstance().filter(
                List.of(new Criterion("https://w3id.org/edc/v0.0.1/ns/id","=",key),
                        new Criterion("http://www.w3.org/1999/02/22-rdf-syntax-ns#type","=","cx-common:SkillAsset"))).build();
        try {
            return management.listAssets(findAsset).stream().findFirst().map( asset->
                    JsonLd.asString(asset.getPrivateProperties().get("https://w3id.org/catenax/ontology/common#query"))
            );
        } catch(IOException e) {
            return Optional.empty();
        }
    }
}
