//
// EDC Data Plane Agent Extension Test
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc;

import java.util.Optional;
import java.util.regex.Matcher;

/**
 * interface to a skill store
 */
public interface ISkillStore {

    /**
     * match a given asset
     * @param key asset name
     * @return matcher
     */
    static Matcher matchSkill(String key) {
        return AgentExtension.SKILL_PATTERN.matcher(key);
    }

    /**
     * check a given asset for being a skill
     * @param key asset name
     * @return whether the asset encodes a skill
     */
    boolean isSkill(String key);

    /**
     * register a skill
     * @param key asset name required
     * @param skill query text required
     * @param name of skill optional
     * @param description of skill optional
     * @param version of skill optional
     * @param contract of skill optional
     * @param dist of skill required
     * @param isFederated whether skill maybe synchronized in catalogue
     * @param ontologies a set of ontologies
     * @return skill id
     */
    String put(String key, String skill, String name, String description, String version, String contract, SkillDistribution dist, boolean isFederated, String... ontologies);

    /**
     * return the skill distribution
     * @param key asset name
     * @return skill distribution mode
     */
    SkillDistribution getDistribution(String key);

    /**
     * return the stored skill text
     * @param key asset name
     * @return optional skill text if registered
     */
    Optional<String> get(String key);
}
