package play.template2;

import groovy.lang.MissingPropertyException;
import groovy.lang.Script;
import play.template2.exceptions.GTException;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/11/11
 * Time: 8:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTGroovyBase extends Script {

    @Override
    public Object run() {
        throw new GTException("This method must be overridden in generated groovy script");
    }

    /**
     * All first-level property resolving is done through here
     */
    @Override
    public Object getProperty(String property) {
        try {
            return super.getProperty(property);
        } catch (MissingPropertyException mpe) {
            // Just return null if not found
            return null;
        }
    }

    // Method used by groovy scripts to convert from string to class
    // Must lookup the class in the current live classloader - Must return null if not found
    public Class _(String clazzName) {
        throw new GTException("Not implemented by default. Must be overridden by framework impl");
    }
}
