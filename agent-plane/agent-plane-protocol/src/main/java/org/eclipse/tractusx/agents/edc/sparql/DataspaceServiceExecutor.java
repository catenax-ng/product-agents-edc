//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.sparql;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.IAgreementController;
import org.eclipse.tractusx.agents.edc.http.HttpClientAdapter;
import okhttp3.OkHttpClient;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.riot.ResultSetMgr;
import org.apache.jena.riot.WebContent;
import org.apache.jena.riot.resultset.ResultSetLang;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.OpAsQuery;
import org.apache.jena.sparql.algebra.Transformer;
import org.apache.jena.sparql.algebra.op.*;
import org.apache.jena.sparql.algebra.table.TableData;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.engine.ExecutionContext;
import org.apache.jena.sparql.engine.QueryIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.engine.binding.BindingBuilder;
import org.apache.jena.sparql.engine.http.HttpParams;
import org.apache.jena.sparql.engine.iterator.QueryIter;
import org.apache.jena.sparql.engine.iterator.QueryIter1;
import org.apache.jena.sparql.engine.iterator.QueryIterPlainWrapper;
import org.apache.jena.sparql.exec.RowSet;
import org.apache.jena.sparql.exec.RowSetAdapter;
import org.apache.jena.sparql.exec.http.*;
import org.apache.jena.sparql.graph.NodeTransformLib;
import org.apache.jena.sparql.resultset.ResultSetMem;
import org.apache.jena.sparql.service.bulk.ChainingServiceExecutorBulk;
import org.apache.jena.sparql.service.bulk.ServiceExecutorBulk;
import org.apache.jena.sparql.service.single.ServiceExecutor;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.edr.EndpointDataReference;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A service executor (single and bulk mode) which replaces outgoing http calls
 * by appropriate dataspace agreements/calls.
 * Register in the ServiceExecutorRegistry via
 * - add(...) for single execution
 * - addBulkLink(...) for multiple execution
 */
public class DataspaceServiceExecutor implements ServiceExecutor, ChainingServiceExecutorBulk {

    /**
     * EDC services
     */
    final Monitor monitor;
    final IAgreementController agreementController;
    final AgentConfig config;
    final HttpClient client;
    final ExecutorService executor;
    final ObjectMapper objectMapper;
    final AgentConfig agentConfig;

    /**
     * some constants
     */
    public final static Symbol authKey = Symbol.create("cx:authKey");
    public final static Symbol authCode = Symbol.create("cx:authCode");
    public final static Pattern EDC_TARGET_ADDRESS = Pattern.compile("((?<protocol>edc|edcs)://(?<connector>[^#?]*))?(#(?<asset>[^/?]*))?(\\?(?<params>.*))?");
    public final static Symbol targetUrl = Symbol.create("cx:targetUrl");
    public final static Symbol asset = Symbol.create("cx:asset");

    /**
     * create a new executor
     *
     * @param monitor    logging subsystem
     * @param controller dataspace agreement
     */
    public DataspaceServiceExecutor(Monitor monitor, IAgreementController controller, AgentConfig config, OkHttpClient client, ExecutorService executor, TypeManager typeManager, AgentConfig agentConfig) {
        this.monitor = monitor;
        this.agreementController = controller;
        this.config = config;
        this.client=new HttpClientAdapter(client);
        this.executor=executor;
        this.objectMapper=typeManager.getMapper();
        this.agentConfig=agentConfig;
    }

    /**
     * bulk execution call
     * @param opService bound operator
     * @param queryIterator incoming bindings (may set service uri and input params)
     * @param executionContext context
     * @param serviceExecutorBulk bulk executor
     * @return binding generating iterator
     * TODO implement batch size per service and not globally
     */
    @Override
    public QueryIterator createExecution(OpService opService, QueryIterator queryIterator, ExecutionContext executionContext, ServiceExecutorBulk serviceExecutorBulk) {
        Node serviceNode=opService.getService();
        Set<String> boundVars=new HashSet<>();
        long batchSize=config.getFederationServiceBatchSize();
        // return an iterator over batches
        return new QueryIter1(queryIterator,executionContext) {

            // the iterator over the current batch
            private QueryIterator batchIterator;

            @Override
            protected boolean hasNextBinding() {
                return (batchIterator!=null && batchIterator.hasNext()) || hasNextResultBinding();
            }

            public boolean hasNextResultBinding() {
                // do we have additional input bindings
                if(this.getInput().hasNext()) {
                    // yes then read the next batch
                    Map<String,List<Binding>> bindings=new HashMap<>();
                    long batchLength=0;
                    while(this.getInput().hasNext() && batchLength++<batchSize) {
                        Binding binding = this.getInput().next();
                        Iterator<Var> vars=binding.vars();
                        while(vars.hasNext()) {
                            boundVars.add(vars.next().getVarName());
                        }
                        // detect the service uri under the current binding
                        Node keyNode = serviceNode;
                        if (keyNode.isVariable())
                            keyNode = binding.get((Var) keyNode);
                        if (keyNode.isURI()) {
                            String key = keyNode.getURI();
                            if (!bindings.containsKey(key)) {
                                bindings.put(key, new ArrayList<>());
                            }
                            bindings.get(key).add(binding);
                        } else {
                            monitor.warning("Omitting a call because of lacking service binding");
                        }
                    }
                    ExecutionContext ctx=this.getExecContext();

                    List<Future<QueryIterator>> futureBindings=bindings.entrySet().stream().map(serviceSpec -> executor.submit(() ->
                            createExecution(opService, serviceSpec.getKey(), boundVars, serviceSpec.getValue(), ctx))).collect(Collectors.toList());

                    batchIterator=new QueryIterFutures(config,monitor,config.getControlPlaneManagementUrl(),config.getDefaultAsset(),serviceNode, ctx.getContext(),futureBindings);
                    return hasNextBinding();
                } else {
                    return false;
                }
            }

            @Override
            protected Binding moveToNextBinding() {
                return batchIterator.next();
            }

            @Override
            protected void requestSubCancel() {

            }

            @Override
            protected void closeSubIterator() {

            }

        };
    }

    /**
     * single execution mode
     * @param opExecute  the bound operator (if variable is used in service description)
     * @param opOriginal the unbound operator
     * @param binding    the current binding
     * @param execCxt    the execution context
     * @return a set of query results
     */
    @Override
    public QueryIterator createExecution(OpService opExecute, OpService opOriginal, Binding binding, ExecutionContext execCxt) {
        // it maybe that a subselect has "masked" some input variables
        Node serviceNode=opExecute.getService();
        if(serviceNode.isVariable()) {
            serviceNode=binding.get((Var) serviceNode);
        }
        if (!serviceNode.isURI())
            throw new QueryExecException("Service URI not bound: " + opExecute.getService());
        // check whether we need to route over EDC
        String target = serviceNode.getURI();
        Set<String> allowedVars=new HashSet<>();
        Iterator<Var> allAllowedVars = binding.vars();
        while(allAllowedVars.hasNext()) {
            allowedVars.add(allAllowedVars.next().getVarName());
        }
        return createExecution(opOriginal,target,allowedVars,List.of(binding),execCxt);
    }

    /**
     * (re-) implements the remote http service execution
     * @param opOriginal the unbound operator
     * @param serviceURL uri of the target service
     * @param boundVars a set of all bound variables
     * @param bindings   the current bindings
     * @param execCxt    the execution context
     * @return a set of query results
     */
     public QueryIterator createExecution(OpService opOriginal, String serviceURL, Set<String> boundVars, List<Binding> bindings, ExecutionContext execCxt) {
        Context context = execCxt.getContext();
        boolean silent = opOriginal.getSilent();

        // derive the asset type from the service URL, if possible
        String assetType= serviceURL.contains("Skill") ? "cx-common:SkillAsset" : serviceURL.contains("Graph") ? "cx-common:GraphAsset" : "cx-common:Asset";

        // in case we have an EDC target, we need to negotiate/proxy the transfer
        Matcher edcMatcher = EDC_TARGET_ADDRESS.matcher(serviceURL);
        if (edcMatcher.matches()) {

            //
            // EDC case: negotiate and proxy the transfer
            //

            monitor.info(String.format("About to execute edc target %s via dataspace", serviceURL));
            String remoteUrl = edcMatcher.group("connector");
            if (remoteUrl == null || remoteUrl.length() == 0) {
                remoteUrl = config.getControlPlaneIdsUrl();
            } else {
                if ("edcs".equals(edcMatcher.group("protocol"))) {
                    remoteUrl = "https://" + remoteUrl;
                } else {
                    remoteUrl = "http://" + remoteUrl;
                }
            }
            String asset = edcMatcher.group("asset");
            if (asset == null || asset.length() == 0) {
                GraphRewriteVisitor grv=new GraphRewriteVisitor();
                GraphRewrite gr=new GraphRewrite(monitor,bindings,grv);
                Op transformed=Transformer.transform(gr,opOriginal.getSubOp(),grv,null);
                opOriginal=new OpService(opOriginal.getService(),transformed,opOriginal.getSilent());
                Set<String> graphNames=gr.getGraphNames();
                if(graphNames.size()>1) {
                    throw new QueryExecException("There are several graph assets (currently not supported due to negotiation strategy, please rewrite your query) under EDC-based service: " + serviceURL);
                } else {
                    Optional<String> graphName=graphNames.stream().findAny();
                    if(graphName.isEmpty()) {
                        throw new QueryExecException("There is no graph asset under EDC-based service: " + serviceURL);
                    } else {
                        asset = graphName.get();
                    }
                }
            }
            EndpointDataReference endpoint = agreementController.get(asset);
            if (endpoint == null) {
                endpoint = agreementController.createAgreement(remoteUrl, asset);
                if(endpoint == null) {
                    throw new QueryExecException(String.format("Could not get an endpoint calback from connector %s to asset %s - Most likely this was a recursive call and you forgot to setup two control planes.", remoteUrl, asset));
                }
            }
            // the asset type should be annotated in the rdf type property
            assetType=endpoint.getProperties().getOrDefault("http://www.w3.org/1999/02/22-rdf-syntax-ns#type",assetType);

            // put the endpoint information into a new service operator
            // and cater for the EDC public api slash problem
            serviceURL = endpoint.getEndpoint();
            if(!serviceURL.endsWith("/")) {
                serviceURL=serviceURL+"/";
            }
            if (edcMatcher.group("params") != null) {
                serviceURL = serviceURL + "?" + edcMatcher.group("params");
            }
            Map<String, Map<String, List<String>>> allServiceParams = context.get(Service.serviceParams);
            if (allServiceParams == null) {
                allServiceParams = new HashMap<>();
                context.put(Service.serviceParams, allServiceParams);
            }
            Map<String, List<String>> serviceParams = allServiceParams.computeIfAbsent(serviceURL, k -> new HashMap<>());
            serviceParams.put("cx_accept",List.of("application/json"));
            execCxt.getContext().put(authKey, endpoint.getAuthKey());
            execCxt.getContext().put(authCode, endpoint.getAuthCode());
        } else {
            monitor.info(String.format("About to execute http target %s without dataspace", serviceURL));
        }

        // Next case distinction: we could either have a query or
        // a direct skill call
        if(!assetType.contains("Skill")) {
            // http execute with headers and such
            try {
                Op opRemote = opOriginal.getSubOp();
                int hashCode = Math.abs(opRemote.hashCode());
                String bindingVarName = "binding" + hashCode;
                Var idVar = Var.alloc(bindingVarName);
                VariableDetector vd = new VariableDetector(boundVars);
                opRemote = NodeTransformLib.transform(vd, opRemote);
                List<Var> neededVars = vd.getVariables();
                Map<String, Binding> resultingBindings = new HashMap<>();
                Map<Node, List<Binding>> newBindings = new HashMap<>();
                for (Binding originalBinding : bindings) {
                    StringBuilder keyBuilder = new StringBuilder();
                    BindingBuilder bb = BindingBuilder.create();
                    for (Var neededVar : neededVars) {
                        Node node = originalBinding.get(neededVar);
                        keyBuilder.append(neededVar.getVarName());
                        keyBuilder.append("#");
                        keyBuilder.append(node.toString());
                        bb.add(neededVar, node);
                    }
                    String key = keyBuilder.toString();
                    Node keyNode;
                    if (resultingBindings.containsKey(key)) {
                        Binding existingBinding = resultingBindings.get(key);
                        keyNode = existingBinding.get(idVar);
                    } else {
                        keyNode = NodeFactory.createLiteral(String.valueOf(resultingBindings.size()));
                        bb.add(idVar, keyNode);
                        newBindings.put(keyNode, new ArrayList<>());
                        Binding newBinding = bb.build();
                        //bb = BindingBuilder.create(newBinding);
                        resultingBindings.put(key, newBinding);
                    }
                    final BindingBuilder bb2 = BindingBuilder.create(originalBinding);
                    bb2.set(idVar, keyNode);
                    newBindings.get(keyNode).add(bb2.build());
                }
                neededVars.add(idVar);
                TableData table = new TableData(neededVars, new ArrayList<>(resultingBindings.values()));
                OpTable opTable = OpTable.create(table);

                Query query;

                // do we have a "sub-select", then we smuggle our binding into it
                if(opRemote instanceof OpProject) {
                    OpProject opRemoteProject=(OpProject) opRemote;
                    Op join = OpSequence.create(opTable,opRemoteProject.getSubOp());
                    List<Var> resultVars=opRemoteProject.getVars();
                    resultVars.add(idVar);
                    query=OpAsQuery.asQuery(new OpProject(join,resultVars));
                } else {
                    Op join = OpSequence.create(opTable, opRemote);
                    query = OpAsQuery.asQuery(join);
                }

                monitor.debug(String.format("Prepared target %s for query %s", serviceURL, query));

                // -- Setup
                //boolean withCompression = context.isTrueOrUndef(httpQueryCompression);
                long timeoutMillis = config.getReadTimeout();

                // RegistryServiceModifier is applied by QueryExecHTTP
                Params serviceParams = getServiceParamsFromContext(serviceURL, context);
                HttpClient httpClient = chooseHttpClient(serviceURL, context);

                QuerySendMode querySendMode = chooseQuerySendMode(serviceURL, context, QuerySendMode.asGetWithLimitBody);
                // -- End setup

                // Build the execution
                QueryExecutorBuilder qExecBuilder = QueryExecutor.newBuilder()
                        .endpoint(serviceURL)
                        .timeout(timeoutMillis, TimeUnit.MILLISECONDS)
                        .query(query)
                        .params(serviceParams)
                        .context(context)
                        .httpClient(httpClient)
                        .objectMapper(objectMapper)
                        .agentConfig(agentConfig)
                        .sendMode(querySendMode);

                if (context.isDefined(authKey)) {
                    String authKeyProp = context.get(authKey);
                    monitor.debug(String.format("About to use authentication header %s on http target %s", authKeyProp, serviceURL));
                    String authCodeProp = context.get(authCode);
                    qExecBuilder = qExecBuilder.httpHeader(authKeyProp, authCodeProp);
                }

                try (QueryExecutor qExec = qExecBuilder.build()) {
                    // Detach from the network stream.
                    RowSet rowSet = qExec.select().materialize();
                    QueryIterator qIter = QueryIterPlainWrapper.create(rowSet);
                    qIter = QueryIter.makeTracked(qIter, execCxt);
                    return new QueryIterJoin(qIter, newBindings, idVar, execCxt);
                }
            } catch (RuntimeException ex) {
                if (silent) {
                    Log.warn(this, "SERVICE " + serviceURL + " : " + ex.getMessage());
                    // Return the input
                    return QueryIterPlainWrapper.create(bindings.iterator(), execCxt);
                }
                throw ex;
            }
        } else {
            // Skill call
            try {
                // [QExec] Add getSubOpUnmodified();
                Op opRemote = opOriginal.getSubOp();
                String bindingVarName="binding";
                Var idVar = Var.alloc(bindingVarName);
                SkillVariableDetector vd = new SkillVariableDetector(boundVars);
                opRemote=Transformer.transform(vd,opRemote);
                Map<String,Node> neededVars=vd.getVariables();
                var parameterSet=new ResultSetMem() {
                    public void setVarNames(List<String> vars) {
                        this.varNames=vars;
                    }
                    public List<Binding> getRows() {
                        return this.rows;
                    }

                };
                List<String> vars=new ArrayList<>();
                vars.add(bindingVarName);
                neededVars.forEach((key1, value) -> vars.add(key1));
                parameterSet.setVarNames(vars);
                Map<String, Binding> resultingBindings = new HashMap<>();
                Map<Node, List<Binding>> newBindings = new HashMap<>();
                for(Binding originalBinding : bindings) {
                    StringBuilder keyBuilder = new StringBuilder();
                    BindingBuilder bb=BindingBuilder.create();
                    for(Map.Entry<String,Node> neededVar : neededVars.entrySet()) {
                        Node node=neededVar.getValue();
                        if(node.isVariable()) {
                            node=originalBinding.get((Var) node);
                        }
                        if(node!=null) {
                            keyBuilder.append(neededVar.getKey());
                            keyBuilder.append("#");
                            keyBuilder.append(node);
                            bb.add(Var.alloc(neededVar.getKey()), node);
                        }
                    }
                    String key=keyBuilder.toString();
                    Node keyNode;
                    if(resultingBindings.containsKey(key)) {
                        Binding existingBinding=resultingBindings.get(key);
                        keyNode=existingBinding.get(idVar);
                    } else {
                        keyNode=NodeFactory.createLiteral(String.valueOf(resultingBindings.size()));
                        bb.add(idVar,keyNode);
                        newBindings.put(keyNode,new ArrayList<>());
                        Binding newBinding=bb.build();
                        //bb=BindingBuilder.create(newBinding);
                        resultingBindings.put(key,newBinding);
                    }
                    final BindingBuilder bb2=BindingBuilder.create(originalBinding);
                    bb2.set(idVar,keyNode);
                    newBindings.get(keyNode).add(bb2.build());
                }
                parameterSet.getRows().addAll(resultingBindings.values());
                parameterSet.reset();
                long timeoutMillis = config.getReadTimeout();
                HttpClient httpClient = chooseHttpClient(serviceURL, context);

                String bindingSet=ResultSetMgr.asString(parameterSet, ResultSetLang.RS_JSON);
                HttpRequest.Builder skillRequest= HttpRequest.newBuilder().
                        uri(new URI(serviceURL)).
                        header("Content-Type", WebContent.contentTypeResultsJSON).
                        timeout(Duration.ofMillis(timeoutMillis)).
                        header("Accept",WebContent.contentTypeResultsJSON).
                        POST(HttpRequest.BodyPublishers.ofString(bindingSet));

                if (context.isDefined(authKey)) {
                    String authKeyProp=context.get(authKey);
                    monitor.debug(String.format("About to use authentication header %s on http target %s", authKeyProp,serviceURL));
                    String authCodeProp=context.get(authCode);
                    skillRequest=skillRequest.header(authKeyProp,authCodeProp);
                }

                HttpResponse<InputStream> remoteCall=httpClient.send(skillRequest.build(), HttpResponse.BodyHandlers.ofInputStream());
                if(remoteCall.statusCode()>=200 && remoteCall.statusCode()<300) {
                    ResultSet result=ResultSetMgr.read(remoteCall.body(), ResultSetLang.RS_JSON);
                    RowSet rowSet=new RowSetAdapter(result);
                    QueryIterator qIter = QueryIterPlainWrapper.create(rowSet);
                    qIter = QueryIter.makeTracked(qIter, execCxt);
                    return new QueryIterJoin(qIter, newBindings, idVar, execCxt);
                } else {
                    Log.warn(this, "SERVICE " + serviceURL + " resulted in status code " + remoteCall.statusCode());
                    remoteCall.body().close();
                    // Return the input
                    return QueryIterPlainWrapper.create(bindings.iterator(), execCxt);
                }
            } catch (URISyntaxException | IOException | InterruptedException | RuntimeException ex) {
                if (silent) {
                    Log.warn(this, "SERVICE " + serviceURL + " : " + ex.getMessage());
                    // Return the input
                    return QueryIterPlainWrapper.create(bindings.iterator(), execCxt);
                }
                throw new RuntimeException("Could not invoke remote skill",ex);
            }
        }
    }

    /**
     * choose an appropriate client
     *
     * @param serviceURL target url
     * @param context    query context
     * @return http client
     */
    protected HttpClient chooseHttpClient(String serviceURL, Context context) {
        if(context==null) {
            monitor.warning(String.format("Context is null when obtaining http client for %s",serviceURL));
        }
        return client;
    }

    /**
     * choose an appropriate send mode
     *
     * @param serviceURL target url
     * @param context    query content
     * @param dftValue   default send mode of dft
     * @return decided send mode
     */
    protected QuerySendMode chooseQuerySendMode(String serviceURL, Context context, QuerySendMode dftValue) {
        if(dftValue!=QuerySendMode.asPost) {
            monitor.warning(String.format("Default send mode %s for %s is not post",dftValue,serviceURL));
        }
        if(context==null) {
            monitor.warning(String.format("Context is null when obtaining send mode for %s",serviceURL));
        }
        return QuerySendMode.asPost;
    }

    /**
     * extract http params from query
     *
     * @param serviceURI target url
     * @param context    query context
     * @return query params
     * @throws QueryExecException in case there is something wrong
     */
    protected Params getServiceParamsFromContext(String serviceURI, Context context) throws QueryExecException {
        Params params = Params.create();

        Object obj = context.get(Service.serviceParams);

        if (obj == null)
            return params;

        // Old style.
        try {
            @SuppressWarnings("unchecked")
            Map<String, Map<String, List<String>>> serviceParams = (Map<String, Map<String, List<String>>>) obj;
            Map<String, List<String>> paramsMap = serviceParams.get(serviceURI);
            if (paramsMap != null) {
                for (String param : paramsMap.keySet()) {
                    if (HttpParams.pQuery.equals(param))
                        throw new QueryExecException("ARQ serviceParams overrides the 'query' SPARQL protocol parameter");
                    List<String> values = paramsMap.get(param);
                    for (String value : values)
                        params.add(param, value);
                }
            }
            return params;
        } catch (Throwable ex) {
            monitor.warning("Failed to process " + obj + " : context value of ARQ.serviceParams");
            return null;
        }
    }

}