package play.template2;

import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/12/11
 * Time: 12:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTJavaCompileToClassTest {

    public static class CL extends ClassLoader {

        public CL(ClassLoader parent, String classname, byte[] bytes) {
            super(parent);
            defineClass(classname, bytes, 0, bytes.length);
        }
    }
    @Test
    public void testCompile() throws Exception {

        String src = "package a;\n" +
                "\n" +
                "public class MyClass implements Runnable {\n" +
                "\n" +
                "\tpublic void run() {\n" +
                "\t\tSystem.out.println(\"It runs!\");\n" +
                "\t}\n" +
                "\n" +
                "}";

        String name = "a.MyClass";

        byte[] b = new GTJavaCompileToClass(getClass().getClassLoader()).compile(name, src);

        CL cl = new CL(getClass().getClassLoader(), "a.MyClass", b);

        Class c = cl.loadClass("a.MyClass");

        Runnable r = (Runnable)c.newInstance();

        r.run();

        int a = 0;
    }
}
