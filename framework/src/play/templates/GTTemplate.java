package play.templates;

import play.Play;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateExecutionException;
import play.exceptions.TemplateNotFoundException;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.Http;
import play.template2.GTJavaBase;
import play.template2.GTRenderingResult;
import play.template2.GTTemplateLocation;
import play.template2.GTTemplateLocationReal;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;
import play.template2.exceptions.GTRuntimeExceptionWithSourceInfo;
import play.template2.exceptions.GTTemplateNotFoundWithSourceInfo;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.concurrent.Callable;

public class GTTemplate extends Template{

    private final GTTemplateLocation templateLocation;

    public GTTemplate(GTTemplateLocation templateLocation) {
        this.templateLocation = templateLocation;
        this.name = templateLocation.relativePath;
    }

    public GTTemplate(String name) {
        this.templateLocation = null;
        this.name = name;
    }

    @Override
    public void compile() {
        throw new RuntimeException("not implemented");
    }

    @Override
    protected String internalRender(Map<String, Object> args) {


        Http.Request currentResponse = Http.Request.current();
        if ( currentResponse != null) {
            args.put("_response_encoding", currentResponse.encoding);
        }
        args.put("play", new Play());
        args.put("messages", new Messages());
        args.put("lang", Lang.get());

        GTRenderingResult renderingResult = renderGTTemplate(args);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderingResult.writeOutput(out, "utf-8");

        try {
            return new String(out.toByteArray(), "utf-8");
        } catch ( UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected GTJavaBase getGTTemplateInstance() {
        return TemplateLoader.getGTTemplateInstance((GTTemplateLocationReal)templateLocation);
    }

    protected GTRenderingResult renderGTTemplate(Map<String, Object> args) {

        try {

            GTJavaBase gtTemplate = getGTTemplateInstance();
            gtTemplate.renderTemplate(args);
            return gtTemplate;

        } catch ( GTTemplateNotFoundWithSourceInfo e) {
            GTTemplate t = new GTTemplate(e.templateLocation);
            t.loadSource();
            throw new TemplateNotFoundException(e.queryPath, t, e.lineNo);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            GTTemplate t = new GTTemplate(e.templateLocation);
            t.loadSource();
            throw new TemplateCompilationException( t, e.oneBasedLineNo, e.specialMessage);
        } catch (GTRuntimeExceptionWithSourceInfo e){
            GTTemplate t = new GTTemplate(e.templateLocation);
            t.loadSource();
            throw new TemplateExecutionException( t, e.lineNo, e.getMessage(), e);
        }

    }

    @Override
    public String render(Map<String, Object> args) {
        return internalRender(args);
    }
    
    public void loadSource() {
        if ( source == null) {
            source = templateLocation.readSource();
        }
    }
}
