package play.template2;

import org.junit.Test;
import play.template2.tmp.Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

public class GTCompilerTest {

    @Test
    public void runit() {
        Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html t = new Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_using_list_html();
        t.main();
        System.out.println(t.out);
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

}
