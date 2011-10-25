package play.templates.gt_integration;

import groovy.lang.MissingPropertyException;
import play.Play;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.templates.GroovyTemplate;

public class GTGroovyBase1xImpl extends GTGroovyBase {

    @Override
    public Object getProperty(String property) {
        try {
            if (property.equals("actionBridge")) {
                // special object used to resolving actions
                GTJavaBase template = (GTJavaBase)super.getProperty("java_class");
                return new GroovyTemplate.ExecutableTemplate.ActionBridge(template.templateLocation.queryPath);
            }
            return super.getProperty(property);
        } catch (MissingPropertyException mpe) {
            return null;
        }
    }

    @Override
    public Class _(String clazzName) {
        try {
            return Play.classloader.loadClass(clazzName);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
