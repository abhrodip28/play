package play.template2.compile;

import play.template2.GTTemplateInstanceFactory;
import play.template2.GTTemplateRepo;

import javax.management.RuntimeErrorException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GTCompiler {

    public static File srcDestFolder = null;
    private final ClassLoader parentClassloader;
    private final GTTemplateRepo templateRepo;
    private final GTPreCompilerFactory preCompilerFactory;

    public GTCompiler(ClassLoader parentClassloader, GTTemplateRepo templateRepo, GTPreCompilerFactory preCompilerFactory) {
        this.parentClassloader = parentClassloader;
        this.templateRepo = templateRepo;
        this.preCompilerFactory = preCompilerFactory;
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

    /**
     * Write String content to a file (always use utf-8)
     * @param content The content to write
     * @param file The file to write
     */
    protected static void writeContent(CharSequence content, File file, String encoding) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, encoding));
            printWriter.println(content);
            printWriter.flush();
            os.flush();
        } catch(IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if(os != null) os.close();
            } catch(Exception e) {
                //
            }
        }
    }
    public CompiledTemplate compile( String templatePath, File templateFile ) {
        // precompile it
        GTPreCompiler.Output precompiled = preCompilerFactory.createCompiler(templateRepo).compile(templatePath, templateFile);

        // compile the java code
        //System.out.println("java: \n"+precompiled.javaCode);
        //System.out.println("groovy: \n"+precompiled.groovyCode);

        if ( srcDestFolder != null) {
            // store the generated src to disk
            File folder = new File( srcDestFolder, GTPreCompiler.generatedPackageName.replace('.','/'));
            if (!folder.exists()) {
                folder.mkdirs();
            }
            File file = new File( folder, precompiled.javaClassName+".java");
            writeContent(precompiled.javaCode, file, "utf-8");
            file = new File( folder, precompiled.groovyClassName+".groovy");
            writeContent(precompiled.groovyCode, file, "utf-8");
        }

        // compile groovy
        GTJavaCompileToClass.CompiledClass[] groovyClasses = new GTGroovyCompileToClass(parentClassloader).compileGroovySource( precompiled.groovyCode);

        // Create Classloader witch includes our groovy class
        GTTemplateInstanceFactory.CL cl = new GTTemplateInstanceFactory.CL(parentClassloader, groovyClasses);

        GTJavaCompileToClass.CompiledClass[] compiledJavaClasses = new GTJavaCompileToClass(cl).compile(precompiled.javaClassName, precompiled.javaCode);

        List<GTJavaCompileToClass.CompiledClass> allCompiledClasses = new ArrayList<GTJavaCompileToClass.CompiledClass>();
        allCompiledClasses.addAll( Arrays.asList(compiledJavaClasses) );
        allCompiledClasses.addAll( Arrays.asList(groovyClasses));

        return new CompiledTemplate(precompiled.javaClassName, allCompiledClasses.toArray( new GTJavaCompileToClass.CompiledClass[]{}));
    }

}
