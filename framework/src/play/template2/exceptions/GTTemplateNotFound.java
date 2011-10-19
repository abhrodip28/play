package play.template2.exceptions;

public class GTTemplateNotFound extends GTException {

    public final String templatePath;

    public GTTemplateNotFound(String templatePath) {
        super("Cannot find template file " + templatePath);
        this.templatePath = templatePath;
    }
}
