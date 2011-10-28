package play.template2;


import play.template2.compile.GTCompiler;
import play.template2.compile.GTJavaCompileToClass;
import play.template2.exceptions.GTException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class GTTemplateInstanceFactory {

    private final GTCompiler.CompiledTemplate compiledTemplate;
    private final CL cl;
    private final Class< ? extends GTJavaBase> templateClass;

    public static class CL extends ClassLoader {

        private final Map<String, byte[]> resource2bytes = new HashMap<String, byte[]>();

        public CL(ClassLoader parent, GTJavaCompileToClass.CompiledClass[] compiledClasses) {
            super(parent);

            for (GTJavaCompileToClass.CompiledClass cp : compiledClasses) {
                defineClass(cp.classname, cp.bytes, 0, cp.bytes.length);
                String resourceName = cp.classname.replace(".", "/") + ".class";
                resource2bytes.put(resourceName, cp.bytes);
            }
        }

        @Override
        public InputStream getResourceAsStream(String s) {

            if (resource2bytes.containsKey(s)) {
                return new ByteArrayInputStream(resource2bytes.get(s));
            } else {
                return super.getResourceAsStream(s);
            }
        }
    }

    public GTTemplateInstanceFactory(ClassLoader parentClassLoader, GTCompiler.CompiledTemplate compiledTemplate) {
        this.compiledTemplate = compiledTemplate;
        this.cl = new CL(parentClassLoader, compiledTemplate.compiledJavaClasses);
        try {
            this.templateClass = (Class<? extends GTJavaBase>)cl.loadClass(compiledTemplate.templateClassName);
        } catch (Exception e) {
            throw new GTException("Error creating template class instance", e);
        }
    }

    public GTJavaBase create(GTTemplateRepo templateRepo) {
        GTJavaBase templateInstance;
        try {
            templateInstance = (GTJavaBase)templateClass.newInstance();
            templateInstance.compiledTemplate = compiledTemplate;
            // Must tell the template Instance where the current templateRepo is - needed when processing #{extends} and custom tags
            templateInstance.templateRepo = templateRepo;
            return templateInstance;
        } catch (Exception e) {
            throw new GTException("Error creating template instance", e);
        }
    }
}
