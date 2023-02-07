//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc.http;

import io.catenax.knowledge.dataspace.edc.AgentConfig;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * Extended Http Client Factory which has configurable timeouts
 */
public class HttpClientFactory {

    /**
     * Create an OkHttpClient instance
     *
     * @param config             agent config
     * @return the OkHttpClient
     */
    @NotNull
    public static OkHttpClient create(AgentConfig config) {
        var builder = new OkHttpClient.Builder()
                .connectTimeout(config.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(config.getReadTimeout(), TimeUnit.MILLISECONDS);
        return builder.build();
    }

}
