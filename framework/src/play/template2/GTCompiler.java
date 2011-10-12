package play.template2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GTCompiler {

    private final ClassLoader parentClassloader;

    public GTCompiler(ClassLoader parentClassloader) {
        this.parentClassloader = parentClassloader;
    }

    public static class CL extends ClassLoader {

        private final String resourceName;
        private final byte[] bytes;

        public CL(ClassLoader parent, String classname, byte[] bytes) {
            super(parent);
            resourceName = classname.replace(".", "/") + ".class";;
            this.bytes = bytes;
            Class c = defineClass(classname, bytes, 0, bytes.length);
            int a = 0;
        }

        @Override
        public InputStream getResourceAsStream(String s) {
            if (resourceName.equals(s)) {
                return new ByteArrayInputStream(bytes);
            } else {
                return super.getResourceAsStream(s);
            }
        }
    }

    public static class CompiledTemplate {
        public final String templateClassName;
        public final GTJavaCompileToClass.CompiledClass[] compiledJavaClasses;

        public CompiledTemplate(String templateClassName, GTJavaCompileToClass.CompiledClass[] compiledJavaClasses) {
            this.templateClassName = templateClassName;
            this.compiledJavaClasses = compiledJavaClasses;
        }
    }

    public CompiledTemplate compile( File templateFile ) {
        // precompile it
        GTPreCompiler.Output precompiled = new GTPreCompiler().compile( templateFile);

        // compile groovy
        byte[] groovyClassBytes = new GTGroovyCompileToClass(parentClassloader).compileGroovySource( precompiled.groovyCode);

        // Create Classloader witch includes our groovy class
        CL cl = new CL(parentClassloader, precompiled.groovyClassName, groovyClassBytes);

        // compile the java code
        System.out.println(precompiled.javaCode);
        GTJavaCompileToClass.CompiledClass[] compiledJavaClasses = new GTJavaCompileToClass(cl).compile(precompiled.javaClassName, precompiled.javaCode);

        List<GTJavaCompileToClass.CompiledClass> allCompiledClasses = new ArrayList<GTJavaCompileToClass.CompiledClass>();
        allCompiledClasses.addAll( Arrays.asList(compiledJavaClasses) );
        allCompiledClasses.add( new GTJavaCompileToClass.CompiledClass(precompiled.groovyClassName, groovyClassBytes));

        return new CompiledTemplate(precompiled.javaClassName, allCompiledClasses.toArray( new GTJavaCompileToClass.CompiledClass[]{}));
    }

}
