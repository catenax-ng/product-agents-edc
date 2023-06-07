//
// EDC Data Plane Agent Extension Test
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package org.eclipse.tractusx.agents.edc.service;

import org.eclipse.edc.policy.model.PolicyType;
import org.eclipse.tractusx.agents.edc.TestConfig;
import org.eclipse.tractusx.agents.edc.rdf.RDFStore;
import okhttp3.*;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.tractusx.agents.edc.AgentConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.apache.jena.graph.NodeFactory;


import org.mockito.MockitoAnnotations;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.edc.connector.contract.spi.types.offer.ContractOffer;

/**
 * Tests the dataspace synchronization
 */
public class TestDataspaceSynchronizer {
    
    ConsoleMonitor monitor=new ConsoleMonitor();
    TestConfig config=new TestConfig();
    AgentConfig agentConfig=new AgentConfig(monitor,config);
    OkHttpClient client=new OkHttpClient();
    ScheduledExecutorService threadedExecutor= Executors.newSingleThreadScheduledExecutor();
    RDFStore store = new RDFStore(agentConfig,monitor);

    TypeManager typeManager=new TypeManager();

    DataManagement dm=new DataManagement(monitor,typeManager,client,agentConfig);
    DataspaceSynchronizer synchronizer = new DataspaceSynchronizer(threadedExecutor,agentConfig,dm,store,monitor);

    AutoCloseable mocks=null;

    @BeforeEach
    public void setUp()  {
        mocks=MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if(mocks!=null) {
            mocks.close();
            mocks=null;
        }
    }
    
    /**
     * test quad representation of a contract offer
     */
    @Test
    public void testQuadRepresentation()  {
        Node graph = store.getDefaultGraph();
        Node connector = NodeFactory.createURI("edc://test");
        Asset asset = Asset.Builder.newInstance()
                .id("urn:cx-common#GraphAsset?test:ExampleAsset")
                .contentType("application/json, application/xml")
                .version("1.9.2-SNAPSHOT")
                .name("Test Asset")
                .description("Test Asset for RDF Representation")
                .property("asset:prop:contract","<urn:cx-common#Contract?test:Contract>")
                .property("rdf:type","<urn:cx-common#GraphAsset>")
                .property("rdfs:isDefinedBy","<urn:cx-diagnosis>,<urn:cx-part>")
                .property("cx:protocol","<urn:cx-common#Protocol?w3c:Http#SPARQL>")
                .property("sh:shapesGraph",
                        "@prefix : <urn:cx:common#GraphAsset?test=ExampleAsset&> .\n"+
                        "@prefix cx-part: <urn:cx:part#> .\n"+
                        "@prefix cx-diag: <urn:cx:diagnosis#> .\n"+
                        "@prefix owl: <http://www.w3.org/2002/07/owl#> .\n"+
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n"+
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .\n"+
                        "@prefix sh: <http://www.w3.org/ns/shacl#> .\n"+
                        ":OemDTC rdf:type sh:NodeShape ;\n"+
                        "  sh:targetClass cx-diag:DTC ;\n"+
                        "  sh:property [\n"+
                        "        sh:path cx-diag:provisionedBy ;\n"+
                        "        sh:hasValue <urn:bpn:legal:BPNL00000003COJN> ;\n"+
                        "    ] ;\n"+
                        "  sh:property [\n"+
                        "        sh:path cx-diag:version ;\n"+
                        "        sh:hasValue \"0\"^^xsd:long ;\n    ] ;\n"+
                        "  sh:property [\n"+
                        "        sh:path cx-diag:affects ;\n"+
                        "        sh:class :OemDiagnosedParts ;\n    ] .\n"+
                        ":OemDiagnosedParts rdf:type sh:NodeShape ;\n"+
                        "  sh:targetClass cx-part:Part ;\n"+
                        "  sh:property [\n"+
                        "        sh:path cx-part:provisionedBy ;\n"+
                        "        sh:hasValue <urn:bpn:legal:BPNL00000003COJN> ;\n"+
                        "    ] .\n")
                .property("cx:isFederated","true")
                .build();
        Policy policy = Policy.Builder.newInstance()
                .type(PolicyType.OFFER)
                .extensibleProperties(asset.getProperties())
                .assignee("urn:cx-common#BusinessPartner?test:TestConsumer")
                .assigner("urn:cx-common#BusinessPartner?test:TestProvider")
                .target(asset.getId())
                .build();
        ContractOffer offer = ContractOffer.Builder.newInstance().id(String.valueOf(asset.getProperty("asset:prop:contract")))
                .policy(policy)
                .assetId(asset.getId())
                .providerId("urn:cx-common#BusinessPartner?test:TestProvider")
                .build();
        Collection<Quad> result=synchronizer.convertToQuads(graph, connector, offer);
        assertEquals(1+18+16,result.size(),"Got correct number of quads (1 connector subject and 18 asset subjects + 16 shape triples).");
    }

}
