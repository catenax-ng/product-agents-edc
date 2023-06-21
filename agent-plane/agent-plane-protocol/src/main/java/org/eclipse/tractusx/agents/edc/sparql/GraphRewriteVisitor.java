package org.eclipse.tractusx.agents.edc.sparql;

import org.apache.jena.sparql.algebra.OpVisitorBase;
import org.apache.jena.sparql.algebra.op.OpService;

/**
 * A visitor that indicates stop when inside a service
 */
public class GraphRewriteVisitor extends OpVisitorBase {
    protected boolean inService=false;

    @Override
    public void visit(OpService opService) {
        super.visit(opService);
        inService=true;
    }
}
