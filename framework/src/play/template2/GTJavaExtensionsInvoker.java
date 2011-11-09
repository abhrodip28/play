package play.template2;

import org.apache.commons.lang.reflect.MethodUtils;
import play.template2.exceptions.GTTemplateRuntimeException;

import java.lang.reflect.Method;

public abstract class GTJavaExtensionsInvoker {

    public static Object invoke( Class jeClazz, String methodName, Object object, Object[] args) {

        // first check if methodName is present on object
        Class[] argsTypes = new Class[args.length];
        for (int i=0; i < args.length; i++) {
            Object arg = args[i];
            if ( arg != null) {
                argsTypes[i] = arg.getClass();
            }
        }
        try {

            Method m = MethodUtils.getMatchingAccessibleMethod(object.getClass(), methodName, argsTypes);
            if ( m != null) {
                // object has this method - use it
                return m.invoke(object, args);
            }

            // need new set of params with object as first param
            Class[] jeArgsTypes = new Class[args.length+1];
            Object[] jeArgs = new Object[args.length+1];
            jeArgs[0] = object;
            jeArgsTypes[0] = object.getClass();
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    jeArgs[i+1] = arg;
                    jeArgsTypes[i+1] = arg.getClass();
                }
            }

            // object does not have it - use JavaExtensions
            m = MethodUtils.getMatchingAccessibleMethod(jeClazz, methodName, jeArgsTypes);

            Object result = m.invoke(null, jeArgs);

            return result;

        } catch (Exception e) {
            throw new GTTemplateRuntimeException(e);
        }
    }
}
