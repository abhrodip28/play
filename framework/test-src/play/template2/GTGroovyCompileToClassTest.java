package play.template2;

import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/12/11
 * Time: 12:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTGroovyCompileToClassTest {
    @Test
    public void testCompileGroovySource() throws Exception {

        String name = "a";
        String src = "package play.template2.tmp\n" +
                "\n" +
                "class Template2__Users_mortenkjetland_tmp_mbkplay_play_framework_test_src_play_template2_template_ifs_htmlG extends play.template2.GTGroovyBase {\n" +
                " Boolean ifChecker(Object e) {\n" +
                "  if(e) {return true;} else {return false;}\n" +
                " }\n" +
                "Map<String, Object> args_if_0() {\n" +
                " return [arg:1];\n" +
                "}\n" +
                "}";

        byte[] bytes = new GTGroovyCompileToClass(getClass().getClassLoader()).compileGroovySource(src);

        
        int a = 0;

        
    }
}
