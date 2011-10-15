package play.templates;

import play.Play;
import play.mvc.Router;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;

public abstract class GTJavaBase1xImpl extends GTJavaBase {

    public GTJavaBase1xImpl(Class<? extends GTGroovyBase> groovyClass, String templatePath) {
        super(groovyClass, templatePath);
    }

    // add extra methods used when resolving actions
    public String __reverseWithCheck_absolute_true(String action) {
        return __reverseWithCheck(action, true);
    }

    public String __reverseWithCheck_absolute_false(String action) {
        return __reverseWithCheck(action, false);
    }

    public static String __reverseWithCheck(String action, boolean absolute) {
        return Router.reverseWithCheck(action, Play.getVirtualFile(action), absolute);
    }

    
}
