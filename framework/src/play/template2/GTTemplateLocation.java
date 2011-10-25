package play.template2;

import java.io.File;

public class GTTemplateLocation {

    /**
     * as used when loaded
     */
    public final String queryPath;

    /**
     * correct relative path in the app-context
     */
    public final String relativePath;

    public GTTemplateLocation(String queryPath, String relativePath) {
        this.queryPath = queryPath;
        this.relativePath = relativePath;
    }
}
