package play.templates;

import groovy.lang.GroovyObjectSupport;
import groovy.lang.Script;
import play.classloading.enhancers.LocalvariablesNamesEnhancer.LocalVariablesNamesTracer;
import play.data.binding.Unbinder;
import play.exceptions.ActionNotFoundException;
import play.exceptions.NoRouteFoundException;
import play.exceptions.PlayException;
import play.exceptions.UnexpectedException;
import play.mvc.ActionInvoker;
import play.mvc.Http.Request;
import play.mvc.Router;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * A template
 */
public abstract class GroovyTemplate extends BaseTemplate {


    protected GroovyTemplate(String source) {
        super(source);
    }

    /**
     * Groovy template
     */
    public static abstract class ExecutableTemplate extends Script {

        // Leave this field public to allow custom creation of TemplateExecutionException from different pkg
        public GroovyTemplate template;

        public static class ActionBridge extends GroovyObjectSupport {

            String templateName = null;
            String controller = null;
            boolean absolute = false;

            public ActionBridge(String templateName, String controllerPart, boolean absolute) {
                this.templateName = templateName;
                this.controller = controllerPart;
                this.absolute = absolute;
            }

            public ActionBridge(String templateName) {
                this.templateName = templateName;
            }

            @Override
            public Object getProperty(String property) {
                return new ActionBridge(templateName, controller == null ? property : controller + "." + property, absolute);
            }

            public Object _abs() {
                this.absolute = true;
                return this;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object invokeMethod(String name, Object param) {
                try {
                    if (controller == null) {
                        controller = Request.current().controller;
                    }
                    String action = controller + "." + name;
                    if (action.endsWith(".call")) {
                        action = action.substring(0, action.length() - 5);
                    }
                    try {
                        Map<String, Object> r = new HashMap<String, Object>();
                        Method actionMethod = (Method) ActionInvoker.getActionMethod(action)[1];
                        String[] names = (String[]) actionMethod.getDeclaringClass().getDeclaredField("$" + actionMethod.getName() + LocalVariablesNamesTracer.computeMethodHash(actionMethod.getParameterTypes())).get(null);
                        if (param instanceof Object[]) {
                            if(((Object[])param).length == 1 && ((Object[])param)[0] instanceof Map) {
                                r = (Map<String,Object>)((Object[])param)[0];
                            } else {
                                // too many parameters versus action, possibly a developer error. we must warn him.
                                if (names.length < ((Object[]) param).length) {
                                    throw new NoRouteFoundException(action, null);
                                }
                                for (int i = 0; i < ((Object[]) param).length; i++) {
                                    if (((Object[]) param)[i] instanceof Router.ActionDefinition && ((Object[]) param)[i] != null) {
                                        Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "", actionMethod.getAnnotations());
                                    } else if (isSimpleParam(actionMethod.getParameterTypes()[i])) {
                                        if (((Object[]) param)[i] != null) {
                                            Unbinder.unBind(r, ((Object[]) param)[i].toString(), i < names.length ? names[i] : "", actionMethod.getAnnotations());
                                        }
                                    } else {
                                        Unbinder.unBind(r, ((Object[]) param)[i], i < names.length ? names[i] : "", actionMethod.getAnnotations());
                                    }
                                }
                            }
                        }
                        Router.ActionDefinition def = Router.reverse(action, r);
                        if (absolute) {
                            def.absolute();
                        }
                        if (templateName.endsWith(".xml")) {
                            def.url = def.url.replace("&", "&amp;");
                        }
                        return def;
                    } catch (ActionNotFoundException e) {
                        throw new NoRouteFoundException(action, null);
                    }
                } catch (Exception e) {
                    if (e instanceof PlayException) {
                        throw (PlayException) e;
                    }
                    throw new UnexpectedException(e);
                }
            }
        }
    }

    static boolean isSimpleParam(Class type) {
        return Number.class.isAssignableFrom(type) || type.equals(String.class) || type.isPrimitive();
    }
}
