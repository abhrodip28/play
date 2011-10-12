package play.template2;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.OutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public abstract class GTJavaBase {

    // Used by the set-/get-tags
    protected Map<String, String> tag_set_get_store = new HashMap<String, String>();

    // Used by if/elseif/ifnot/else
    // if tlid is present in set, then the else /elseif should run
    protected Set<Integer> runNextElse = new HashSet<Integer>();

    protected StringWriter out;
    protected List<StringWriter> allOuts = new ArrayList<StringWriter>();

    protected Script groovyScript = null;
    protected Binding binding;
    private final Class<? extends GTGroovyBase> groovyClass;

    // TagLevelID - when the runtime enters a new "tag-level" (Think: indent),it sets a unique value to
    // this variable. When the runtime leaves this level, it restores the previous value.
    // TagLevelID can therefor be used as a key when you need to store info between tags in same level - eg: if/else/elseif etc
    protected int tlid = -1;

    public GTJavaBase(Class<? extends GTGroovyBase> groovyClass) {
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

    protected void insertNewOut( StringWriter outToInsert) {
        allOuts.add(outToInsert);
        initNewOut();
    }

    private void initNewOut() {
        // must create new live out
        out = new StringWriter();
        allOuts.add(out);
    }

    public void renderTemplate(Map<String, Object> args) {
        this.binding = new Binding(args);
        // must init our groovy script
        groovyScript = InvokerHelper.createScript(groovyClass, binding);
        _renderTemplate();
    }

    protected abstract void _renderTemplate();
    
}
