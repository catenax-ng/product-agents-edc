//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import jakarta.ws.rs.InternalServerErrorException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.tractusx.agents.edc.jsonld.JsonLd;
import org.eclipse.tractusx.agents.edc.model.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.String.format;

/**
 * DataManagement
 * is a service wrapper around the management endpoint
 * of the EDC control plane
 */
public class DataManagement {
    /**
     * some constants when interacting with control plane
     */
    public static final String DSP_PATH="%s/api/v1/dsp";
    public static final String CATALOG_CALL = "%s/catalog/request";
    public static final String CATALOG_REQUEST_BODY="{" +
            "\"@context\": {}," +
            "\"protocol\": \"dataspace-protocol-http\"," +
            "\"providerUrl\": \"%s\", " +
            "\"querySpec\": %s }";

    public static final String NEGOTIATION_REQUEST_BODY="{\n" +
            "\"@context\": { \"odrl\": \"http://www.w3.org/ns/odrl/2/\"},\n" +
            "\"@type\": \"NegotiationInitiateRequestDto\",\n" +
            "\"connectorAddress\": \"%1$s\",\n" +
            "\"protocol\": \"dataspace-protocol-http\",\n" +
            "\"providerId\": \"%2$s\",\n" +
            "\"connectorId\": \"%3$s\",\n" +
            "\"offer\": {\n" +
            "  \"offerId\": \"%4$s\",\n" +
            "  \"assetId\": \"%5$s\",\n" +
            "  \"policy\": %6$s\n" +
            "}\n" +
            "}";

    public static final String NEGOTIATION_INITIATE_CALL = "%s/contractnegotiations";
    public static final String NEGOTIATION_CHECK_CALL = "%s/contractnegotiations/%s";
    public static final String TRANSFER_INITIATE_CALL = "%s/transferprocesses";

    public static final String TRANSFER_REQUEST_BODY="{\n" +
            "    \"@context\": {\n" +
            "        \"odrl\": \"http://www.w3.org/ns/odrl/2/\"\n" +
            "    },\n" +
            "    \"assetId\": \"%1$s\",\n" +
            "    \"connectorAddress\": \"%2$s\",\n" +
            "    \"contractId\": \"%3$s\",\n" +
            "    \"dataDestination\": {\n" +
            "        \"properties\": {\n" +
            "            \"type\": \"HttpProxy\"\n" +
            "        }\n" +
            "    },\n" +
            "    \"managedResources\": false,\n" +
            "    \"privateProperties\": {\n" +
            "        \"receiverHttpEndpoint\": \"%4$s\"\n" +
            "    },\n" +
            "    \"protocol\": \"dataspace-protocol-http\",\n" +
            "    \"transferType\": {\n" +
            "        \"contentType\": \"application/octet-stream\",\n" +
            "        \"isFinite\": true\n" +
            "    }\n" +
            "}";
    public static final String TRANSFER_CHECK_CALL = "%s/transferprocesses/%s";
    public static final String AGREEMENT_CHECK_CALL = "%s/contractagreements/%s";

    /**
     * references to EDC services
     */
    private final Monitor monitor;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final AgentConfig config;

    /**
     * creates a service wrapper
     * @param monitor logger
     * @param typeManager serialization
     * @param httpClient remoting
     * @param config typed config
     */
    public DataManagement(Monitor monitor, TypeManager typeManager, OkHttpClient httpClient, AgentConfig config) {
        this.monitor = monitor;
        this.objectMapper = typeManager.getMapper();
        this.httpClient = httpClient;
        this.config=config;
    }

    /**
     * Search for a dedicated asset
     * TODO imperformant
     * TODO replace by accessing the federated data catalogue
     * @param remoteControlPlaneIdsUrl url of the remote control plane ids endpoint
     * @param assetId (connector-unique) identifier of the asset
     * @return a collection of contract options to access the given asset
     * @throws IOException in case that the remote call did not succeed
     */
    public DcatCatalog findContractOffers(String remoteControlPlaneIdsUrl, String assetId) throws IOException {
        QuerySpec findAsset=QuerySpec.Builder.newInstance().filter(
                List.of(new Criterion("https://w3id.org/edc/v0.0.1/ns/id","=",assetId))
        ).build();
        return getCatalog(remoteControlPlaneIdsUrl,findAsset);
    }

    /**
     * Access the catalogue
     * @param remoteControlPlaneIdsUrl url of the remote control plane ids endpoint
     * @param spec query specification
     * @return catalog object
     * @throws IOException in case something went wrong
     */
    public DcatCatalog getCatalog(String remoteControlPlaneIdsUrl, QuerySpec spec) throws IOException {

        var url = String.format(CATALOG_CALL,config.getControlPlaneManagementUrl());
        var catalogSpec =String.format(CATALOG_REQUEST_BODY,String.format(DSP_PATH,remoteControlPlaneIdsUrl),objectMapper.writeValueAsString(spec));

        var request = new Request.Builder().url(url).post(RequestBody.create(catalogSpec,MediaType.parse("application/json")));
        config.getControlPlaneManagementHeaders().forEach(request::addHeader);

        try (var response = httpClient.newCall(request.build()).execute()) {
            var body = response.body();

            if (!response.isSuccessful() || body == null) {
                throw new InternalServerErrorException(format("Control plane responded with: %s %s", response.code(), body != null ? body.string() : ""));
            }

            return JsonLd.processCatalog(body.string());
        } catch (Exception e) {
            monitor.severe(format("Error in calling the control plane at %s", url), e);
            throw e;
        }
    }

    /**
     * initiates negotation
     * @param negotiationRequest outgoing request
     * @return negotiation id
     * @throws IOException in case something went wronf
     */
    public String initiateNegotiation(ContractNegotiationRequest negotiationRequest) throws IOException {
        var url = String.format(NEGOTIATION_INITIATE_CALL,config.getControlPlaneManagementUrl());

        var negotiateSpec =String.format(NEGOTIATION_REQUEST_BODY,
                negotiationRequest.getConnectorAddress(),
                negotiationRequest.getLocalBusinessPartnerNumber(),
                negotiationRequest.getRemoteBusinessPartnerNumber(),
                negotiationRequest.getOffer().getOfferId(),
                negotiationRequest.getOffer().getAssetId(),
                negotiationRequest.getOffer().getPolicy().asString());

        var requestBody = RequestBody.create(negotiateSpec,MediaType.parse("application/json"));

        var request = new Request.Builder()
                .url(url)
                .post(requestBody);
        config.getControlPlaneManagementHeaders().forEach(request::addHeader);

        try (var response = httpClient.newCall(request.build()).execute()) {
            var body = response.body();

            if (!response.isSuccessful() || body == null) {
                throw new InternalServerErrorException(format("Control plane responded with: %s %s", response.code(), body != null ? body.string() : ""));
            }


            var negotiationId = JsonLd.processIdResponse(body.string()).getId();

            monitor.debug("Started negotiation with ID: " + negotiationId);

            return negotiationId;
        } catch (Exception e) {
            monitor.severe(format("Error in calling the control plane at %s", url), e);
            throw e;
        }
    }

    /**
     * return state of contract negotiation
     * @param negotiationId id of the negotation to inbestigate
     * @return status of the negotiation
     * @throws IOException in case something went wrong
     */
    public ContractNegotiation getNegotiation(String negotiationId) throws IOException {
        var url = String.format(NEGOTIATION_CHECK_CALL,config.getControlPlaneManagementUrl(),negotiationId);
        var request = new Request.Builder()
                .url(url);
        config.getControlPlaneManagementHeaders().forEach(request::addHeader);

        try (var response = httpClient.newCall(request.build()).execute()) {
            var body = response.body();

            if (!response.isSuccessful() || body == null) {
                throw new InternalServerErrorException(format("Control plane responded with: %s %s", response.code(), body != null ? body.string() : ""));
            }

            var negotiation = JsonLd.processContractNegotiation(body.string());
            monitor.debug(format("Negotiation %s is in state '%s' (agreementId: %s)", negotiationId, negotiation.getState(), negotiation.getContractAgreementId()));

            return negotiation;
        } catch (Exception e) {
            monitor.severe(format("Error in calling the Control plane at %s", url), e);
            throw e;
        }
    }

    /**
     * @param agreementId id of the agreement
     * @return contract agreement
     * @throws IOException something wild happens
     */
    public ContractAgreement getAgreement(String agreementId) throws IOException {
        var url = String.format(AGREEMENT_CHECK_CALL,config.getControlPlaneManagementUrl(), URLEncoder.encode(agreementId, StandardCharsets.UTF_8));
        var request = new Request.Builder()
                .url(url);
        config.getControlPlaneManagementHeaders().forEach(request::addHeader);

        try (var response = httpClient.newCall(request.build()).execute()) {
            var body = response.body();

            if (!response.isSuccessful() || body == null) {
                throw new InternalServerErrorException(format("Control plane responded with: %s %s", response.code(), body != null ? body.string() : ""));
            }

            var agreement = JsonLd.processContractAgreement(body.string());
            monitor.debug(format("Agreement %s found for asset %s", agreementId, agreement.getAssetId()));

            return agreement;
        } catch (Exception e) {
            monitor.severe(format("Error in calling the Control plane at %s", url), e);
            throw e;
        }
    }

    /**
     * Initiates a transfer
     * @param transferRequest request
     * @return transfer id
     * @throws IOException in case something went wrong
     */
    public String initiateHttpProxyTransferProcess(TransferRequest transferRequest) throws IOException {
        var url = String.format(TRANSFER_INITIATE_CALL,config.getControlPlaneManagementUrl());

        var transferSpec =String.format(TRANSFER_REQUEST_BODY,
                transferRequest.getAssetId(),
                transferRequest.getConnectorAddress(),
                transferRequest.getContractId(),
                transferRequest.getCallbackAddresses().get(0).getUri());

        var requestBody = RequestBody.create(transferSpec,MediaType.parse("application/json"));

        var request = new Request.Builder()
                .url(url)
                .post(requestBody);
        config.getControlPlaneManagementHeaders().forEach(request::addHeader);

        try (var response = httpClient.newCall(request.build()).execute()) {
            var body = response.body();

            if (!response.isSuccessful() || body == null) {
                throw new InternalServerErrorException(format("Control plane responded with: %s %s", response.code(), body != null ? body.string() : ""));
            }

            // For debugging purposes:
            // var transferProcessId = TransferId.Builder.newInstance().id(body.string()).build();
            var transferProcessId = JsonLd.processIdResponse(body.string()).getId();

            monitor.debug(format("Transfer process (%s) initiated", transferProcessId));

            return transferProcessId;
        } catch (Exception e) {
            monitor.severe(format("Error in calling the control plane at %s", url), e);
            throw e;
        }
    }

    /**
     * return state of transfer process
     * @param transferProcessId id of the transfer process
     * @return state of the transfer process
     * @throws IOException in case something went wrong
     */
    public TransferProcess getTransfer(String transferProcessId) throws IOException {
        var url = String.format(TRANSFER_CHECK_CALL,config.getControlPlaneManagementUrl(),transferProcessId);
        var request = new Request.Builder()
                .url(url);
        config.getControlPlaneManagementHeaders().forEach(request::addHeader);

        try (var response = httpClient.newCall(request.build()).execute()) {
            var body = response.body();

            if (!response.isSuccessful() || body == null) {
                throw new InternalServerErrorException(format("Control plane responded with: %s %s", response.code(), body != null ? body.string() : ""));
            }

            var process = JsonLd.processTransferProcess(body.string());
            monitor.info(format("Transfer %s is in state '%s'", transferProcessId, process.getState()));

            return process;
        } catch (Exception e) {
            monitor.severe(format("Error in calling the Control plane at %s", url), e);
            throw e;
        }
    }

}