package play.template2;

import org.junit.Test;
import play.template2.tmp.Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class GTCompilerTest {

    @Test
    public void runit() throws Exception {

        Map<String, Object> args = new HashMap<String, Object>();

        List<Integer> myList = new ArrayList<Integer>();
        for (int i=0;i<1;i++) {
            myList.add(i);
        }

        args.put("myList", myList);
        //args.put("item", new Integer(10));

        Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html t = new Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html(args);

        t.renderTemplate();
        t = new Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html(args);
        t.renderTemplate();

        t = new Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html(args);


        long start = System.currentTimeMillis();
        t.renderTemplate();
        long now = System.currentTimeMillis();
        long diff = now-start;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        t.writeOutput(out, "utf-8");
        System.out.println(new String(out.toByteArray(), "utf-8"));

        System.out.println("\n***> mills: " + diff);
    }

    @Test
    public void testCompile_onlyPlainText() throws Exception {
        File file = new File("test-src/play/template2/template1.html");
        assertThat(file.exists()).isTrue();
        GTCompiler.Output out = new GTCompiler().compile(file);
        printOutput( file, out);

        file = new File("test-src/play/template2/template1_multiple_lines.html");
        assertThat(file.exists()).isTrue();
        out = new GTCompiler().compile(file);
        printOutput( file, out);
    }

    private void printOutput( File file, GTCompiler.Output out) {
        System.out.println("Template: " + file + " [------->");
        System.out.println(out);
        System.out.println("<------------------]");
    }

    @Test
    public void testCompile_simpleTags() throws Exception {
        File file = new File("test-src/play/template2/template2.html");
        assertThat(file.exists()).isTrue();
        GTCompiler.Output out = new GTCompiler().compile(file);
        printOutput( file, out);
    }

    @Test(expected = GTCompilerException.class)
    public void testCompile_simpleTags_withError() throws Exception {
        File file = new File("test-src/play/template2/template2_withError.html");
        assertThat(file.exists()).isTrue();
        GTCompiler.Output out = new GTCompiler().compile(file);
        printOutput( file, out);
    }

    @Test(expected = GTCompilerException.class)
    public void testCompile_simpleTags_withError2() throws Exception {
        File file = new File("test-src/play/template2/template2_withError2.html");
        assertThat(file.exists()).isTrue();
        GTCompiler.Output out = new GTCompiler().compile(file);
        printOutput( file, out);
    }

    @Test
    public void testCompile_multipleTags() throws Exception {
        File file = new File("test-src/play/template2/template3.html");
        assertThat(file.exists()).isTrue();
        GTCompiler.Output out = new GTCompiler().compile(file);
        printOutput( file, out);
    }

    @Test
    public void testCompile_list() throws Exception {
        File file = new File("test-src/play/template2/template_using_list.html");
        assertThat(file.exists()).isTrue();
        GTCompiler.Output out = new GTCompiler().compile(file);
        printOutput( file, out);
    }

    @Test
    public void testCompile_ifs() throws Exception {
        File file = new File("test-src/play/template2/template_ifs.html");
        assertThat(file.exists()).isTrue();
        GTCompiler.Output out = new GTCompiler().compile(file);
        printOutput( file, out);
    }

}
