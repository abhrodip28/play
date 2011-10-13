package play.templates;

import groovy.lang.Closure;
import play.template2.GTJavaBase;
import play.template2.legacy.GTLegacyFastTagResolver;

import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

public class GTLegacyFastTagResolver1X implements GTLegacyFastTagResolver {

    static Class fc = FastTags.class;

    public String resolveFastTag(String tagName) {


        if (findTagMethod(tagName) == null) {
            return null;
        }

        return GTLegacyFastTagResolver1X.class.getName() + ".legacyFastTagBridge";
    }

    public static void legacyFastTagBridge(String tagName, GTJavaBase template, Map<String, Object> args, Closure body ) {
        Method m = findTagMethod(tagName);
        if (m == null) {
            throw new RuntimeException("Did not find the legacy fastTag method for " + tagName );
        }

        PrintWriter out = new PrintWriter( template.out );
        GroovyTemplate.ExecutableTemplate executableTemplate = null;
        int fromLine = 0;

        try {
            m.invoke(null, args, body, out, executableTemplate, fromLine);
        } catch (Exception e) {
            throw new RuntimeException("Error when executing lecacy fastTag " + tagName, e);
        }
    }

    private static Method findTagMethod(String tagName) {
        // look for a method named _tagName using the old 1.x fasttag-format
        Method m;
        try {
            m = fc.getMethod("_"+tagName,Map.class, Closure.class, PrintWriter.class, GroovyTemplate.ExecutableTemplate.class, Integer.TYPE);
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new RuntimeException("A fast-tag method must be static: " + m);
            }
        } catch (Exception e) {
            // not found
            return null;
        }
        return m;
    }
}
