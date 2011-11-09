package play.template2;

import groovy.lang.GroovyObjectSupport;
import org.apache.commons.lang.reflect.MethodUtils;
import play.template2.exceptions.GTTemplateRuntimeException;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public abstract class GTJavaExtensionsInvoker {

    public static Object invoke( Class jeClazz, String methodName, Object object, Object[] args) {

        if ( object == null ) {
            return null;
        }

        try {

            // start looking for JavaExtension

            // first we look for method with regular args..

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
            Method m = MethodUtils.getMatchingAccessibleMethod(jeClazz, methodName, jeArgsTypes);

            if ( m != null) {
                Object result = m.invoke(null, jeArgs);
                return result;
            }

            // is object array?
            if ( object.getClass().isArray()) {
                // try with collection
                jeArgsTypes[0] = Collection.class;
                m = MethodUtils.getMatchingAccessibleMethod(jeClazz, methodName, jeArgsTypes);
                if (m != null) {
                    // create list from object-array
                    Collection objectCollection = new ArrayList();
                    int arrayLength = Array.getLength(object);
                    for ( int i=0; i < arrayLength; i++) {
                        objectCollection.add( Array.get(object,i));
                    }

                    jeArgs[0] = objectCollection;

                    Object result = m.invoke(null, jeArgs);
                    return result;
                }
            }


            // now we look for method with real args as array
            if ( args.length > 0) {
                jeArgsTypes = new Class[2];
                jeArgs = new Object[2];
                jeArgs[0] = object;
                jeArgsTypes[0] = object.getClass();

                Class arrayType = args[0].getClass();

                // create new args-array with the array-type == the type of the first element in the real args
                Object argsArray = Array.newInstance(arrayType, args.length);
                for ( int i=0; i < args.length; i++) {
                    Array.set(argsArray, i, args[i]);
                }

                jeArgsTypes[1] = argsArray.getClass(); // create array-type of the same type as the first real arg

                // set real args-array as second param with correct array-type
                jeArgs[1] = argsArray;

                // object does not have it - use JavaExtensions
                m = MethodUtils.getMatchingAccessibleMethod(jeClazz, methodName, jeArgsTypes);

                if ( m != null) {
                    Object result = m.invoke(null, jeArgs);
                    return result;
                }
            }

            // did not find JavaExtension
            // Look for regular method


            if ( object instanceof GroovyObjectSupport) {
                // This is a special groovy object - must special case
                GroovyObjectSupport gos = (GroovyObjectSupport)object;
                return gos.invokeMethod(methodName, args);
            }

            Class[] argsTypes = new Class[args.length];
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    argsTypes[i] = arg.getClass();
                }
            }

            m = MethodUtils.getMatchingAccessibleMethod(object.getClass(), methodName, argsTypes);
            if ( m != null) {
                // object has this method - use it
                return m.invoke(object, args);
            }

            throw new NoSuchMethodException(methodName);

        } catch (Exception e) {
            throw new GTTemplateRuntimeException(e);
        }
    }
}
