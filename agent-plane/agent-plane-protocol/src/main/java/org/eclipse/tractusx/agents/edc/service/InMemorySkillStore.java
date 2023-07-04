//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.service;

import org.eclipse.tractusx.agents.edc.ISkillStore;
import org.eclipse.tractusx.agents.edc.SkillDistribution;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory store for local skills
 */
public class InMemorySkillStore implements ISkillStore {

    // temporary local skill store
    final protected Map<String,String> skills=new HashMap<>();

    /**
     * create the store
     */
    public InMemorySkillStore() {
    }

    @Override
    public boolean isSkill(String key) {
        return ISkillStore.matchSkill(key).matches();
    }

    @Override
    public String put(String key, String skill, String name, String description, String version, String contract, SkillDistribution dist, boolean isFederated, String... ontologies) {
        skills.put(key,skill);
        return key;
    }

    @Override
    public Optional<String> get(String key) {
        if(!skills.containsKey(key)) {
            return Optional.empty();
        } else {
            return Optional.of(skills.get(key));
        }
    }
}
