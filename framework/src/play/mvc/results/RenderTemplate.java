package play.mvc.results;

import play.exceptions.TemplateExecutionException;
import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http.Request;
import play.mvc.Http.Response;
import play.template2.GTRenderingResult;
import play.template2.exceptions.GTRuntimeException;
import play.templates.GTTemplate;
import play.templates.Template;

import java.util.Map;

/**
 * 200 OK with a template rendering
 */
public class RenderTemplate extends Result {

    private final String name;
    private final String content;
    private final GTRenderingResult renderingResult;

    public RenderTemplate(Template template, Map<String, Object> args) {
        this.name = template.name;
        if (args.containsKey("out")) {
            throw new RuntimeException("Assertion failed! args shouldn't contain out");
        }
        if ( template instanceof GTTemplate) {
            // render it without storing it in string..
            GTTemplate gtt = (GTTemplate)template;
            renderingResult = gtt.internalGTRender(args);
            this.content = null;
        } else {
            this.content = template.render(args);
            this.renderingResult = null;
        }
    }

    public void apply(Request request, Response response) {
        try {
            final String contentType = MimeTypes.getContentType(name, "text/plain");
            if ( this.renderingResult != null) {
                this.renderingResult.writeOutput(response.out, getEncoding());
            } else {
                response.out.write(content.getBytes(getEncoding()));
            }
            setContentTypeIfNotSet(response, contentType);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getContent() {
        return content;
    }

}
