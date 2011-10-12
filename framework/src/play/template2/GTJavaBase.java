package play.template2;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public abstract class GTJavaBase extends GTRenderingResult {

    // Used by the set-/get-tags
    protected Map<String, String> tag_set_get_store = new HashMap<String, String>();

    // Used by if/elseif/ifnot/else
    // if tlid is present in set, then the else /elseif should run
    protected Set<Integer> runNextElse = new HashSet<Integer>();

    protected StringWriter out;

    protected Script groovyScript = null;
    protected Binding binding;
    private final Class<? extends GTGroovyBase> groovyClass;

    // if this tag uses #{extends}, then the templatePath we extends is stored here.
    public String extendsTemplatePath = null; // default is not to extend anything...
    public GTJavaBase extendedTemplate = null;
    public GTJavaBase extendingTemplate = null; // if someone is extending us, this is the ref to their rendered template - used when dumping their output

    protected GTTemplateRepo templateRepo;

    // TagLevelID - when the runtime enters a new "tag-level" (Think: indent),it sets a unique value to
    // this variable. When the runtime leaves this level, it restores the previous value.
    // TagLevelID can therefor be used as a key when you need to store info between tags in same level - eg: if/else/elseif etc
    protected int tlid = -1;

    public GTJavaBase(Class<? extends GTGroovyBase> groovyClass ) {
        this.groovyClass = groovyClass;
        initNewOut();

    }

    public void writeOutput(OutputStream os, String encoding) {
        for ( StringWriter s : allOuts) {
            try {
                os.write(s.getBuffer().toString().getBytes(encoding));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void writeOutput(PrintStream ps, String encoding) {
        // if we have extended another template, we must pass this on to this template-instance,
        // because "it" has all the output
        if (extendedTemplate != null) {
            extendedTemplate.writeOutput(ps, encoding);
            return ;
        }

        for ( StringWriter s : allOuts) {
            try {
                ps.write(s.getBuffer().toString().getBytes(encoding));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void insertOutput(GTRenderingResult otherTemplate) {
        allOuts.addAll( otherTemplate.allOuts);
        initNewOut();
    }

    protected void insertNewOut( StringWriter outToInsert) {
        allOuts.add(outToInsert);
        initNewOut();
    }

    protected void initNewOut() {
        // must create new live out
        out = new StringWriter();
        allOuts.add(out);
    }

    protected void renderTemplate(Map<String, Object> args) {
        this.binding = new Binding(args);
        // must init our groovy script
        groovyScript = InvokerHelper.createScript(groovyClass, binding);
        _renderTemplate();

        // check if "we" have extended an other template..
        if (extendsTemplatePath != null) {
            // yes, we've extended another template
            // Get the template we are extending
            extendedTemplate = templateRepo.getTemplateInstance( extendsTemplatePath);

            // tell it that "we" extended it..
            extendedTemplate.extendingTemplate = this;

            // ok, render it..
            extendedTemplate.renderTemplate( args);
        }
    }

    protected abstract void _renderTemplate();
    
}
