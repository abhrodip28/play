package play.template2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

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
        public final String javaClassName;
        public final byte[] javaClassBytes;
        public final String groovyClassName;
        public final byte[] groovyClassBytes;

        public CompiledTemplate(String javaClassName, byte[] javaClassBytes, String groovyClassName, byte[] groovyClassBytes) {
            this.javaClassName = javaClassName;
            this.javaClassBytes = javaClassBytes;
            this.groovyClassName = groovyClassName;
            this.groovyClassBytes = groovyClassBytes;
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
        byte[] javaClassBytes = new GTJavaCompileToClass(cl).compile( precompiled.javaClassName, precompiled.javaCode);

        return new CompiledTemplate(precompiled.javaClassName, javaClassBytes, precompiled.groovyClassName, groovyClassBytes);

    }

}
