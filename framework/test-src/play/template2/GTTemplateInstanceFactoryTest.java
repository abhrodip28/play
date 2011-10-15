package play.template2;

import org.junit.Test;
import play.template2.compile.GTCompiler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/12/11
 * Time: 2:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTTemplateInstanceFactoryTest {
    @Test
    public void testCreate() throws Exception {

        ClassLoader parentClassLoader = getClass().getClassLoader();

        File file = new File("test-src/play/template2/template_using_list.html");
        assertThat(file.exists()).isTrue();

        GTCompiler.CompiledTemplate cp = new GTCompiler(parentClassLoader, null, null).compile( file );

        GTTemplateInstanceFactory factory = new GTTemplateInstanceFactory(parentClassLoader, cp);

        GTJavaBase t = factory.create();

        Map<String, Object> args = new HashMap<String, Object>();

        args.put("myList", Arrays.asList(1,2,3,4,5));

        t.renderTemplate(args);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        t.writeOutput(out, "utf-8");
        System.out.println(new String(out.toByteArray(), "utf-8"));
    }
}
