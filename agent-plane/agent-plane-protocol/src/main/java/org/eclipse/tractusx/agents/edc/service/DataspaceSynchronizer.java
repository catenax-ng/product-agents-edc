package org.eclipse.tractusx.agents.edc.service;

import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.eclipse.tractusx.agents.edc.rdf.RDFStore;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.catalog.spi.Catalog;
import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.spi.query.Criterion;
import org.eclipse.edc.spi.query.QuerySpec;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * A service which keeps a triple store and
 * the associated dataspace in sync
 */
public class DataspaceSynchronizer implements Runnable {

    /**
     * constants
     */
    protected final static Node CX_ASSET=NodeFactory.createURI("https://github.com/catenax-ng/product-knowledge/ontology/cx.ttl#offersAsset");
    protected final static Map<String,Node> assetPropertyMap=new HashMap<>();
    protected final static QuerySpec federatedAssetQuery = QuerySpec.Builder.newInstance().filter(List.of(new Criterion("cx:isFederated","=","true"))).build();

    static {
        assetPropertyMap.put("https://w3id.org/edc/v0.0.1/ns/id",NodeFactory.createURI("urn:cx-common#id"));
        assetPropertyMap.put("https://w3id.org/edc/v0.0.1/ns/name",NodeFactory.createURI("urn:cx-common#name"));
        assetPropertyMap.put("https://w3id.org/edc/v0.0.1/ns/description",NodeFactory.createURI("urn:cx-common#description"));
        assetPropertyMap.put("https://w3id.org/edc/v0.0.1/ns/version",NodeFactory.createURI("urn:cx-common#version"));
        assetPropertyMap.put("https://w3id.org/edc/v0.0.1/ns/contenttype",NodeFactory.createURI("urn:cx-common#contentType"));
        assetPropertyMap.put("rdf:type",NodeFactory.createURI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"));
        assetPropertyMap.put("rdfs:isDefinedBy",NodeFactory.createURI("http://www.w3.org/2000/01/rdf-schema#isDefinedBy"));
        assetPropertyMap.put("cx:protocol",NodeFactory.createURI("urn:cx-common#protocol"));
        assetPropertyMap.put("sh:shapeGraph",NodeFactory.createURI("http://www.w3.org/ns/shacl#shapeGraph"));
        assetPropertyMap.put("cx:isFederated",NodeFactory.createURI("urn:cx-common#isFederated"));
        assetPropertyMap.put("asset:prop:contract",NodeFactory.createURI("urn:cx-common#contract"));
        assetPropertyMap.put("asset:prop:provider",NodeFactory.createURI("urn:cx-common#provider"));
        assetPropertyMap.put("policy:type",NodeFactory.createURI("urn:cx-common#policyType"));
        assetPropertyMap.put("policy:assignee",NodeFactory.createURI("urn:cx-common#policyAssignee"));
        assetPropertyMap.put("policy:assigner",NodeFactory.createURI("urn:cx-common#policyAssigner"));
        assetPropertyMap.put("policy:target",NodeFactory.createURI("urn:cx-common#policyTarget"));
        assetPropertyMap.put("offer:id",NodeFactory.createURI("urn:cx-common#offer"));
    }

    /**
     * service links
     */
    protected final ScheduledExecutorService service;
    protected final AgentConfig config;
    protected final DataManagement dataManagement;
    protected final RDFStore rdfStore;
    protected final Monitor monitor;

    /**
     * internal state
     */
    protected boolean isStarted=false;

    /**
     * creates the synchronizer
     * @param service scheduler
     * @param config edc config
     * @param dataManagement data management service remoting
     * @param rdfStore a triple store for persistance
     * @param monitor logging subsystem
     */
    public DataspaceSynchronizer(ScheduledExecutorService service, AgentConfig config, DataManagement dataManagement, RDFStore rdfStore, Monitor monitor) {
        this.service=service;
        this.config=config;
        this.dataManagement=dataManagement;
        this.rdfStore=rdfStore;
        this.monitor=monitor;
    }

    /**
     * starts the synchronizer
     */
    public synchronized void start() {
        if(!isStarted) {
            isStarted=true;
            long interval=config.getDataspaceSynchronizationInterval();
            String[] connectors=config.getDataspaceSynchronizationConnectors();
            if(interval>0 && connectors!=null && connectors.length>0) {
                monitor.info(String.format("Starting dataspace synchronization on %d connectors with interval %d milliseconds", connectors.length,interval));
                service.schedule(this,interval,TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * stops the synchronizer
     */
    public synchronized void shutdown() {
        if(isStarted) {
            monitor.info("Shutting down dataspace synchronization");
            isStarted=false;
            service.shutdown();
        }
    }

    /**
     * runs the synchronizer when scheduled
     */
    @Override
    public void run() {
        monitor.debug("Synchronization run has been started");
        if(isStarted) {
            for (String remote : config.getDataspaceSynchronizationConnectors()) {
                if(isStarted) {
                    monitor.debug(String.format("About to synchronize remote connector %s", remote));
                    rdfStore.startTx();
                    try {
                        Catalog catalog = dataManagement.getCatalog(remote,federatedAssetQuery);
                        monitor.debug(String.format("Found a catalog with %d entries for remote connector %s", catalog.getContractOffers().size(), remote));
                        Node graph = rdfStore.getDefaultGraph();
                        Node connector = NodeFactory.createURI(remote.replace("https", "edcs").replace("http", "edc"));
                        Quad findAssets = Quad.create(graph,connector,CX_ASSET,Node.ANY);
                        Iterator<Quad> assetQuads= rdfStore.getDataSet().find(findAssets);
                        int tupleCount=0;
                        while(assetQuads.hasNext()) {
                            Quad quadAsset=assetQuads.next();
                            Quad findAssetProps = Quad.create(graph,quadAsset.getObject(),Node.ANY,Node.ANY);
                            Iterator<Quad> propQuads=rdfStore.getDataSet().find(findAssetProps);
                            while(propQuads.hasNext()) {
                                Quad quadProp=propQuads.next();
                                rdfStore.getDataSet().delete(quadProp);
                                tupleCount++;
                            }
                            rdfStore.getDataSet().delete(quadAsset);
                            tupleCount++;
                        }
                        monitor.debug(String.format("About to delete %d old tuples.", tupleCount));
                        tupleCount=0;
                        for (ContractOffer offer : catalog.getContractOffers()) {
                            for(Quad quad : convertToQuads(graph,connector,offer)) {
                                tupleCount++;
                                rdfStore.getDataSet().add(quad);
                            }
                        }
                        monitor.debug(String.format("About to add %d new tuples.", tupleCount));
                        rdfStore.commit();
                    } catch (Throwable io) {
                        monitor.warning(String.format("Could not synchronize remote connector %s because of %s. Going ahead.", remote, io));
                        rdfStore.abort();
                    }
                } else {
                    monitor.debug(String.format("Synchronization is no more active. Skipping all connectors starting from %s.",remote));
                    break;
                }
            } // for
            if(isStarted) {
                monitor.debug("Schedule next synchronization run");
                service.schedule(this, config.getDataspaceSynchronizationInterval(), TimeUnit.MILLISECONDS);
            } else {
                monitor.debug("Synchronization is no more active. Disable next run.");
            }
        }
    }

    /**
     * Workaround the castration of the IDS catalogue
     * @param offer being made
     * @return default props
     */
    public static Map<String,String> getProperties(ContractOffer offer) {
        Map<String,String> assetProperties = new HashMap<>();
        offer.getPolicy().getExtensibleProperties().forEach( (key,value) -> assetProperties.put(key,String.valueOf(value)));
        assetProperties.put("offer:id",offer.getId());
        assetProperties.put("asset:prop:id",offer.getAssetId());
        assetProperties.put("asset:prop:provider",offer.getProviderId());
        assetProperties.put("policy:type",offer.getPolicy().getType().name());
        assetProperties.put("policy:target",offer.getPolicy().getTarget());
        assetProperties.put("policy:assignee",offer.getPolicy().getAssignee());
        assetProperties.put("policy:assigner",offer.getPolicy().getAssigner());
        if(!assetProperties.containsKey("rdf:type")) {
            String assetType=offer.getAssetId();
            int indexOfQuestion = assetType.indexOf("?");
            if (indexOfQuestion > 0) {
                assetType = assetType.substring(0, indexOfQuestion - 1);
            }
            assetProperties.put("rdf:type","<"+assetType+">");
        }
        return assetProperties;
    }

    /**
     * convert a given contract offer into quads
     * @param graph default graph
     * @param connector parent connector hosting the offer
     * @param offer the contract offer
     * @return a collection of quads
     */
    public Collection<Quad> convertToQuads(Node graph, Node connector, ContractOffer offer) {
        Map<String,String> assetProperties=getProperties(offer);

        if(!"true".equals(String.valueOf(assetProperties.getOrDefault("cx:isFederated","false")))) {
            return List.of();
        }
        List<Quad> quads=new ArrayList<>();
        Node assetNode=NodeFactory.createURI(offer.getAssetId());
        quads.add(Quad.create(graph,
                connector,
                CX_ASSET,
                assetNode));
        for(Map.Entry<String,String> assetProp : assetProperties.entrySet()) {
            String key=assetProp.getKey();
            Node node=assetPropertyMap.get(key);
            while(node==null && key.indexOf('.')>=0) {
                key=key.substring(key.lastIndexOf(".")+1);
                node=assetPropertyMap.get(key);
            }
            if(node!=null) {
                String pureProperty=assetProp.getValue();
                if(pureProperty!=null) {
                    switch (key) {
                        case "asset:prop:contract":
                        case "rdfs:isDefinedBy":
                        case "rdf:type":
                        case "cx:protocol":
                            String[] urls = pureProperty.split(",");
                            for (String url : urls) {
                                Node o;
                                url = url.trim();
                                if (url.startsWith("<") && url.endsWith(">")) {
                                    url = url.substring(1, url.length() - 1);
                                    o = NodeFactory.createURI(url);
                                } else if (url.startsWith("\"") && url.endsWith("\"")) {
                                    // TODO parse ^^literalType annotations
                                    url = url.substring(1, url.length() - 1);
                                    o = NodeFactory.createLiteral(url);
                                } else {
                                    o = NodeFactory.createLiteral(url);
                                }
                                quads.add(Quad.create(graph, assetNode, node, o));
                            }
                            break;
                        default:
                            quads.add(Quad.create(graph, assetNode, node, NodeFactory.createLiteral(pureProperty)));
                    }
                }
            }
        }
        return quads;
    }

}
