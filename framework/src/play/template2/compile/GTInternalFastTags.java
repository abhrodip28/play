package play.template2.compile;

import play.exceptions.TagInternalException;
import play.exceptions.TemplateExecutionException;
import play.mvc.Http;
import play.template2.GTContentRenderer;
import play.template2.GTFastTagResolver;
import play.template2.GTJavaBase;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

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

    private static final String set_get_key = "__internal_set_get_map";

    public static void tag_get(GTJavaBase template, Map<String, Object> args, GTContentRenderer content ) {

        String key = args.get("arg").toString();
        if ( key == null) {
            throw new RuntimeException("Specify a variable name when using #{get/}");
        }

        // we must get from the template that extended us.
        if ( template.extendingTemplate == null) {
            return ;
        }

        Map<String, String> set_get_map = (Map<String, String>)template.customData.get(set_get_key);

        if ( set_get_map == null) {
            return ;
        }

        String value = set_get_map.get(key);

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
            Map<String, String> set_get_map = (Map<String, String>)template.customData.get(set_get_key);
            if ( set_get_map == null ) {
                // first time - create it
                set_get_map = new HashMap<String, String>(16);
                template.customData.put(set_get_key, set_get_map);
            }
            set_get_map.put(key, value);
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
            throw new RuntimeException("Please specify the error key");
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
            throw new RuntimeException("Specify a template name");
        }
        String name = args.get("arg").toString();
        if (name.startsWith("./")) {
            String ct = template.templatePath;
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

}
