//
// EDC Data Plane Agent Extension 
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc;

import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.SPARQL_QueryGeneral;

import java.util.Map;

/**
 * dedicate query processor which is skill-enabled
 */
public class SparqlQueryProcessor extends SPARQL_QueryGeneral.SPARQL_QueryProc {
     
    /**
     * execute GET-style
     * @param action typically a GET request
     */
    @Override
    protected void executeWithParameter(HttpAction action) {
        String queryString = ((AgentHttpAction) action).getSkill();
        if(queryString==null) {
            super.executeWithParameter(action);
        } else {
            execute(queryString, action);
        }
    }

    /**
     * execute POST-style
     * @param action typically a POST request
     */
    @Override
    protected void executeBody(HttpAction action) {
        String queryString = ((AgentHttpAction) action).getSkill();
        if(queryString==null) {
            super.executeBody(action);
        } else {
            execute(queryString, action);
        }
    }

    /**
     * general query execution
     * @param queryString
     * @param action
     */
    @Override
    protected void execute(String queryString, HttpAction action) {
        // parameter replacement        
        for(Map.Entry<String,String[]> param : action.getRequest().getParameterMap().entrySet()) {
            queryString=queryString.replace(":"+param.getKey(),param.getValue()[0]);
        }
        super.execute(queryString,action);
    }
}
