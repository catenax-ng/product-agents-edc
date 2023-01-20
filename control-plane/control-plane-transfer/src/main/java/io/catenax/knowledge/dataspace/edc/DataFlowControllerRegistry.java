//
// EDC Control Plane Agent Extension
// See copyright notice in the top folder
// See authors file in the top folder
// See license file in the top folder
//
package io.catenax.knowledge.dataspace.edc;

import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowController;
import org.eclipse.dataspaceconnector.spi.transfer.flow.DataFlowManager;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;

import java.util.List;

/**
 * A helper to ensure the right priority
 * for data flow controllers as long as the core
 * controllers are implemented "suboptimally"
 */
public class DataFlowControllerRegistry {

    /** its a hidden thingy in the manager */
    protected static java.lang.reflect.Field controllersField=null;

    /** open it up */
    static {
        try {
            controllersField= DataFlowManager.class.getClassLoader().loadClass("org.eclipse.dataspaceconnector.transfer.core.flow.DataFlowManagerImpl").getDeclaredField("controllers");
            controllersField.trySetAccessible();
        } catch(SecurityException | NoSuchFieldException | ClassNotFoundException e) {
        }
    }

    /**
     * register the given controll with priority if possible
     * @param manager to register at
     * @param controller to register
     */
    public static void registerWithPriority(DataFlowManager manager, DataFlowController controller) {
        if(controllersField!=null) {
            try {
                ((List<DataFlowController>)controllersField.get(manager)).add(0,controller);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        } else {
            manager.register(controller);
        }
    }
}
