//
// EDC Data Plane Agent Extension 
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc;

import com.nimbusds.jose.JWSObject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.edc.policy.model.Action;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;
import org.eclipse.edc.connector.transfer.spi.types.TransferType;
import org.eclipse.tractusx.agents.edc.service.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;


/**
 * An endpoint/service that receives information from the control plane
 */
@Consumes({MediaType.APPLICATION_JSON})
@Path("/endpoint-data-reference")
public class AgreementController implements IAgreementController {

    /**
     * which transfer to use
     */
    public static String TRANSFER_TYPE="HttpProtocol";

    /**
     * EDC service references
     */
    protected final Monitor monitor;
    protected final DataManagement dataManagement;
    protected final AgentConfig config;

    /**
     * memory store for links from assets to the actual transfer addresses
     * TODO make this a distributed cache
     * TODO let this cache evict invalidate references automatically
     */
    // hosts all pending processes
    protected final Set<String> activeAssets = new HashSet<>();
    // any contract agreements indexed by asset
    protected final Map<String, ContractAgreement> agreementStore = new HashMap<>();
    // any transfer processes indexed by asset, the current process should
    // always adhere to the above agreement
    protected final Map<String, TransferProcess> processStore = new HashMap<>();
    // at the end of provisioning and endpoint reference will be set
    // that fits to the current transfer process
    protected final Map<String, EndpointDataReference> endpointStore = new HashMap<>();

    /**
     * creates an agreement controller
     *
     * @param monitor        logger
     * @param config         typed config
     * @param dataManagement data management service wrapper
     */
    public AgreementController(Monitor monitor, AgentConfig config, DataManagement dataManagement) {
        this.monitor = monitor;
        this.dataManagement = dataManagement;
        this.config = config;
    }

    /**
     * render nicely
     */
    @Override
    public String toString() {
        return super.toString() + "/endpoint-data-reference";
    }

    /**
     * this is called by the control plane when an agreement has been made
     *
     * @param dataReference contains the actual call token
     */
    @POST
    public void receiveEdcCallback(EndpointDataReference dataReference) {
        var agreementId = dataReference.getProperties().get("cid");
        monitor.debug(String.format("An endpoint data reference for agreement %s has been posted.", agreementId));
        synchronized (agreementStore) {
            for (Map.Entry<String, ContractAgreement> agreement : agreementStore.entrySet()) {
                if (agreement.getValue().getId().equals(agreementId)) {
                    synchronized (endpointStore) {
                        monitor.debug(String.format("Agreement %s belongs to asset %s.", agreementId, agreement.getKey()));
                        endpointStore.put(agreement.getKey(), dataReference);
                        return;
                    }
                }
            }
        }
        monitor.debug(String.format("Agreement %s has no active asset. Guess that came for another plane. Ignoring.", agreementId));
    }

    /**
     * accesses an active endpoint for the given asset
     *
     * @param assetId id of the agreed asset
     * @return endpoint found, null if not found or invalid
     */
    @Override
    public EndpointDataReference get(String assetId) {
        synchronized (activeAssets) {
            if (!activeAssets.contains(assetId)) {
                monitor.debug(String.format("Asset %s is not active", assetId));
                return null;
            }
            synchronized (endpointStore) {
                EndpointDataReference result = endpointStore.get(assetId);
                if (result != null) {
                    String token = result.getAuthCode();
                    if (token != null) {
                        try {
                            JWSObject jwt = JWSObject.parse(token);
                            Object expiryObject=jwt.getPayload().toJSONObject().get("exp");
                            if(expiryObject instanceof Long) {
                                // token times are in seconds
                                if(!new Date((Long) expiryObject*1000).before(new Date(System.currentTimeMillis() + 30 * 1000))) {
                                    return result;
                                }
                            }
                        } catch(ParseException | NumberFormatException e) {
                            monitor.debug(String.format("Active asset %s has invalid agreement token.", assetId));
                        }
                    }
                    endpointStore.remove(assetId);
                }
                monitor.debug(String.format("Active asset %s has timed out or was not installed.", assetId));
                synchronized (processStore) {
                    processStore.remove(assetId);
                    synchronized (agreementStore) {
                        ContractAgreement agreement = agreementStore.get(assetId);
                        if (agreement != null && agreement.getContractEndDate() <= System.currentTimeMillis()) {
                            agreementStore.remove(assetId);
                        }
                        activeAssets.remove(assetId);
                    }
                }
            }
        }
        return null;
    }

    /**
     * sets active
     * @param asset name
     */
    protected void activate(String asset) {
        synchronized (activeAssets) {
            if (activeAssets.contains(asset)) {
                throw new ClientErrorException("Cannot agree on an already active asset.", Response.Status.CONFLICT);
            }
            activeAssets.add(asset);
        }
    }

    /**
     * sets active
     * @param asset name
     */
    protected void deactivate(String asset) {
        synchronized (activeAssets) {
            activeAssets.remove(asset);
        }
        synchronized (agreementStore) {
            agreementStore.remove(asset);
        }
        synchronized (processStore) {
            processStore.remove(asset);
        }
    }

    /**
     * register an agreement
     * @param asset name
     * @param agreement object
     */
    protected void registerAgreement(String asset, ContractAgreement agreement) {
        synchronized (agreementStore) {
            agreementStore.put(asset, agreement);
        }
    }

    /**
     * register a process
     * @param asset name
     * @param process object
     */
    protected void registerProcess(String asset, TransferProcess process) {
        synchronized (processStore) {
            processStore.put(asset, process);
        }
    }

    /**
     * creates a new agreement (asynchronously)
     * and waits for the result
     *
     * @param remoteUrl ids endpoint url of the remote connector
     * @param asset     name of the asset to agree upon
     * TODO make this federation aware: multiple assets, different policies
     */
    @Override
    public EndpointDataReference createAgreement(String remoteUrl, String asset) throws WebApplicationException {
        monitor.debug(String.format("About to create an agreement for asset %s at connector %s",asset,remoteUrl));

        activate(asset);

        Collection<ContractOffer> contractOffers;

        try {
            contractOffers=dataManagement.findContractOffers(remoteUrl, asset);
        } catch(IOException io) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when resolving contract offers from %s for asset %s through data management api.",remoteUrl,asset),io);
        }

        if (contractOffers.isEmpty()) {
            deactivate(asset);
            throw new BadRequestException(String.format("There is no contract offer in remote connector %s related to asset %s.", remoteUrl, asset));
        }

        // TODO implement a cost-based offer choice
        ContractOffer contractOffer = contractOffers.stream().findFirst().get();
        String assetType=contractOffer.getAsset().getProperties().getOrDefault("rdf:type","<cx_ontology.ttl#Asset>").toString();

        monitor.debug(String.format("About to create an agreement for contract offer %s (for asset %s of type %s at connector %s)",contractOffer.getId(),asset,assetType,remoteUrl));

        // Initiate negotiation
        var policy = Policy.Builder.newInstance()
                .permission(Permission.Builder.newInstance()
                        .target(asset)
                        .action(Action.Builder.newInstance().type("USE").build())
                        .build())
                .type(PolicyType.SET)
                .build();

        var contractOfferDescription = new ContractOfferDescription(
                contractOffer.getId(),
                asset,
                policy
        );
        var contractNegotiationRequest = ContractNegotiationRequest.Builder.newInstance()
                .offerId(contractOfferDescription)
                .connectorId("provider")
                .connectorAddress(String.format(DataManagement.IDS_PATH, remoteUrl))
                .protocol("ids-multipart")
                .build();
        String negotiationId;

        try {
            negotiationId=dataManagement.initiateNegotiation(contractNegotiationRequest);
        } catch(IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when initiating negotation for offer %s through data management api.",contractOffer.getId()),ioe);
        }

        monitor.debug(String.format("About to check negotiation %s for contract offer %s (for asset %s at connector %s)",negotiationId,contractOffer.getId(),asset,remoteUrl));

        // Check negotiation state
        ContractNegotiation negotiation = null;

        long startTime = System.currentTimeMillis();

        try {
            while ((System.currentTimeMillis() - startTime < config.getNegotiationTimeout()) && (negotiation == null || !negotiation.getState().equals("CONFIRMED"))) {
                Thread.sleep(config.getNegotiationPollInterval());
                negotiation = dataManagement.getNegotiation(
                        negotiationId
                );
            }
        } catch (InterruptedException e) {
            monitor.info(String.format("Negotiation thread for asset %s negotiation %s has been interrupted. Giving up.", asset, negotiationId),e);
        } catch(IOException e) {
            monitor.warning(String.format("Negotiation thread for asset %s negotiation %s run into problem. Giving up.", asset, negotiationId),e);
        }

        if (negotiation == null || !negotiation.getState().equals("CONFIRMED")) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Contract Negotiation %s for asset %s was not successful.", negotiationId, asset));
        }

        monitor.debug(String.format("About to check agreement %s for contract offer %s (for asset %s at connector %s)",negotiation.getContractAgreementId(),contractOffer.getId(),asset,remoteUrl));

        ContractAgreement agreement;

        try {
            agreement=dataManagement.getAgreement(negotiation.getContractAgreementId());
        } catch(IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Error when retrieving agreement %s for negotiation %s.",negotiation.getContractAgreementId(),negotiationId),ioe);
        }

        if (agreement == null || !agreement.getAssetId().endsWith(asset)) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Agreement %s does not refer to asset %s.", negotiation.getContractAgreementId(), asset));
        }

        registerAgreement(asset,agreement);

        DataAddress dataDestination = DataAddress.Builder.newInstance()
                .type(TRANSFER_TYPE)
                .build();

        TransferType transferType = TransferType.Builder.
                transferType()
                .contentType("application/octet-stream")
                // TODO make streaming
                .isFinite(true)
                .build();

        TransferRequest transferRequest = TransferRequest.Builder.newInstance()
                .assetId(asset)
                .contractId(agreement.getId())
                .connectorId("provider")
                .connectorAddress(String.format(DataManagement.IDS_PATH, remoteUrl))
                .protocol("ids-multipart")
                .dataDestination(dataDestination)
                .managedResources(false)
                .properties(Map.of("receiver.http.endpoint",config.getCallbackEndpoint()))
                .transferType(transferType)
                .build();

        monitor.debug(String.format("About to initiate transfer for agreement %s (for asset %s at connector %s)",negotiation.getContractAgreementId(),asset,remoteUrl));

        String transferId;

        try {
            transferId=dataManagement.initiateHttpProxyTransferProcess(transferRequest);
        } catch(IOException ioe) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("HttpProxy transfer for agreement %s could not be initiated.", agreement.getId()),ioe);
        }

        monitor.debug(String.format("About to check transfer %s (for asset %s at connector %s)",transferId,asset,remoteUrl));

        // Check negotiation state
        TransferProcess process = null;

        startTime = System.currentTimeMillis();

        try {
            while ((System.currentTimeMillis() - startTime < config.getNegotiationTimeout()) && (process == null || !process.getState().equals("COMPLETED"))) {
                Thread.sleep(config.getNegotiationPollInterval());
                process = dataManagement.getTransfer(
                        transferId
                );
                registerProcess(asset, process);
            }
        } catch (InterruptedException e) {
            monitor.info(String.format("Process thread for asset %s transfer %s has been interrupted. Giving up.", asset, transferId),e);
        } catch(IOException e) {
            monitor.warning(String.format("Process thread for asset %s transfer %s run into problem. Giving up.", asset, transferId),e);
        }

        if (process == null || !process.getState().equals("COMPLETED")) {
            deactivate(asset);
            throw new InternalServerErrorException(String.format("Transfer process %s for agreement %s and asset %s could not be provisioned.", transferId, agreement.getId(), asset));
        }

        // finally wait a bit for the endpoint data reference in case
        // that the process was signalled earlier than the callbacks
        startTime = System.currentTimeMillis();

        EndpointDataReference reference=null;

        try {
            while ((System.currentTimeMillis() - startTime < config.getNegotiationTimeout()) && (reference == null)) {
                Thread.sleep(config.getNegotiationPollInterval());
                synchronized(endpointStore) {
                    reference=endpointStore.get(asset);
                }
            }
        } catch (InterruptedException e) {
            monitor.info(String.format("Wait thread for reference to asset %s has been interrupted. Giving up.", asset),e);
        }

        // mark the type in the endpoint
        if(reference!=null) {
            reference.getProperties().put("rdf:type",assetType);
        }

        // now delegate to the original getter
        return get(asset);
    }

}
