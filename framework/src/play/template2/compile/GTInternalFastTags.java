package play.template2.compile;

import play.mvc.Http;
import play.template2.GTContentRenderer;
import play.template2.GTFastTagResolver;
import play.template2.GTJavaBase;
import play.template2.exceptions.GTTemplateRuntimeException;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/12/11
 * Time: 11:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTInternalFastTags implements GTFastTagResolver {


    public String resolveFastTag(String tagName) {
        // Look for static methods in this class with the name "tag_tagName"
        try {
            Method m = getClass().getMethod("tag_"+tagName,GTJavaBase.class, Map.class, GTContentRenderer.class);
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new RuntimeException("A fast-tag method must be static: " + m);
            }
        } catch( NoSuchMethodException e) {
            // not found
            return null;
        }

        return getClass().getName() + ".tag_" + tagName;
    }

    public static void tag_testFastTag(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        template.out.append("[testFastTag before]");
        template.insertOutput( content.render());
        template.out.append("[from testFastTag after]");
    }

    public static void tag_get(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {

        String key = args.get("arg").toString();
        if ( key == null) {
            throw new GTTemplateRuntimeException("Specify a variable name when using #{get/}");
        }

        // we must get from the template that extended us.
        if ( template.extendingTemplate == null) {
            return ;
        }


        String value = (String)GTJavaBase.layoutData.get().get(key);

        if (value != null) {
            template.out.append(value);
        }

    }

    public static void tag_set(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        String key = null;
        String value = null;
        // Simple case : #{set title:'Yop' /}

        for ( String k : args.keySet()) {
            if ( !"arg".equals(k)) {
                key = k;
                Object v = args.get(key);

                value = template.objectToString( v);
                break;
            }
        }

        if ( key == null) {
            // Body case
            key = args.get("arg").toString();
            // render content to string
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            String encoding = Http.Response.current().encoding;
            content.render().writeOutput(out, encoding);
            try {
                value = out.toString(encoding);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }

        if ( key != null ) {
            GTJavaBase.layoutData.get().put(key, value);
        }
    }

    public static void tag_ifErrors(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        if ( template.validationHasErrors()) {
            template.clearElseFlag();
            template.insertOutput( content.render());
        } else {
            // Must set the else-condition
            template.setElseFlag();
        }
    }

    public static void tag_ifError(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        Object key = args.get("arg");
        if (key==null) {
            throw new GTTemplateRuntimeException("Please specify the error key");
        }
        if ( template.validationHasError(key.toString())) {
            template.clearElseFlag();
            template.insertOutput( content.render());
        } else {
            // Must set the else-condition
            template.setElseFlag();
        }
    }

    public static void tag_include(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {
        if (!args.containsKey("arg") || args.get("arg") == null) {
            throw new GTTemplateRuntimeException("Specify a template name");
        }
        String name = args.get("arg").toString();
        if (name.startsWith("./")) {
            String ct = template.templateLocation.queryPath;
            if (ct.matches("^/lib/[^/]+/app/views/.*")) {
                ct = ct.substring(ct.indexOf("/", 5));
            }
            ct = ct.substring(0, ct.lastIndexOf("/"));
            name = ct + name.substring(1);
        }

        GTJavaBase newTemplate = template.templateRepo.getTemplateInstance( name);
        Map<String, Object> newArgs = new HashMap<String, Object>();
        newArgs.putAll(args);
        newArgs.put("_isInclude", true);

        newTemplate.renderTemplate(newArgs);
        template.insertOutput( newTemplate );
    }

    public static void tag_doBody(GTJavaBase template, Map<String, Object> args, GTContentRenderer _content ) {

        // the content we're supposed to output here is the body-content inside the tag we're now in.
        // we must not output the body of the doBody-tag it self.
        // output this: template.contentRenderer


        // if we have an arg named "vars" which is a map, then
        // we should inject the key->values in var into args to body.
        // if the org value of the key, is null, we should restore the value after we have rendered.
        Map<String, Object> vars = (Map<String, Object>)args.get("vars");

        Set<String> propertiesToResetToNull = new HashSet<String>();

        if ( vars != null) {
            for (Map.Entry<String, Object> e : vars.entrySet()) {
                String key = e.getKey();
                if ( template.contentRenderer.getRuntimeProperty(key) == null ) {
                    // this one should reseted after rendering
                    propertiesToResetToNull.add( key);
                }
                // set the value
                template.contentRenderer.setRuntimeProperty(key, e.getValue());
            }
        }

        String as = (String)args.get("as");


        if ( as == null ) {
            // render body right now
            template.insertOutput(template.contentRenderer.render());
        } else {
            // render body to string and store it with the name in as
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            template.contentRenderer.render().writeOutput(out, "utf-8");
            String contentString;
            try {
                contentString = out.toString("utf-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            template.binding.setProperty(as, contentString);

        }

        // do we have anything to reset?
        for ( String key : propertiesToResetToNull) {
            template.contentRenderer.setRuntimeProperty(key, null);
        }

    }

    


}
