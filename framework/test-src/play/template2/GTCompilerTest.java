package play.template2;

import org.junit.Test;

import java.io.File;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/12/11
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTCompilerTest {
    @Test
    public void testCompile() throws Exception {
        File file = new File("test-src/play/template2/template_ifs.html");
        assertThat(file.exists()).isTrue();
        new GTCompiler(getClass().getClassLoader()).compile( file );
    }
}
