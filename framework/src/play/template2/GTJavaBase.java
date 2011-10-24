package play.template2;

import groovy.lang.Binding;
import groovy.lang.Script;
import org.apache.commons.collections.iterators.ArrayIterator;
import org.codehaus.groovy.runtime.InvokerHelper;
import play.template2.compile.GTCompiler;
import play.template2.exceptions.GTRuntimeException;
import play.template2.exceptions.GTTemplateNotFoundWithSourceInfo;
import play.template2.exceptions.GTTemplateRuntimeException;

import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public abstract class GTJavaBase extends GTRenderingResult {

    public final static String executeNextElseKeyName = "_executeNextElse";

    public StringWriter out;

    protected Script groovyScript = null;
    public Binding binding;
    private final Class<? extends GTGroovyBase> groovyClass;

    protected Map<String, Object> orgArgs = null;

    // if this tag uses #{extends}, then the templatePath we extends is stored here.
    public String extendsTemplatePath = null; // default is not to extend anything...
    public GTJavaBase extendedTemplate = null;
    public GTJavaBase extendingTemplate = null; // if someone is extending us, this is the ref to their rendered template - used when dumping their output

    // When invoking a template as a tag, the content of the tag / body is stored here..
    public GTContentRenderer contentRenderer;

    public GTTemplateRepo templateRepo;

    public final String templatePath;
    public final File templateFile;


    // this gets a value (injected) after the template is new'ed - contains line-mapping info
    public GTCompiler.CompiledTemplate compiledTemplate;

    public static ThreadLocal<Map<Object, Object>> layoutData = new ThreadLocal<Map<Object, Object>>();

    public GTJavaBase(Class<? extends GTGroovyBase> groovyClass, String templatePath, File templateFile ) {
        this.groovyClass = groovyClass;
        this.templatePath = templatePath;
        this.templateFile = templateFile;

        initNewOut();

    }


    @Override
    public void writeOutput(OutputStream ps, String encoding) {
        // if we have extended another template, we must pass this on to this template-instance,
        // because "it" has all the output
        if (extendedTemplate != null) {
            extendedTemplate.writeOutput(ps, encoding);
            return ;
        }
        super.writeOutput( ps, encoding);
    }

    public void insertOutput(GTRenderingResult otherTemplate) {
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

    public void renderTemplate(Map<String, Object> args) throws GTTemplateNotFoundWithSourceInfo, GTRuntimeException{
        // this is the main rendering start of the actual template

        // init layout data which should be visible for all templates involved
        layoutData.set( new HashMap<Object, Object>() );

        internalRenderTemplate(args, true);
    }
    

    protected void internalRenderTemplate(Map<String, Object> args, boolean startingNewRendering) throws GTTemplateNotFoundWithSourceInfo, GTRuntimeException{

        if ( startingNewRendering) {
            // start with fresh tag-stack
            GTTagContext.init();
        }

        try {

            // must store a copy of args, so we can pass the same (unchnaged) args to an extending template.
            this.orgArgs = new HashMap<String, Object>(args);
            this.binding = new Binding(args);
            // must init our groovy script

            groovyScript = InvokerHelper.createScript(groovyClass, binding);
            // create a property in groovy so that groovy can find us (this)
            groovyScript.setProperty(GTGroovyBase.__templateRef_propertyName, this);

            groovyScript.setProperty("java_class", this);
            groovyScript.run();
            //_renderTemplate();

            // check if "we" have extended an other template..
            if (extendsTemplatePath != null) {
                // yes, we've extended another template
                // Get the template we are extending
                extendedTemplate = templateRepo.getTemplateInstance( extendsTemplatePath);

                // tell it that "we" extended it..
                extendedTemplate.extendingTemplate = this;

                // ok, render it with original args..
                extendedTemplate.internalRenderTemplate( orgArgs, true );
            }

        } catch( GTTemplateNotFoundWithSourceInfo e) {
            throw e;
        } catch ( GTRuntimeException e) {
            // just throw it
            throw e;
        } catch ( Throwable e) {
            // wrap it in a GTRuntimeException
            throw templateRepo.fixException(e);

        }
    }

    protected abstract void _renderTemplate();

    protected void enterTag( String tagName) {
        GTTagContext.enterTag(tagName);
    }

    protected void leaveTag( String tagName) {
        GTTagContext.exitTag();
    }
    
    /**
     * return the class/interface that, when an object is instanceof it, we should use
     * convertRawDataToString when converting it to String.
     * Framework should override.
     */
    public abstract Class getRawDataClass();

    /**
     *  See getRawDataClass for info
     */
    public abstract String convertRawDataToString(Object rawData);

    public abstract String escapeHTML( String s);



    // We know that o is never null
    public String objectToString( Object o) {
        Class rawDataClass = getRawDataClass();
        if (rawDataClass != null && rawDataClass.isAssignableFrom(o.getClass())) {
            return convertRawDataToString(o);
        } else if (!templatePath.endsWith(".html") || GTTagContext.hasParentTag("verbatim")) {
            return o.toString();
        } else {
            return escapeHTML( o.toString());
        }
    }

    public boolean evaluateCondition(Object test) {
        if (test != null) {
            if (test instanceof Boolean) {
                return ((Boolean) test).booleanValue();
            } else if (test instanceof String) {
                return ((String) test).length() > 0;
            } else if (test instanceof Number) {
                return ((Number) test).intValue() != 0;
            } else if (test instanceof Collection) {
                return !((Collection) test).isEmpty();
            } else {
                return true;
            }
        }
        return false;
    }

    protected void invokeTagFile(String tagName, String tagFilePath, GTContentRenderer contentRenderer, Map<String, Object> tagArgs) {

        GTJavaBase tagTemplate = templateRepo.getTemplateInstance(tagFilePath);
        // must set contentRenderes so that when the tag/template calls doBody, we can inject the output of the content of this tag
        tagTemplate.contentRenderer = contentRenderer;
        // render the tag
        // input should be all org args
        Map<String, Object> completeTagArgs = new HashMap<String, Object>( orgArgs );

        // and all scoped variables under _caller
        completeTagArgs.put("_caller", this.binding.getVariables());

        // TODO: Must handle tag args like  _:_

        // and of course the tag args:
        // must prefix all tag args with '_'
        for ( String key : tagArgs.keySet()) {
            completeTagArgs.put("_"+key, tagArgs.get(key));
        }

        // Must also add all tag-args (the map) with original names as a new value named '_attrs'
        completeTagArgs.put("_attrs", tagArgs);

        tagTemplate.internalRenderTemplate(completeTagArgs, false);
        //grab the output
        insertOutput( tagTemplate );
    }


    // must be overridden by play framework
    public abstract boolean validationHasErrors();

    // must be overridden by play framework
    public abstract boolean validationHasError(String key);

    public abstract String messagesGet(Object key, Object... args);

    public void clearElseFlag() {
        GTTagContext.parent().data.remove(executeNextElseKeyName);
    }

    public void setElseFlag() {
        GTTagContext.parent().data.put(executeNextElseKeyName, true);
    }

    public boolean elseFlagIsSet() {
        Boolean v = (Boolean)GTTagContext.parent().data.get(executeNextElseKeyName);
        if ( v != null) {
            return v;
        } else {
            return false;
        }
    }
    
    protected String handleMessageTag(Object _args) {

        List argsList = (List)_args;

        if ( argsList.size()==0) {
            throw new GTTemplateRuntimeException("It looks like you don't have anything in your Message tag");
        }
        Object key = argsList.get(0);
        if (key==null) {
            throw new GTTemplateRuntimeException("You are trying to resolve a message with an expression " +
                    "that is resolved to null - " +
                    "have you forgotten quotes around the message-key?");
        }
        if (argsList.size() == 1) {
            String m = messagesGet(key);
            return m;
        } else {
            // extract args from val
            Object[] args = new Object[argsList.size()-1];
            for( int i=1;i<argsList.size();i++) {
                args[i-1] = argsList.get(i);
            }
            String m = messagesGet(key, args);
            return m;
        }
    }

    protected Iterator convertToIterator(final Object o) {

        if ( o instanceof Iterator) {
            return (Iterator)o;
        }

        if ( o instanceof Iterable ) {
            return ((Iterable)o).iterator();
        }

        if ( o instanceof Map ) {
            return (((Map)o).entrySet()).iterator();
        }

        if ( o.getClass().isArray()) {
            return new Iterable() {
                public Iterator iterator() {
                    return new ArrayIterator(o);
                }
            }.iterator();
        }

        throw new GTTemplateRuntimeException("Cannot convert object-reference to Iterator");
    }
}
