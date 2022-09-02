//
// EDC Data Plane Agent Extension Test
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.reasoner.rulesys.impl.BindingVectorMultiSet;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletContext;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;

/**
 * Tests the agent controller
 */
public class TestAgentController {
    
    ConsoleMonitor monitor=new ConsoleMonitor();
    TestConfig config=new TestConfig();
    AgentController agentController=new AgentController(monitor,null,new AgentConfig(monitor,config));

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }
    
    @Mock
    HttpServletRequest request;
 
    @Mock
    HttpServletResponse response;

    @Mock
    ServletContext context;

    ObjectMapper mapper=new ObjectMapper();

    /**
     * execution helper
     * @param method http methos
     * @param query optional query
     * @param asset optional asset name
     * @param accepts determines return representation
     * @param params additional parameters
     * @return response body as string
     */
    protected String testExecute(String method, String query, String asset, String accepts, Map<String,String[]> params) throws IOException {
        when(request.getMethod()).thenReturn(method);
        if(query!=null) {
            params.put("query",new String[] { query });
            when(request.getParameter("query")).thenReturn(query);
        }
        if(asset!=null) {
            params.put("asset",new String[] { asset });
            when(request.getParameter("asset")).thenReturn(asset);
        }
        when(request.getParameterMap()).thenReturn(params);
        when(request.getServletContext()).thenReturn(context);
        when(request.getHeaders("Accept")).thenReturn(Collections.enumeration(List.of(accepts)));
        when(context.getAttribute(Fuseki.attrVerbose)).thenReturn(false);
        when(context.getAttribute(Fuseki.attrOperationRegistry)).thenReturn(agentController.operationRegistry);
        when(context.getAttribute(Fuseki.attrNameRegistry)).thenReturn(agentController.dataAccessPointRegistry);
        ByteArrayOutputStream responseStream=new ByteArrayOutputStream();
        MockServletOutputStream mos=new MockServletOutputStream(responseStream);
        when(response.getOutputStream()).thenReturn(mos);
        agentController.getQuery(request, response, null);
        return new String(responseStream.toByteArray());
    }

    /**
     * test canonical call with fixed binding
     * @throws IOException
     */
    @Test
    public void testFixedQuery() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?what WHERE { VALUES (?what) { (\"42\"^^xsd:int)} }";
        String result=testExecute("GET",query,null,"*/*",new HashMap<>());
        JsonNode root=mapper.readTree(result);
        JsonNode whatBinding0=((ArrayNode) root.get("results").get("bindings")).get(0).get("what");
        assertEquals("42",whatBinding0.get("value").asText(),"Correct binding");
    }

    /**
     * test canonical call with simple replacement binding
     * @throws IOException
     */
    @Test
    public void testParameterizedQuerySingle() throws IOException {
        String query="PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> SELECT ?what WHERE { VALUES (?what) { (\":input\"^^xsd:int)} }";
        Map<String,String[]> params=new HashMap<>();
        params.put("input",new String[] { "84"} );
        String result=testExecute("GET",query,null,"*/*",params);
        JsonNode root=mapper.readTree(result);
        JsonNode whatBinding0=((ArrayNode) root.get("results").get("bindings")).get(0).get("what");
        assertEquals("84",whatBinding0.get("value").asText(),"Correct binding");
    }

}
