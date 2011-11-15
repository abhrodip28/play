package play.template2.compile;

import org.junit.Test;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.template2.GTTemplateLocation;
import play.template2.GTTemplateLocationWithEmbeddedSource;
import play.template2.GTTemplateRepo;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class GTPreCompilerTest {

    private static class GTPreCompilerFactoryImpl implements GTPreCompilerFactory {

        public GTTemplateRepo templateRepo;

        public GTPreCompiler createCompiler(GTTemplateRepo templateRepo) {
            return new GTPreCompiler(templateRepo) {
                @Override
                public Class<? extends GTJavaBase> getJavaBaseClass() {
                    return GTJavaBaseTesterImpl.class;
                }
            };
        }

    }


    private GTTemplateRepo createTemplateRepo() {

        GTGroovyPimpTransformer.gtJavaExtensionMethodResolver = new GTJavaExtensionMethodResolver() {
            public Class findClassWithMethod(String methodName) {
                return null;
            }
        };

        GTJavaCompileToClass.typeResolver = new GTTypeResolver() {
            public byte[] getTypeBytes(String name) {

                try {
                    InputStream in = getClass().getClassLoader().getResourceAsStream( name.replaceAll("\\.", "/") + ".class");
                    if ( in==null) {
                        return null;
                    }

                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    while ( (bytesRead = in.read(buffer))>0 ) {
                        out.write(buffer, 0, bytesRead);
                    }

                    return out.toByteArray();

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };

        final GTPreCompilerFactoryImpl preCompilerFactory = new GTPreCompilerFactoryImpl();
        final GTTemplateRepo templateRepo = new GTTemplateRepo(getClass().getClassLoader(), false, preCompilerFactory, false, null);
        preCompilerFactory.templateRepo = templateRepo;
        return templateRepo;
    }





    @Test
    public void testNewLineInsideTagsExprEtc() throws Exception {

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");

        assertThat(renderSrc("hello world", args)).isEqualTo("hello world");
        assertThat(renderSrc("${myData}", args)).isEqualTo("xxx");
        assertThat(renderSrc("${myData\n}", args)).isEqualTo("xxx");
        assertThat(renderSrc("${\r\nmyData\n}", args)).isEqualTo("xxx");
        assertThat(renderSrc("a${\nmyData\n}\nb${myData}", args)).isEqualTo("axxx\nbxxx");

        assertThat(renderSrc("#{set title:'a'/}", args)).isEqualTo("");
        assertThat(renderSrc("#{set 'title'}Q#{/set}", args)).isEqualTo("");

        assertThat(renderSrc("${ 1 == 1 ?\n 'true' : 'false'}", args)).isEqualTo("true");



    }

    // test some errors
    @Test
    public void testNewLineInsideTagsExprEtcError_1() throws Exception {

        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");

        GTCompilationExceptionWithSourceInfo ex = null;
        try {
            renderSrc("${myData", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found open $-declaration");

        ex = null;
        try {
            renderSrc("#{myTag", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found open #-declaration");

        ex = null;
        try {
            renderSrc("@@{'sss'", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found open @@-declaration");

        ex = null;
        try {
            renderSrc("#{aTag}", args);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            ex = e;
        }
        assertThat(ex.specialMessage).isEqualTo("Found unclosed tag #{aTag}");

        
    }


    private String renderSrc(String src, Map<String, Object> args) throws UnsupportedEncodingException {
        final GTTemplateRepo tr = createTemplateRepo();

        GTTemplateLocationWithEmbeddedSource tl = new GTTemplateLocationWithEmbeddedSource(src);

        GTJavaBase t = tr.getTemplateInstance(tl);

        t.renderTemplate( args );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        t.writeOutput(out, "utf-8");

        return new String(out.toByteArray(), "utf-8");
    }

}
