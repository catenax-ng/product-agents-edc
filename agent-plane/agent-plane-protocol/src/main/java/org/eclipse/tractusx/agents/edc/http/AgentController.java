//
// EDC Data Plane Agent Extension 
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.http;

import org.eclipse.tractusx.agents.edc.ISkillStore;
import org.eclipse.tractusx.agents.edc.SkillDistribution;
import org.eclipse.tractusx.agents.edc.sparql.SparqlQueryProcessor;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.AgentExtension;
import org.eclipse.tractusx.agents.edc.IAgreementController;

import java.util.Optional;
import java.util.regex.Matcher;

/**
 * The Agent Controller exposes a REST API endpoint
 * with which the EDC tenant can issue queries and execute
 * skills in interaction with local resources and the complete
 * Dataspace (the so-called Matchmaking Agent that is also hit by
 * incoming Agent transfers).
 * TODO exchange fixed memory store by configurable options
 * TODO generalize sub-protocols from SparQL
 */
@Path("/agent")
public class AgentController {

    // EDC services
    protected final Monitor monitor;
    protected final IAgreementController agreementController;
    protected final AgentConfig config;
    protected final ISkillStore skillStore;

    // the actual Matchmaking Agent is a Fuseki engine
    protected final SparqlQueryProcessor processor;
    protected final IDelegationService delegationService;

    /** 
     * creates a new agent controller 
     * @param monitor logging subsystem
     * @param agreementController agreement controller for remote skill/queries
     * @param config configuration
     * @param processor sparql processor
     */
    public AgentController(Monitor monitor, IAgreementController agreementController, AgentConfig config, SparqlQueryProcessor processor, ISkillStore skillStore, IDelegationService delegationService) {
        this.monitor = monitor;
        this.agreementController = agreementController;
        this.config=config;
        this.processor=processor;
        this.skillStore=skillStore;
        this.delegationService=delegationService;
    }

    /**
     * render nicely
     */
    @Override
    public String toString() {
        return super.toString()+"/agent";
    }

    /**
     * endpoint for posting a sparql query (maybe as a stored skill with a bindingset)
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @POST
    @Consumes({"application/sparql-query","application/sparql-results+json"})
    public Response postSparqlQuery(@QueryParam("asset") String asset,
                                    @Context HttpHeaders headers,
                                    @Context HttpServletRequest request,
                                    @Context HttpServletResponse response,
                                    @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a SparQL POST request %s for asset %s",request,asset));
        return executeQuery(asset, headers, request, response, uri);
    }

    /**
     * endpoint for posting a url-encoded form-based query
     * this is the default mode for graphdb which
     * sends the actual query language and other params together
     * with the query text in the body and expects a
     * special content disposition in the response header which
     * marks the body as a kind of file attachement.
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response compatible with graphdb convention
     */
    @POST
    @Consumes({"application/x-www-form-urlencoded"})
    public Response postFormQuery(@QueryParam("asset") String asset,
                                  @Context HttpHeaders headers,
                                  @Context HttpServletRequest request,
                                  @Context HttpServletResponse response,
                                  @Context UriInfo uri) {
        monitor.debug(String.format("Received a Form-based POST request %s for asset %s",request,asset));
        Response result= executeQuery(asset, headers, request, response, uri);
        response.addHeader("Content-Disposition","attachement; filename=query-result.srjs");
        return result;
    }

    /**
     * endpoint for posting a url-encoded form-based query
     * this is the default mode for graphdb which
     * sends the actual query language and other params together
     * with the query text in the body and expects a
     * special content disposition in the response header which
     * marks the body as a kind of file attachement.
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response compatible with graphdb convention
     */
    @POST
    @Path("/repositories/AGENT")
    @Consumes({"application/x-www-form-urlencoded"})
    public Response postFormRepositoryQuery(@QueryParam("asset") String asset,
                                            @Context HttpHeaders headers,
                                            @Context HttpServletRequest request,
                                            @Context HttpServletResponse response,
                                            @Context UriInfo uri) {
        monitor.debug(String.format("Received a Form-based POST repository request %s for asset %s",request,asset));
        Response result= executeQuery(asset, headers, request, response, uri);
        response.addHeader("Content-Disposition","attachement; filename=query-result.srjs");
        return result;
    }

    /**
     * endpoint for getting a query
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    public Response getQuery(@QueryParam("asset") String asset,
                             @Context HttpHeaders headers,
                             @Context HttpServletRequest request,
                             @Context HttpServletResponse response,
                             @Context UriInfo uri) {
        monitor.debug(String.format("Received a GET request %s for asset %s",request,asset));
        return executeQuery(asset, headers, request, response, uri);
    }

    /**
     * 2nd endpoint for getting a query
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    @Path("/repositories/AGENT")
    public Response getRepositoryQuery(@QueryParam("asset") String asset,
                                       @Context HttpHeaders headers,
                                       @Context HttpServletRequest request,
                                       @Context HttpServletResponse response,
                                       @Context UriInfo uri) {
        monitor.debug(String.format("Received a GET repository request %s for asset %s",request,asset));
        return executeQuery(asset, headers, request, response, uri);
    }

    /**
     * check import status
     * @return response
     */
    @GET
    @Path("/repositories/AGENT/import/active")
    public Response getRepositoryImportQuery(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET repository import active request %s",request));
        return Response.status(406,"Not Acceptable (HTTP status 406)").build();
    }

    /**
     * check size
     * @return response
     */
    @GET
    @Path("/rest/repositories/AGENT/size")
    public Response getRestRepositorySizeQuery(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET rest repository size request %s",request));
        return Response.ok("{\n" +
                "    \"inferred\": 70,\n" +
                "    \"total\": 70,\n" +
                "    \"explicit\": 0\n" +
                "}").type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * check size
     * @return response
     */
    @GET
    @Path("/repositories/AGENT/size")
    public Response getRepositorySizeQuery(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET repository size request %s",request));
        return Response.ok("0" ).type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * check import status
     * @return response
     */
    @GET
    @Path("/rest/repositories/AGENT/import/active")
    public Response getRestRepositoryImportQuery(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET rest repository import active request %s",request));
        return Response.ok("0").type(jakarta.ws.rs.core.MediaType.APPLICATION_JSON_TYPE).build();
    }

    /**
     * return version info for graphdb/fedx integration
     * @return version string
     */
    @GET
    @Path("/rest/info/version")
    public String getVersion(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Version request %s",request));
        return "0.8.4";
    }

    /**
     * return protocol info for graphdb/fedx integration
     * @return protocol string
     */
    @GET
    @Path("/protocol")
    public String getProtocol(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Protocol request %s",request));
        return "12";
    }

    /**
     * return version info for graphdb/fedx integration
     * @return version string
     */
    @GET
    @Path("/rest/locations/id")
    public String getId(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Id request %s",request));
        return "Catena-X Knowledge Agent";
    }

    /**
     * return repositories for graphdb/fedx integration
     * @return single repo as json
     */
    @GET
    @Path("/rest/repositories")
    public String getRestRepositories(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request,
            //@Context HttpServletResponse response,
            @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Rest Repositories request %s",request));
        String url=uri.toString();
        url=url.substring(0,url.length()-18);
        return "[\n" +
                "    {\n" +
                "        \"id\": \"AGENT\",\n" +
                "        \"title\": \"Catena-X Knowledge Agent Dataspace Endpoint\",\n" +
                "        \"uri\": \""+url+"\",\n" +
                "        \"externalUrl\": \""+url+"\",\n" +
                "        \"local\": false,\n" +
                "        \"type\": \"fuseki\",\n" +
                "        \"sesameType\": \"cx:AgentController\",\n" +
                "        \"location\": \"Catena-X Dev Dataspace\",\n" +
                "        \"readable\": true,\n" +
                "        \"writable\": true,\n" +
                "        \"unsupported\": false,\n" +
                "        \"state\": \"RUNNING\"\n" +
                "    }\n" +
                "]";
    }

    /**
     * return repositories for graphdb/fedx integration
     * @return single repo as csv
     */
    @GET
    @Path("/repositories")
    public Response getRepositories(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request,
            //@Context HttpServletResponse response,
            @Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Repositories request %s",request));
        String url=uri.toString();
        url=url.substring(0,url.length()-13);
        Response.ResponseBuilder builder=Response.ok("uri,id,title,readable,writable\n"+url+",AGENT,Catena-X Knowledge Agent Dataspace Endpoint,true,true\n");
        builder.type("text/csv;charset=UTF-8");
        builder.header("Content-Disposition","attachment; filename=repositories.csv");
        return builder.build();
    }

    /**
     * return repositories for graphdb/fedx integration
     * @return single repo as csv
     */
    @GET
    @Path("/repositories/AGENT/namespaces")
    public Response getNamespaces(
            //@Context HttpHeaders headers,
            @Context HttpServletRequest request
            //,@Context HttpServletResponse response,
            //@Context UriInfo uri
    ) {
        monitor.debug(String.format("Received a GET Namespaces request %s",request));
        Response.ResponseBuilder builder=Response.ok("prefix,namespace\n" +
                "rdf,http://www.w3.org/1999/02/22-rdf-syntax-ns#\n" +
                "owl,http://www.w3.org/2002/07/owl#\n" +
                "xsd,http://www.w3.org/2001/XMLSchema#\n" +
                "rdfs,http://www.w3.org/2000/01/rdf-schema#\n" +
                "cx,https://raw.githubusercontent.com/catenax-ng/product-knowledge/main/ontology/cx_ontology.ttl#\n");
        builder.type("text/csv;charset=UTF-8");
        builder.header("Content-Disposition","attachment; filename=namespaces.csv");
        return builder.build();
    }

    /**
     * the actual execution is done by delegating to the Fuseki engine
     * @param asset target graph
     * @return a response
     */
    public Response executeQuery(String asset, HttpHeaders headers, HttpServletRequest request, HttpServletResponse response, UriInfo uri) {
        String skill=null;
        String graph=null;
        String remoteUrl=null;

        if(asset!=null) {
            Matcher matcher= AgentExtension.GRAPH_PATTERN.matcher(asset);
            if(matcher.matches()) {
                remoteUrl=matcher.group("url");
                graph=matcher.group("graph");
            } else {
                matcher=ISkillStore.matchSkill(asset);
                if(!matcher.matches()) {
                    return Response.status(Response.Status.BAD_REQUEST).build();
                }
                remoteUrl=matcher.group("url");
                skill=matcher.group("skill");
            }
        }

        if(remoteUrl!=null) {
            return delegationService.executeQueryRemote(remoteUrl,skill,graph, headers, request, response, uri);
        }

        try {
            // exchange skill against text
            if( asset!=null ) {
                if(skillStore.isSkill(asset)) {
                    Optional<String> skillOption = skillStore.get(asset);
                    if (skillOption.isPresent()) {
                        skill = skillOption.get();
                    } else {
                        return HttpUtils.respond(monitor,headers, HttpStatus.SC_NOT_FOUND, "The requested skill is not registered.", null);
                    }
                }
            }

            processor.execute(request,response,skill,graph);
            // kind of redundant, but javax.ws.rs likes it this way
            return Response.status(response.getStatus()).build();
        } catch(WebApplicationException e) {
            return HttpUtils.respond(monitor,headers,e.getResponse().getStatus(),e.getMessage(),e.getCause());
        }
    }

    /**
     * endpoint for posting a skill
     * @param query mandatory query
     * @param asset asset key
     * @param name asset name
     * @param description asset description
     * @param version asset version
     * @param contract asset contract
     * @param mode asset mode
     * @param isFederated whether it appears in fed catalogue
     * @param ontologies list of ontologies
     * @return only status
     */
    @POST
    @Path("/skill")
    @Consumes({"application/sparql-query"})
    public Response postSkill(String query, 
        @QueryParam("asset") String asset, 
        @QueryParam("assetName") String name, 
        @QueryParam("assetDescription") String description, 
        @QueryParam("assetVersion") String version,  
        @QueryParam("contract") String contract,
        @QueryParam("distributionMode") SkillDistribution mode,
        @QueryParam("isFederated") boolean isFederated,
        @QueryParam("ontology") String[] ontologies
        ) {
        monitor.debug(String.format("Received a POST skill request %s %s %s %s %s %b %s ",asset,name,description,version,contract,mode.getMode(),isFederated,query));
        Response.ResponseBuilder rb;
        if(skillStore.put(asset,query,name,description,version,contract,mode,isFederated,ontologies)!=null) {
            rb=Response.ok();
        } else {
            rb=Response.status(HttpStatus.SC_CREATED);
        }
        return rb.build();
    }

    /**
     * endpoint for getting a skill
     * @param asset can be a named graph for executing a query or a skill asset
     * @return response
     */
    @GET
    @Path("/skill")
    @Produces({"application/sparql-query"})
    public Response getSkill(@QueryParam("asset") String asset) {
        monitor.debug(String.format("Received a GET skill request %s",asset));
        Response.ResponseBuilder rb;
        String query=skillStore.get(asset).orElse(null);
        if(query==null) {
            rb = Response.status(HttpStatus.SC_NOT_FOUND);
        } else {
            rb = Response.ok(query);
        }
        return rb.build();
    }
}
