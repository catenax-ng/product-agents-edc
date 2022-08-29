package io.catenax.knowledge.dataspace.edc;

import java.lang.reflect.*;

/**
 * An invocation handler which maps all jakarta objects
 * to a javax.servlet level
 */
public class JakartaWrapper implements InvocationHandler {
    
    Object jakartaDelegate;

    public JakartaWrapper(Object jakartaDelegate) {
        this.jakartaDelegate=jakartaDelegate;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class[] types=method.getParameterTypes();
        if(args==null) args=new Object[0];
        for(int count=0;count<args.length;count++) {
            if(types[count].getCanonicalName().startsWith("javax.servlet") && args[count]!=null) {
                // we assume its a proxy
                JakartaWrapper wrapper=(JakartaWrapper) Proxy.getInvocationHandler(args[count]);
                args[count]=wrapper.jakartaDelegate;
                Class jakartaClass=JakartaWrapper.class.getClassLoader().loadClass(types[count].getCanonicalName().replace("javax.servlet","jakarta.servlet"));
                types[count]=jakartaClass;
            }
        }
        Method targetMethod=jakartaDelegate.getClass().getMethod(method.getName(),types);
        Object result=targetMethod.invoke(jakartaDelegate,args);
        if((!method.getReturnType().isAssignableFrom(targetMethod.getReturnType())) && result!=null) {
            result=javaxify(result,method.getReturnType());
        }
        return result;
    }

    public static <Target> Target javaxify(Object jakarta, Class<Target> javaxClass) {
        return (Target) Proxy.newProxyInstance(JakartaWrapper.class.getClassLoader(),
            new Class[] { javaxClass },
            new JakartaWrapper(jakarta));
    }

}
