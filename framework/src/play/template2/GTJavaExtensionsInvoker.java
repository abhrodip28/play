package play.template2;

import groovy.lang.GroovyObjectSupport;
import org.apache.commons.lang.reflect.MethodUtils;
import play.template2.exceptions.GTTemplateRuntimeException;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class GTJavaExtensionsInvoker {

    private static final Invoker regularArgsInvoker = new RegularArgsInvoker();
    private static final Invoker regularArgsWithObjectAsCollectionInvoker = new WithObjectAsCollectionInvoker(regularArgsInvoker);
    private static final Invoker withRealArgsAsArrayInvoker = new WithRealArgsAsArrayInvoker(regularArgsInvoker);
    private static final Invoker withRealArgsAsArrayAndObjectAsCollectionInvoker = new WithRealArgsAsArrayInvoker(regularArgsWithObjectAsCollectionInvoker);

    static final InvokeExecutor invokerExecutorMethod = new InvokerExecutorMethod();
    static final InvokeExecutor invokeExecutorGroovySupport = new InvokeExecutorGroovySupport();
    static final InvokeExecutor invokerExecutorRealMethod = new InvokerExecutorRealMethod();

    static final RealMethodInvoker realMethodInvoker = new RealMethodInvoker();

    static final Map<InvocationSignatur, InvocationInfo> invocationInfoMap = new HashMap<InvocationSignatur, InvocationInfo>();

    private static final Invoker[] invokers = new Invoker[]{
            regularArgsInvoker,
            regularArgsWithObjectAsCollectionInvoker,
            withRealArgsAsArrayInvoker,
            withRealArgsAsArrayAndObjectAsCollectionInvoker
    };



    static interface Invoker {

        // generates args that should be used when invoking this kind
        public Object[] fixArgs(Object object, Object[] args);
        // finds a method on jeClass that matches this kind
        public Method findMethod(Class jeClazz, String methodName, Object object, Object[] args);
    }



    static class RegularArgsInvoker implements Invoker {
        public Object[] fixArgs(Object object, Object[] args) {
            Object[] jeArgs = new Object[args.length+1];
            jeArgs[0] = object;
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    jeArgs[i+1] = arg;
                }
            }
            return jeArgs;
        }

        public Method findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            Class[] jeArgsTypes = new Class[args.length+1];
            jeArgsTypes[0] = object.getClass();
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    jeArgsTypes[i+1] = arg.getClass();
                }
            }

            Method m = MethodUtils.getMatchingAccessibleMethod(jeClazz, methodName, jeArgsTypes);
            return m;
        }
    }

    static class WithObjectAsCollectionInvoker implements Invoker {

        private final Invoker baseInvoker;

        WithObjectAsCollectionInvoker(Invoker baseInvoker) {
            this.baseInvoker = baseInvoker;
        }

        public Object[] fixArgs(Object object, Object[] args) {
            // create list from object-array
            Collection objectCollection = new ArrayList();
            int arrayLength = Array.getLength(object);
            for ( int i=0; i < arrayLength; i++) {
                objectCollection.add( Array.get(object,i));
            }
            return baseInvoker.fixArgs(objectCollection, args);
        }

        public Method findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            if ( !object.getClass().isArray()) {
                return null;
            }
            return baseInvoker.findMethod(jeClazz, methodName, new ArrayList(0), args);
        }
    }

    static class WithRealArgsAsArrayInvoker implements Invoker {

        private final Invoker baseInvoker;

        WithRealArgsAsArrayInvoker(Invoker baseInvoker) {
            this.baseInvoker = baseInvoker;
        }

        public Object[] fixArgs(Object object, Object[] args) {
            Class arrayType = args[0].getClass();

            // create new args-array with the array-type == the type of the first element in the real args
            Object argsArray = Array.newInstance(arrayType, args.length);
            for ( int i=0; i < args.length; i++) {
                Array.set(argsArray, i, args[i]);
            }

            return baseInvoker.fixArgs(object, new Object[]{argsArray});
        }

        public Method findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            if ( args.length == 0) {
                return null;
            }

            Class arrayType = args[0].getClass();
            // create an empty array to get the type
            Object tmpArray = Array.newInstance(arrayType, 0);
            return baseInvoker.findMethod(jeClazz, methodName, object, new Object[]{tmpArray});
        }
    }

    static class RealMethodInvoker implements Invoker {
        public Object[] fixArgs(Object object, Object[] args) {
            return args;
        }

        public Method findMethod(Class jeClazz, String methodName, Object object, Object[] args) {
            Class[] argsTypes = new Class[args.length];
            for (int i=0; i < args.length; i++) {
                Object arg = args[i];
                if ( arg != null) {
                    argsTypes[i] = arg.getClass();
                }
            }
            return MethodUtils.getMatchingAccessibleMethod(object.getClass(), methodName, argsTypes);
        }
    }

    static interface InvokeExecutor {
        public Object doIt( Method m, String methodName, Object object, Object[] args) throws Exception;
    }


    static class InvokerExecutorMethod implements InvokeExecutor {
        public Object doIt(Method m, String methodName, Object object, Object[] args) throws Exception {
            return m.invoke(null, args);
        }
    }

    static class InvokerExecutorRealMethod implements InvokeExecutor {
        public Object doIt(Method m, String methodName, Object object, Object[] args) throws Exception {
            return m.invoke(object, args);
        }
    }

    static class InvokeExecutorGroovySupport implements InvokeExecutor {

        public Object doIt(Method m, String methodName, Object object, Object[] args) throws Exception {
            // This is a special groovy object - must special case
            GroovyObjectSupport gos = (GroovyObjectSupport)object;
            return gos.invokeMethod(methodName, args);
        }
    }

    static class InvocationSignatur {
        private final String methodName;
        private final Class objectType;
        private final Class[] argTypes;

        InvocationSignatur(String methodName, Object object, Object[] args) {
            this.methodName = methodName;
            this.objectType = object.getClass();
            this.argTypes = new Class[args.length];
            for ( int i=0;i<args.length;i++) {
                Object arg = args[i];
                if (arg!=null) {
                    argTypes[i] = args.getClass();
                }
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InvocationSignatur that = (InvocationSignatur) o;

            if (!Arrays.equals(argTypes, that.argTypes)) return false;
            if (!methodName.equals(that.methodName)) return false;
            if (!objectType.equals(that.objectType)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = methodName.hashCode();
            result = 31 * result + objectType.hashCode();
            result = 31 * result + Arrays.hashCode(argTypes);
            return result;
        }
    }

    static class InvocationInfo {
        public final Method method;
        public final String methodName;
        public final InvokeExecutor invokeExecutor;
        public final Invoker invoker;

        InvocationInfo(Method method, String methodName, InvokeExecutor invokeExecutor, Invoker invoker) {
            this.method = method;
            this.methodName = methodName;
            this.invokeExecutor = invokeExecutor;
            this.invoker = invoker;
        }
    }

    public static Object invoke( Class jeClazz, String methodName, Object object, Object[] args) {

        if ( object == null ) {
            return null;
        }

        try {

            InvocationSignatur invocationSignatur = new InvocationSignatur(methodName, object, args);

            // have we resolved this before?
            InvocationInfo invocationInfo = null;
            synchronized (invocationInfoMap) {
                invocationInfo = invocationInfoMap.get(invocationSignatur);
            }

            if ( invocationInfo != null ) {
                Invoker invoker = invocationInfo.invoker;
                return invocationInfo.invokeExecutor.doIt(invocationInfo.method, invocationInfo.methodName, object, (invoker!=null ? invoker.fixArgs(object, args) : null));
            }


            // start looking for JavaExtension

            // first we look for method with regular args..
            Invoker invoker = null;
            Method m = null;
            InvokeExecutor invokerExecutor = null;
            for ( Invoker _invoker : invokers) {
                m = _invoker.findMethod(jeClazz, methodName, object, args);
                if ( m != null) {
                    invoker = _invoker;
                    invokerExecutor = invokerExecutorMethod;
                    break;
                }
            }

            if ( invokerExecutor == null) {
                if ( object instanceof GroovyObjectSupport) {
                    invokerExecutor = invokeExecutorGroovySupport;
                } else {

                    m = realMethodInvoker.findMethod(jeClazz, methodName, object, args);

                    if (m != null) {
                        invoker = realMethodInvoker;
                        invokerExecutor = invokerExecutorRealMethod;
                    }
                }
            }

            if (invokerExecutor != null) {
                Object res = invokerExecutor.doIt(m, methodName, object, (invoker!=null ? invoker.fixArgs(object, args) : null));
                synchronized (invocationInfoMap) {
                    invocationInfoMap.put(invocationSignatur, new InvocationInfo(m, methodName, invokerExecutor, invoker));
                }
                return res;
            } else {
                throw new NoSuchMethodException(methodName);
            }

        } catch (Exception e) {
            throw new GTTemplateRuntimeException(e);
        }
    }
}
