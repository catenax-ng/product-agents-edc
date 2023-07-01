//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.service;

import org.eclipse.tractusx.agents.edc.ISkillStore;

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

    /**
     * check a given asset for being a skill
     * @param key asset name
     * @return whether the asset encodes a skill
     */
    public boolean isSkill(String key) {
        return ISkillStore.matchSkill(key).matches();
    }

    /**
     * register a skill
     * @param key asset name
     * @param skill skill text
     * @return old skill text, if one was registered
     */
    public String put(String key, String skill) {
        return skills.put(key,skill);
    }

    /**
     * return the stored skill text
     * @param key asset name
     * @return optional skill text if registered
     */
    public Optional<String> get(String key) {
        if(!skills.containsKey(key)) {
            return Optional.empty();
        } else {
            return Optional.of(skills.get(key));
        }
    }
}
