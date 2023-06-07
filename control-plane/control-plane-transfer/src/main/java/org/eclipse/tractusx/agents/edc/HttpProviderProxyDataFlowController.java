//
// EDC Control Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc;

import org.eclipse.edc.connector.transfer.dataplane.flow.ConsumerPullTransferDataFlowController;
import org.eclipse.edc.connector.transfer.dataplane.proxy.ConsumerPullTransferProxyResolver;
import org.eclipse.edc.connector.transfer.dataplane.spi.proxy.ConsumerPullTransferEndpointDataReferenceService;
import org.eclipse.edc.connector.transfer.spi.types.DataRequest;
import org.eclipse.edc.spi.types.domain.DataAddress;

/**
 * Subclass of provider data plane proxy data flow
 * in order to be able to deploy "HttpProcols" alongside "HttpProxy"
 */
public class HttpProviderProxyDataFlowController extends ConsumerPullTransferDataFlowController {
    
    /**
     * constructor
     * @param proxyResolver helping to find sinks and sources
     * @param proxyReferenceService helping to build proxy reference addresses
     */
    public HttpProviderProxyDataFlowController(ConsumerPullTransferProxyResolver proxyResolver,
                                               ConsumerPullTransferEndpointDataReferenceService proxyReferenceService) {
        super(proxyResolver,proxyReferenceService);
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
