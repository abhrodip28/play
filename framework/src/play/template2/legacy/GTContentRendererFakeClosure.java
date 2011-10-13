package play.template2.legacy;


import groovy.lang.Closure;
import groovy.lang.MetaClass;
import org.apache.commons.io.output.ByteArrayOutputStream;
import play.template2.GTContentRenderer;
import play.template2.GTJavaBase;
import play.template2.GTRenderingResult;

public class GTContentRendererFakeClosure extends Closure {

    public final GTContentRenderer contentRenderer;
    private final GTJavaBase template;

    public GTContentRendererFakeClosure(GTJavaBase template, GTContentRenderer contentRenderer) {
        super(null, null);
        this.template = template;
        this.contentRenderer = contentRenderer;
    }

    public String renderToString() {
        GTRenderingResult res = contentRenderer.render();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        res.writeOutput(out, "utf-8");
        return out.toString();
    }

    @Override
    public Object call() {
        // run the content
        GTRenderingResult res = contentRenderer.render();
        // inject all the generated output into the output for the template
        template.insertOutput( res );
        return null;
    }



    @Override
    public void setResolveStrategy(int resolveStrategy) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getResolveStrategy() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getThisObject() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getProperty(String property) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setProperty(String property, Object newValue) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isCase(Object candidate) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object call(Object[] args) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object call(Object arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getOwner() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getDelegate() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setDelegate(Object delegate) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Class[] getParameterTypes() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getMaximumNumberOfParameters() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure asWritable() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void run() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure curry(Object[] arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure rcurry(Object[] arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Closure ncurry(int n, Object[] arguments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object clone() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int getDirective() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setDirective(int directive) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object invokeMethod(String name, Object args) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public MetaClass getMetaClass() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setMetaClass(MetaClass metaClass) {
        throw new RuntimeException("Not implemented");
    }
}
