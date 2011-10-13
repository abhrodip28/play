package play.template2.compile;

import play.template2.GTContentRenderer;
import play.template2.GTFastTagResolver;
import play.template2.GTJavaBase;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
}
