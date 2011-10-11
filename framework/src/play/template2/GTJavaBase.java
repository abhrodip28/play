package play.template2;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GTJavaBase {

    // Used by the set-/get-tags
    protected Map<String, String> tag_set_get_store = new HashMap<String, String>();

    protected StringWriter out;
    protected List<StringWriter> allOuts = new ArrayList<StringWriter>();

    protected final Class<? extends GTGroovyBase> groovyClass;
    protected Script groovyScript = null;
    protected final Binding binding;

    public GTJavaBase(Class<? extends GTGroovyBase> groovyClass, Map<String, Object> args) {
        this.groovyClass = groovyClass;
        this.binding = new Binding(args);
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
        allOuts.add( out );
    }

    protected Object invokeGroovy(String methodName) {
        if (groovyScript == null) {
            // must init our groovy script
            groovyScript = InvokerHelper.createScript(groovyClass, binding);
        }
        // must make sure the correct out is present in binding
        binding.setProperty("out", new PrintWriter(out));
        return groovyScript.invokeMethod(methodName,null);
    }
    
}
