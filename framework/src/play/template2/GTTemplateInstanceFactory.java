package play.template2;


import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class GTTemplateInstanceFactory {

    private final GTCompiler.CompiledTemplate compiledTemplate;
    private final CL cl;
    private final Class< ? extends GTJavaBase> templateClass;

    public static class CL extends ClassLoader {

        private final String groovyResourceName;
        private final byte[] groovyBytes;

        private final String javaResourceName;
        private final byte[] javaBytes;

        public CL(ClassLoader parent, String groovyClassname, byte[] groovyBytes, String javaClassname, byte[] javaBytes) {
            super(parent);
            groovyResourceName = groovyClassname.replace(".", "/") + ".class";;
            this.groovyBytes = groovyBytes;
            defineClass(groovyClassname, groovyBytes, 0, groovyBytes.length);

            javaResourceName = javaClassname.replace(".", "/") + ".class";;
            this.javaBytes = javaBytes;
            defineClass(javaClassname, javaBytes, 0, javaBytes.length);
        }

        @Override
        public InputStream getResourceAsStream(String s) {
            if (groovyResourceName.equals(s)) {
                return new ByteArrayInputStream(groovyBytes);
            } else if (javaResourceName.equals(s)) {
                return new ByteArrayInputStream(javaBytes);
            } else {
                return super.getResourceAsStream(s);
            }
        }
    }

    public GTTemplateInstanceFactory(ClassLoader parentClassLoader, GTCompiler.CompiledTemplate compiledTemplate) {
        this.compiledTemplate = compiledTemplate;
        this.cl = new CL(parentClassLoader, compiledTemplate.groovyClassName, compiledTemplate.groovyClassBytes, compiledTemplate.javaClassName, compiledTemplate.javaClassBytes);
        try {
            this.templateClass = (Class<? extends GTJavaBase>)cl.loadClass(compiledTemplate.javaClassName);
        } catch (Exception e) {
            throw new RuntimeException("Error creating template class instance", e);
        }
    }

    public GTJavaBase create() {
        GTJavaBase templateInstance;
        try {
            templateInstance = (GTJavaBase)templateClass.newInstance();
            return templateInstance;
        } catch (Exception e) {
            throw new RuntimeException("Error creating template instance", e);
        }
    }
}
