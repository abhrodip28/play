package play.mvc.results;

import play.exceptions.UnexpectedException;
import play.libs.MimeTypes;
import play.mvc.Http;
import play.template2.GTJavaBase;

import java.util.Map;

public class RenderTemplateGT extends Result {

    private final GTJavaBase template;

    public RenderTemplateGT(GTJavaBase template, Map<String, Object> args) {
        this.template = template;
        long start = System.currentTimeMillis();
        template.renderTemplate( args);
        long end = System.currentTimeMillis();
        long diff = end-start;
        System.out.println("render time: " + diff + " mills");
    }

    public void apply(Http.Request request, Http.Response response) {
        try {
            final String name = template.templateLocation.queryPath;
            final String contentType = MimeTypes.getContentType(name, "text/plain");

            template.writeOutput(response.out, getEncoding());

            setContentTypeIfNotSet(response, contentType);
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    public String getContent() {
        throw new RuntimeException("Not impl");
    }
}
