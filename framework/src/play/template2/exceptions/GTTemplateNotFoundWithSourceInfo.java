package play.template2.exceptions;

import java.io.File;

public class GTTemplateNotFoundWithSourceInfo extends GTTemplateNotFound{
    public final File srcFile;
    public final int lineNo;
    
    public GTTemplateNotFoundWithSourceInfo(String templatePath, File srcFile, int lineNo) {
        super(templatePath);
        this.srcFile = srcFile;
        this.lineNo = lineNo;
    }
}
