//
// EDC Data Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc.sparql;

import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.algebra.optimize.OptimizerStd;
import org.apache.jena.sparql.util.Context;

/**
 * an modified standard optimization strategy which deals with federation and binding
 * of federation-important sparql constructs better at the level of joins
 */
public class Optimizer extends OptimizerStd {
    /**
     * Create a new optimizer
     * @param context query context
     */
    public Optimizer(Context context) {
        super(context);
    }

    /**
     * override to choose the improved join straregy
     * @param op operator to transform
     * @return transformed operator
     */
    @Override
    protected Op transformJoinStrategy(Op op) {
        return apply("Federated Index Join strategy", new OptimizeJoinStrategy(), op);
    }

}
