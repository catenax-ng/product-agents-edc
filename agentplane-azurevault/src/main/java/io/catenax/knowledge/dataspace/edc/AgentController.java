package io.catenax.knowledge.dataspace.edc;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.QueryParam;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

@Consumes({MediaType.TEXT_PLAIN})
@Path("/agent")
public class AgentController {
    private final Monitor monitor;
    private final AgreementController agreementController;

    /** creates a new agreement controller */
    public AgentController(Monitor monitor, AgreementController agreementController, AgentConfig config) {
        this.monitor = monitor;
        this.agreementController = agreementController;
    }

    /**
     * endpoint for posting a query
     * @param request context
     * @param query optional query
     * @param asset can be a a named graph for executing a query or a skill asset
     * @return response
     */
    @POST
    public Response postQuery(@Context UriInfo request, String query, @QueryParam("asset") String asset) {
        monitor.debug(String.format("Received a POST call request %s to asset %s with query %s",request,query,asset));
        return executeQuery(query,asset,request.getQueryParameters());      
    }

    /**
     * endpoint for getting a query
     * @param request context
     * @param query optional query
     * @param asset can be a a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    public Response getQuery(@Context UriInfo request, @QueryParam("query") String query, @QueryParam("asset") String asset) {
        monitor.debug(String.format("Received a GET call request %s to asset %s with query %s",request,query,asset));
        return executeQuery(query,asset,request.getQueryParameters());
    }

    /**
     * internal execution logic
     * @param query a non-null text
     * @param asset a non-null asset name
     * @return result of execution
     */
    protected Response executeQuery(String query, String asset, MultivaluedMap<String,String> parameters) {
        monitor.debug(String.format("Executing query %s on asset %s with params %s",query,asset,parameters));
        return Response.ok("<?xml version="1.0"?> <result/>",MediaType.APPLICATION_XML);
    }
}
