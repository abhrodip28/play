package play.templates;

import play.template2.GTTagContext;

import java.util.Map;

/**
 * Tag Context (retrieve who call you)
 * Exists to be compatible with GTTagContext
 */
public class TagContext {
    
    public final String tagName;
    public final Map<String, Object> data;

    protected TagContext(String tagName, Map<String, Object> data) {
        this.tagName = tagName;
        this.data = data;
    }

    public static void init() {
        GTTagContext.init();
    }

    public static void enterTag(String name) {
        GTTagContext.enterTag(name);
    }
    
    public static void exitTag() {
        GTTagContext.exitTag();
    }

    private static TagContext createWrapper(GTTagContext tc) {
        if ( tc == null) {
            return null;
        }
        return new TagContext(tc.tagName, tc.data);
    }
    
    public static TagContext current() {
        return createWrapper(GTTagContext.current());
    }
    
    public static TagContext parent() {
        return createWrapper(GTTagContext.parent());
    }
    
    public static boolean hasParentTag(String name) {
        return GTTagContext.hasParentTag(name);
    }
    
    public static TagContext parent(String name) {
        return createWrapper(GTTagContext.parent(name));
    }

    @Override
    public String toString() {
        return tagName+""+data;
    }



}
