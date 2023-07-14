//
// EDC Data Plane Agent Extension Test
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc;

/**
 * enumerates the various skill distribution/run modes
 */
public enum SkillDistribution {
    CONSUMER("consumer"),
    PROVIDER("provider"),
    ALL("all");

    private final String mode;

    /**
     * @param mode the textual mode
     */
    SkillDistribution(final String mode) {
        this.mode = mode;
    }

    /**
     * @return mode a semantic value
     */
    public String getDistributionMode() {
        return "cx-common:SkillDistribution?run="+this.mode;
    }

    /**
     * @return mode as argument
     */
    public String getMode() {
        return this.mode;
    }

    /**
     * @param mode as argument
     * @return respective enum (or ALL if it does not fir)
     */
    public static SkillDistribution valueOfMode(String mode) {
        if(mode!=null) {
            if (mode.endsWith("consumer"))
                return CONSUMER;
            if (mode.endsWith("provider"))
                return PROVIDER;
        }
        return ALL;
    }

}
