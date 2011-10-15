package play.templates;

import groovy.lang.MissingPropertyException;
import play.template2.GTGroovyBase;

public class GTGroovyBase1xImpl extends GTGroovyBase {

    @Override
    public Object getProperty(String property) {
        try {
            if (property.equals("actionBridge")) {
                // special object used to resolving actions
                return new GroovyTemplate.ExecutableTemplate.ActionBridge((String)super.getProperty(GTGroovyBase.__TemplatePath_propertyName));
            }
            return super.getProperty(property);
        } catch (MissingPropertyException mpe) {
            return null;
        }
    }

}
