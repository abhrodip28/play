package play.template2;

import groovy.lang.GroovyClassLoader;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.tools.GroovyClass;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class GTGroovyCompileToClass {

    private final ClassLoader parentClassLoader;

    public GTGroovyCompileToClass(ClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }

    public byte[] compileGroovySource( String groovySource) {

        final List<GroovyClass> groovyClassesForThisTemplate = new ArrayList<GroovyClass>();

        GroovyClassLoader classLoader = new GroovyClassLoader(parentClassLoader);

        CompilerConfiguration compilerConfiguration = new CompilerConfiguration();
        compilerConfiguration.setSourceEncoding("utf-8"); // ouf
        CompilationUnit compilationUnit = new CompilationUnit(compilerConfiguration);
        compilationUnit.addSource(new SourceUnit("", groovySource, compilerConfiguration, classLoader, compilationUnit.getErrorCollector()));
        Field phasesF;
        LinkedList[] phases;
        try {
            phasesF = compilationUnit.getClass().getDeclaredField("phaseOperations");
            phasesF.setAccessible(true);
            phases = (LinkedList[]) phasesF.get(compilationUnit);
        } catch (Exception e) {
            throw new RuntimeException("Not supposed to happen", e);
        }

        LinkedList<CompilationUnit.GroovyClassOperation> output = new LinkedList<CompilationUnit.GroovyClassOperation>();
        phases[Phases.OUTPUT] = output;
        output.add(new CompilationUnit.GroovyClassOperation() {
            public void call(GroovyClass gclass) {
                groovyClassesForThisTemplate.add(gclass);
            }
        });
        compilationUnit.compile();

        if ( groovyClassesForThisTemplate.size() != 1 ) {
            throw new RuntimeException("The compilation should only result in one groovy class!");
        }

        GroovyClass groovyClass = groovyClassesForThisTemplate.get(0);

//        Class javaClass;
//        try {
//            javaClass = classLoader.loadClass( groovyClass.getName());
//        } catch (Exception e) {
//            throw new RuntimeException("Error loading java class for groovy class", e);
//        }

        return groovyClass.getBytes();
    }
}
