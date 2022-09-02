//
// EDC Data Plane Agent Extension Test
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc;

import org.junit.jupiter.api.Test;
import org.eclipse.dataspaceconnector.spi.monitor.ConsoleMonitor;

/**
 * Tests the agent controller
 */
public class TestAgentController {
    
    ConsoleMonitor monitor=new ConsoleMonitor();
    TestConfig config=new TestConfig();
    AgentController agentController=new AgentController(monitor,null,new AgentConfig(monitor,config));

    @Test
    public void testParameterizedQuery() {
    }

}
