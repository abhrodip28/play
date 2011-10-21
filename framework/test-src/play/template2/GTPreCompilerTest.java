package play.template2;

import org.junit.Test;
import play.template2.compile.GTPreCompiler;
import play.template2.exceptions.GTCompilationException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class GTPreCompilerTest {

    public static class AGTPreCompiler extends GTPreCompiler {

        public AGTPreCompiler(GTTemplateRepo templateRepo) {
            super(templateRepo);
        }

        @Override
        protected GTFragmentCode generateRegularActionPrinter(boolean absolute, String expression, SourceContext sc) {
            throw new RuntimeException("Not impl");
        }

        @Override
        public Class<? extends GTGroovyBase> getGroovyBaseClass() {
            return GTGroovyBase.class;
        }
    }

    @Test
    public void runit() throws Exception {

        Map<String, Object> args = new HashMap<String, Object>();

        List<Integer> myList = new ArrayList<Integer>();
        for (int i=0;i<1;i++) {
            myList.add(i);
        }

        args.put("myList", myList);
        //args.put("item", new Integer(10));

        long start = System.currentTimeMillis();
//        t.renderTemplate();
        long now = System.currentTimeMillis();
        long diff = now-start;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
  //      t.writeOutput(out, "utf-8");
        System.out.println(new String(out.toByteArray(), "utf-8"));

        System.out.println("\n***> mills: " + diff);
    }

    @Test
    public void testCompile_onlyPlainText() throws Exception {
        File file = new File("test-src/play/template2/template1.html");
        assertThat(file.exists()).isTrue();
        GTPreCompiler.Output out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);

        file = new File("test-src/play/template2/template1_multiple_lines.html");
        assertThat(file.exists()).isTrue();
        out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);
    }

    private void printOutput( File file, GTPreCompiler.Output out) {
        System.out.println("Template: " + file + " [------->");
        System.out.println(out);
        System.out.println("<------------------]");
    }

    @Test
    public void testCompile_simpleTags() throws Exception {
        File file = new File("test-src/play/template2/template2.html");
        assertThat(file.exists()).isTrue();
        GTPreCompiler.Output out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);
    }

    @Test(expected = GTCompilationException.class)
    public void testCompile_simpleTags_withError() throws Exception {
        File file = new File("test-src/play/template2/template2_withError.html");
        assertThat(file.exists()).isTrue();
        GTPreCompiler.Output out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);
    }

    @Test(expected = GTCompilationException.class)
    public void testCompile_simpleTags_withError2() throws Exception {
        File file = new File("test-src/play/template2/template2_withError2.html");
        assertThat(file.exists()).isTrue();
        GTPreCompiler.Output out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);
    }

    @Test
    public void testCompile_multipleTags() throws Exception {
        File file = new File("test-src/play/template2/template3.html");
        assertThat(file.exists()).isTrue();
        GTPreCompiler.Output out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);
    }

    @Test
    public void testCompile_list() throws Exception {
        File file = new File("test-src/play/template2/template_using_list.html");
        assertThat(file.exists()).isTrue();
        GTPreCompiler.Output out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);
    }

    @Test
    public void testCompile_ifs() throws Exception {
        File file = new File("test-src/play/template2/template_ifs.html");
        assertThat(file.exists()).isTrue();
        GTPreCompiler.Output out = new AGTPreCompiler(null).compile(file.getPath(),file);
        printOutput( file, out);
    }

}
