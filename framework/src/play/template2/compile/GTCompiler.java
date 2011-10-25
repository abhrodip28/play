package play.template2.compile;

import play.template2.GTTemplateInstanceFactory;
import play.template2.GTTemplateLocation;
import play.template2.GTTemplateRepo;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static class LineMapper {

        private final Integer[] lineLookup;

        public LineMapper(String[] preCompiledLines) {
            this.lineLookup = generateLineLookup(preCompiledLines);
        }

        public int translateLineNo(int originalLineNo) {
            int line = originalLineNo - 1; // make it 0-based
            if (line >= lineLookup.length) {
                line = lineLookup.length-1;
            }

            // start at line. if value, return it, if not go one up and check again
            while ( line >= 0) {
                Integer i = lineLookup[line];
                if ( i != null) {
                    return i;
                }
                line--;
            }
            return 1;
        }

        private static final Pattern lineNoP = Pattern.compile(".*//lineNo:(\\d+)$");

        // Returns array with one int pr line in the precompile src.
        // each int (if not null) points to the corresponding line in the original template src
        // to convert a line, look up at src line-1 and read out the correct line.
        // if you find null, just walk up until you find a line
        private Integer[] generateLineLookup(String[] precompiledSrcLines) {
            Integer[] mapping = new Integer[precompiledSrcLines.length];
            int i=0;
            for ( String line : precompiledSrcLines ) {
                Matcher m = lineNoP.matcher(line);
                if ( m.find()) {
                    mapping[i] = Integer.parseInt( m.group(1));
                }
                i++;
            }
            return mapping;
        }

    }

    public static class CompiledTemplate {
        public final String templateClassName;
        public final GTJavaCompileToClass.CompiledClass[] compiledJavaClasses;

        public final LineMapper javaLineMapper;
        public final LineMapper groovyLineMapper;

        public CompiledTemplate(String templateClassName, GTJavaCompileToClass.CompiledClass[] compiledJavaClasses, LineMapper javaLineMapper, LineMapper groovyLineMapper) {
            this.templateClassName = templateClassName;
            this.compiledJavaClasses = compiledJavaClasses;
            this.javaLineMapper = javaLineMapper;
            this.groovyLineMapper = groovyLineMapper;
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

    public CompiledTemplate compile( GTTemplateLocation templateLocation) {
        // precompile it
        GTPreCompiler.Output precompiled = preCompilerFactory.createCompiler(templateRepo).compile(templateLocation);

        String[] javaLines = precompiled.javaCode.split("\n");
        LineMapper javaLineMapper = new LineMapper( javaLines);
        String[] groovyLines = precompiled.groovyCode.split("\n");
        LineMapper groovyLineMapper = new LineMapper( groovyLines);

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
        GTJavaCompileToClass.CompiledClass[] groovyClasses = groovyClasses = new GTGroovyCompileToClass(parentClassloader).compileGroovySource( templateLocation, groovyLineMapper, precompiled.groovyCode);

        // Create Classloader witch includes our groovy class
        GTTemplateInstanceFactory.CL cl = new GTTemplateInstanceFactory.CL(parentClassloader, groovyClasses);

        GTJavaCompileToClass.CompiledClass[] compiledJavaClasses = new GTJavaCompileToClass(cl).compile(precompiled.javaClassName, precompiled.javaCode);

        List<GTJavaCompileToClass.CompiledClass> allCompiledClasses = new ArrayList<GTJavaCompileToClass.CompiledClass>();
        allCompiledClasses.addAll( Arrays.asList(compiledJavaClasses) );
        allCompiledClasses.addAll( Arrays.asList(groovyClasses));

        return new CompiledTemplate(precompiled.javaClassName, allCompiledClasses.toArray( new GTJavaCompileToClass.CompiledClass[]{}), javaLineMapper, groovyLineMapper);
    }

}
