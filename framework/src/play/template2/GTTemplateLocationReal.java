package play.template2;

import java.io.File;

public class GTTemplateLocationReal extends GTTemplateLocation {

    public final File realFile;

    public GTTemplateLocationReal(String queryPath, String relativePath, File realFile) {
        super(queryPath, relativePath);
        this.realFile = realFile;
    }
}
