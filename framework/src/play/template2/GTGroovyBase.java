package play.template2;

import groovy.lang.MissingPropertyException;
import groovy.lang.Script;

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
        throw new RuntimeException("Not supposed to run this method");
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
}
