//
// EDC Control Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc;

import org.eclipse.dataspaceconnector.spi.message.RemoteMessageDispatcherRegistry;
import org.eclipse.dataspaceconnector.transfer.dataplane.spi.proxy.DataPlaneTransferProxyReferenceService;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.flow.ProviderDataPlaneProxyDataFlowController;
import org.eclipse.dataspaceconnector.transfer.dataplane.sync.proxy.DataPlaneTransferProxyResolver;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataRequest;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

/**
 * Subclass of provider data plane proxy data flow
 * in order to be able to deploy "HttpProcols" alongside "HttpProxy"
 */
public class HttpProviderProxyDataFlowController extends ProviderDataPlaneProxyDataFlowController {
    
    /**
     * constructor
     * @param connectorId of the source connector
     * @param proxyResolver helping to find sinks and sources
     * @param dispatcherRegistry where to register dispatch messages
     * @param proxyReferenceService helping to build proxy reference addresses
     */
    public HttpProviderProxyDataFlowController(String connectorId,
                                               DataPlaneTransferProxyResolver proxyResolver,
                                               RemoteMessageDispatcherRegistry dispatcherRegistry,
                                               DataPlaneTransferProxyReferenceService proxyReferenceService) {
        super(connectorId,proxyResolver,dispatcherRegistry,proxyReferenceService);
    }

    /**
     * only handle the parallel http-protocols sync transfer not
     * to collide with the usual http-proxy sync.
     */
    @Override
    public boolean canHandle(DataRequest dataRequest, DataAddress contentAddress) {
        return HttpProtocolsConstants.TRANSFER_TYPE.equals(dataRequest.getDestinationType());
    }
    
}
