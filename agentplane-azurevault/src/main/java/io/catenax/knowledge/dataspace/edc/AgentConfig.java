package io.catenax.knowledge.dataspace.edc;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.system.configuration.Config;

public class AgentConfig {

    public static String DEFAULT_ASSET_PROPERTY = "cx.agent.asset.default";
    public static String DEFAULT_ASSET_NAME = "urn:skill:cx:Query";

    Config config;
    Monitor monitor;

    public AgentConfig(Monitor monitor, Config config) {
        this.monitor=monitor;
        this.config=config;
    }

    public String getDefaultAsset() {
        return config.getString(DEFAULT_ASSET_PROPERTY,DEFAULT_ASSET_NAME);
    }
}
