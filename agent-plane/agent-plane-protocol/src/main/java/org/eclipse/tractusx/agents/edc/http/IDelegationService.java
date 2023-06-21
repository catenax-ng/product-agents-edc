package org.eclipse.tractusx.agents.edc.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

/**
 * interface to a service that may
 * delegate agent http calls into the
 * dataspace
 */
public interface IDelegationService {
    /**
     * delegate the given call into the dataspace
     * @param remoteUrl target EDC
     * @param skill name of the remote skill (may be empty, then graph must be set)
     * @param graph name of the remote graph (may be empty, then skill must be set)
     * @param headers url call headers
     * @param request url request
     * @param response final response
     * @param uri original uri
     * @return an intermediate response (the actual result will be put into the final response state)
     */
    Response executeQueryRemote(String remoteUrl, String skill, String graph, HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, UriInfo uri);
}
