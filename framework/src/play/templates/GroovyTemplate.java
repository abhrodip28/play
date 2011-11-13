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
import play.template2.exceptions.GTTemplateRuntimeException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * This class is not removed (after starting to use the new groovy template engine)
 * to not break old fast tags.
 * We cannot remove the ExecutableTemplate
 */
public abstract class GroovyTemplate extends BaseTemplate {

    /**
     * Groovy template
     */
    public static abstract class ExecutableTemplate extends Script {

        // Cannot remove this since it might be referred to when generating exceptions in old fast tags
        public GroovyTemplate template;

    }
}
