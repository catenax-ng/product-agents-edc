// Copyright (c) 2022,2023 Contributors to the Eclipse Foundation
//
// See the NOTICE file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// This program and the accompanying materials are made available under the
// terms of the Apache License, Version 2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0.
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
// WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
// License for the specific language governing permissions and limitations
// under the License.
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.tractusx.agents.edc;

import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.system.configuration.Config;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * typed wrapper around the
 * EDC configuration
 */
public class AgentConfig {

    public static String DEFAULT_ASSET_PROPERTY = "cx.agent.asset.default";
    public static String DEFAULT_ASSET_NAME = "urn:x-arq:DefaultGraph";

    public static String ASSET_FILE_PROPERTY = "cx.agent.asset.file";

    public static String ACCESS_POINT_PROPERTY = "cx.agent.accesspoint.name";
    public static String DEFAULT_ACCESS_POINT = "api";

    public static String VERBOSE_PROPERTY = "cx.agent.sparql.verbose";
    public static boolean DEFAULT_VERBOSE_PROPERTY = false;

    public static String CONTROL_PLANE_MANAGEMENT_PROVIDER = "cx.agent.controlplane.management.provider";
    public static String CONTROL_PLANE_MANAGEMENT = "cx.agent.controlplane.management";
    public static String CONTROL_PLANE_IDS = "cx.agent.controlplane.protocol";

    public static String BUSINESS_PARTNER_NUMBER = "edc.participant.id";
    public static String CONTROL_PLANE_AUTH_HEADER = "edc.api.auth.header";
    public static String CONTROL_PLANE_AUTH_VALUE = "edc.api.auth.key";

    public static String NEGOTIATION_TIMEOUT_PROPERTY = "cx.agent.negotiation.timeout";
    public static long DEFAULT_NEGOTIATION_TIMEOUT = 30000;
    
    public static String NEGOTIATION_POLLINTERVAL_PROPERTY = "cx.agent.negotiation.poll";
    public static long DEFAULT_NEGOTIATION_POLLINTERVAL = 1000;
    
    public static String DATASPACE_SYNCINTERVAL_PROPERTY = "cx.agent.dataspace.synchronization";
    public static long DEFAULT_DATASPACE_SYNCINTERVAL = -1;
    
    public static String DATASPACE_SYNCCONNECTORS_PROPERTY = "cx.agent.dataspace.remotes";
    
    public static String VALIDATION_ENDPOINTS = "edc.dataplane.token.validation.endpoints";
    
    public static String FEDERATION_SERVICE_BATCH_SIZE = "cx.agent.federation.batch.max";
    public static long DEFAULT_FEDERATION_SERVICE_BATCH_SIZE = Long.MAX_VALUE;
    
    public static String THREAD_POOL_SIZE = "cx.agent.threadpool.size";
    public static int DEFAULT_THREAD_POOL_SIZE = 4;

    public static String CONNECT_TIMEOUT_PROPERTY = "cx.agent.connect.timeout";
    public static String WRITE_TIMEOUT_PROPERTY = "cx.agent.write.timeout";
    public static String CALL_TIMEOUT_PROPERTY = "cx.agent.call.timeout";
    public static String READ_TIMEOUT_PROPERTY = "cx.agent.read.timeout";
    public static int DEFAULT_READ_TIMEOUT=1080000;

    public static String CALLBACK_ENDPOINT="cx.agent.callback";

    public static String DEFAULT_SKILL_CONTRACT_PROPERTY = "cx.agent.skill.contract.default";

    public static String SERVICE_ALLOW_PROPERTY = "cx.agent.service.allow";
    public static String DEFAULT_SERVICE_ALLOW_PATTERN = "(http|edc)s?://.*";

    public static String SERVICE_DENY_PROPERTY = "cx.agent.service.deny";
    public static String DEFAULT_SERVICE_DENY_PATTERN = "^$";

    public static String SERVICE_ALLOW_ASSET_PROPERTY = "cx.agent.service.asset.allow";
    public static String DEFAULT_SERVICE_ALLOW_ASSET_PATTERN = "(http|edc)s://.*";

    public static String SERVICE_DENY_ASSET_PROPERTY = "cx.agent.service.asset.deny";
    public static String DEFAULT_SERVICE_DENY_ASSET_PATTERN = "^$";

    /**
     * precompiled stuff
     */
    protected final Pattern serviceAllowPattern;
    protected final Pattern serviceDenyPattern;
    protected final Pattern serviceAssetAllowPattern;
    protected final Pattern serviceAssetDenyPattern;
    
    /**
     * references to EDC services
     */
    protected final Config config;
    protected final Monitor monitor;

    /**
     * creates the typed config
     * @param monitor logger
     * @param config untyped config
     */
    public AgentConfig(Monitor monitor, Config config) {
        this.monitor = monitor;
        this.config = config;
        serviceAllowPattern=Pattern.compile(config.getString(SERVICE_ALLOW_PROPERTY,DEFAULT_SERVICE_ALLOW_PATTERN));
        serviceDenyPattern=Pattern.compile(config.getString(SERVICE_DENY_PROPERTY,DEFAULT_SERVICE_DENY_PATTERN));
        serviceAssetAllowPattern=Pattern.compile(config.getString(SERVICE_ALLOW_ASSET_PROPERTY,DEFAULT_SERVICE_ALLOW_ASSET_PATTERN));
        serviceAssetDenyPattern=Pattern.compile(config.getString(SERVICE_DENY_ASSET_PROPERTY,DEFAULT_SERVICE_DENY_ASSET_PATTERN));
    }

    /**
     * @return callback endpoint
     */
    public String getCallbackEndpoint() {
        return config.getString(CALLBACK_ENDPOINT);
    }

    /**
     * @return the name of the default asset/graph
     */
    public String getDefaultAsset() {
        return config.getString(DEFAULT_ASSET_PROPERTY, DEFAULT_ASSET_NAME);
    }

    public String getBusinessPartnerNumber() {
        return config.getString(BUSINESS_PARTNER_NUMBER, "anonymous");
    }

    /**
     * @return initial file to load
     */
    public String[] getAssetFiles() {
        String[] files= config.getString(ASSET_FILE_PROPERTY,"").split(",");
        if(files.length==1 && (files[0]==null || files[0].length()==0)) {
            return null;
        }
        return files;
    }

    /**
     * @return name of the sparql access point
     */
    public String getAccessPoint() {
        return config.getString(ACCESS_POINT_PROPERTY, DEFAULT_ACCESS_POINT);
    }

    /**
     * @return uri of the control plane management endpoint (without concrete api)
     */
    public String getControlPlaneManagementUrl() {
        return config.getString(CONTROL_PLANE_MANAGEMENT,null);
    }

    /**
     * @return uri of the control plane management endpoint (without concrete api)
     */
    public String getControlPlaneManagementProviderUrl() {
        return config.getString(CONTROL_PLANE_MANAGEMENT_PROVIDER,config.getString(CONTROL_PLANE_MANAGEMENT,null));
    }

    /**
     * @return uri of the control plane ids endpoint (without concrete api)
     */
    public String getControlPlaneIdsUrl() {
        return config.getString(CONTROL_PLANE_IDS,null);
    }

    /**
     * @return a map of key/value paris to be used when interacting with the control plane management endpoint
     */
    public Map<String, String> getControlPlaneManagementHeaders() {
        String key = config.getString(CONTROL_PLANE_AUTH_HEADER,"X-Api-Key");
        String value = config.getString(CONTROL_PLANE_AUTH_VALUE,null);
        if (key != null && value != null) {
            return Map.of(key, value);
        }
        return Map.of();
    }

    /**
     * @return the default overall timeout when waiting for a negotation result
     */
    public long getNegotiationTimeout() {
        return config.getLong(NEGOTIATION_TIMEOUT_PROPERTY,DEFAULT_NEGOTIATION_TIMEOUT);
    }

    /**
     * @return the thread pool size of the agent executors
     */
    public int getThreadPoolSize() {
        return config.getInteger(THREAD_POOL_SIZE,DEFAULT_THREAD_POOL_SIZE);
    }

    /**
     * @return the default overall timeout when waiting for a negotation result
     */
    public long getNegotiationPollInterval() {
        return config.getLong(NEGOTIATION_POLLINTERVAL_PROPERTY,DEFAULT_NEGOTIATION_POLLINTERVAL);
    }

    /**
     * @return the synchronization interval between individual sync calls, -1 if no sync
     */
    public long getDataspaceSynchronizationInterval() {
        return config.getLong(DATASPACE_SYNCINTERVAL_PROPERTY,DEFAULT_DATASPACE_SYNCINTERVAL);
    }

    /**
     * @return array of connector urls to synchronize, null if no sync
     */
    public String[] getDataspaceSynchronizationConnectors() {
        String[] connectors= config.getString(DATASPACE_SYNCCONNECTORS_PROPERTY,"").split(",");
        if(connectors.length==1 && (connectors[0]==null || connectors[0].length()==0)) {
            return null;
        }
        return connectors;
    }

    /**
     * @return array of validation endpoints
     */
    public String[] getValidatorEndpoints() {
        return config.getConfig(VALIDATION_ENDPOINTS).getEntries().values().toArray(new String[0]);
    }

    /**
     * @return whether sparql engine is set to verbose
     */
    public boolean isSparqlVerbose() {
        return config.getBoolean(VERBOSE_PROPERTY,DEFAULT_VERBOSE_PROPERTY);
    }

    /**
     * @return maximal batch size for remote service calls
     */
    public long getFederationServiceBatchSize() {
        return config.getLong(FEDERATION_SERVICE_BATCH_SIZE,DEFAULT_FEDERATION_SERVICE_BATCH_SIZE);
    }

    /**
     * @return outgoing socket connect timeout
     */
    public Integer getConnectTimeout() {
        return config.getInteger(CONNECT_TIMEOUT_PROPERTY,null);
    }

    /**
     * @return outgoing socket read timeout
     */
    public Integer getReadTimeout() {
        return config.getInteger(READ_TIMEOUT_PROPERTY,DEFAULT_READ_TIMEOUT);
    }

    /**
     * @return outgoing socket write timeout
     */
    public Integer getWriteTimeout() {
        return config.getInteger(WRITE_TIMEOUT_PROPERTY,null);
    }

    /**
     * @return outgoing socket write timeout
     */
    public Integer getCallTimeout() {
        return config.getInteger(CALL_TIMEOUT_PROPERTY,null);
    }

    /**
     * @return default skill contract
     */
    public String getDefaultSkillContract() {
        return config.getString(DEFAULT_SKILL_CONTRACT_PROPERTY,null);
    }

    /**
     * @return regular expression for allowed service URLs
     */
    public Pattern getServiceAllowPattern() {
        return serviceAllowPattern;
    }

    /**
     * @return regular expression for denied service URLs
     */
    public Pattern getServiceDenyPattern() {
        return serviceDenyPattern;
    }

        /**
     * @return regular expression for allowed service URLs in assets
     */
    public Pattern getServiceAssetAllowPattern() {
        return serviceAssetAllowPattern;
    }

    /**
     * @return regular expression for denied service URLs in assets
     */
    public Pattern getServiceAssetDenyPattern() {
        return serviceAssetDenyPattern;
    }


}
