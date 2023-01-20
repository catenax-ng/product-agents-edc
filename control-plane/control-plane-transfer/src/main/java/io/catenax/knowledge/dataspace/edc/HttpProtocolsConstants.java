//
// EDC Control Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc;

/**
 * Some constants used in this extension
 */
public interface HttpProtocolsConstants {

    /**
     * the actual application protocol that is routed via synchronous Http transfer
     */
    String PROTOCOL_ID = "protocol"; 
    
    /**
     * the transfer protocol that performs the synchronous Http transfer
     */
     String TRANSFER_TYPE = "HttpProtocol";
}
