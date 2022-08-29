package io.catenax.knowledge.dataspace.edc;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQLQueryProcessor;
import org.apache.jena.fuseki.servlets.SPARQL_QueryGeneral;
import org.apache.jena.fuseki.system.ActionCategory;

import org.eclipse.dataspaceconnector.spi.monitor.Monitor;

@Consumes({MediaType.TEXT_PLAIN})
@Path("/agent")
public class AgentController {
    private final Monitor monitor;
    private final AgreementController agreementController;
    private final SPARQLQueryProcessor processor;
    private long count;

    /** creates a new agreement controller */
    public AgentController(Monitor monitor, AgreementController agreementController, AgentConfig config) {
        this.monitor = monitor;
        this.agreementController = agreementController;
        this.processor=new SPARQL_QueryGeneral.SPARQL_QueryProc();
        monitor.debug(String.format("Creating a new sparql processor %s",this.processor));
    }

    public javax.servlet.http.HttpServletResponse getJavaxResponse(HttpServletResponse jakartaResponse) {
        return JakartaWrapper.javaxify(jakartaResponse,javax.servlet.http.HttpServletResponse.class);
    }

    public javax.servlet.http.HttpServletRequest getJavaxRequest(HttpServletRequest jakartaRequest) {
        return JakartaWrapper.javaxify(jakartaRequest,javax.servlet.http.HttpServletRequest.class);
    }

    /**
     * endpoint for posting a query
     * @param request context
     * @param query optional query
     * @param asset can be a a named graph for executing a query or a skill asset
     * @return response
     */
    @POST
    public Response postQuery(@Context HttpServletRequest request,@Context HttpServletResponse response) {
        monitor.debug(String.format("Received a POST call request %s ",request,response));
        HttpAction action=new HttpAction(count++,null,ActionCategory.ACTION,getJavaxRequest(request),getJavaxResponse(response));
        processor.execute(action); 
        return Response.ok("ok").build();   
    }

    /**
     * endpoint for getting a query
     * @param request context
     * @param query optional query
     * @param asset can be a a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    public Response getQuery(@Context HttpServletRequest request,@Context HttpServletResponse response) {
        monitor.debug(String.format("Received a GET call request %s response %s",request,response));
        HttpAction action=new HttpAction(count++,null,ActionCategory.ACTION,getJavaxRequest(request),getJavaxResponse(response));
        processor.execute(action);    
        return Response.ok("ok").build();   
    }

}
