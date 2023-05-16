//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc.sparql;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.TransformSingle;
import org.apache.jena.sparql.algebra.op.OpExtend;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.sparql.expr.ExprVar;
import org.apache.jena.sparql.expr.NodeValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * a pseudo transform which visits every graph node
 * to list all variables. helps us to
 * only serialize the needed portion from
 * consumer to producer
 */
public class SkillVariableDetector extends TransformSingle {

    HashMap<String,Node> variables=new HashMap<>();
    Set<String> allowed;

    public SkillVariableDetector(Set<String> allowed) {
        this.allowed=allowed;
    }

    @Override
    public Op transform(OpExtend opExtend, Op subOp) {
        opExtend.getVarExprList().forEachExpr( (assignment,expr) -> {
            String varName= assignment.getVarName();
            if(!variables.containsKey(varName)) {
                if (expr.isVariable()) {
                    Var var = (Var) ((ExprVar) expr).getAsNode();
                    if (allowed.contains(var.getVarName())) {
                        variables.put(varName, var);
                    }
                } else if (expr.isConstant()) {
                    Node node = ((NodeValue) expr).getNode();
                    variables.put(varName, node);
                }
            }
        });
        return opExtend;
    }

    public Map<String,Node> getVariables() {
        return variables;
    }
}
