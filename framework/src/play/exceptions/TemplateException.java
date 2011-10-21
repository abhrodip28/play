package play.exceptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import play.templates.Template;

/**
 * An exception during template execution
 */
public abstract class TemplateException extends PlayException implements SourceAttachment {

    private Template template;
    private Integer lineNumber;

    public TemplateException(Template template, Integer lineNumber, String message) {
        super(message);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public TemplateException(Template template, Integer lineNumber, String message, Throwable cause) {
        super(message, cause);
        this.template = template;
        this.lineNumber = lineNumber;
    }

    public Template getTemplate() {
        return template;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public List<String> getSource() {
        if ( template != null) {
            return Arrays.asList(template.source.split("\n"));
        } else {
            // return fake source
            List<String> lines = new ArrayList<String>();
            lines.add( "");
            return lines;
        }

    }

    public String getSourceFile() {
        return (template == null ? "unknown source file" : template.name);
    }
}
