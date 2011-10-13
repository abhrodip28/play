package play.template2;

import org.junit.Test;
import play.template2.compile.GTPreCompiler;
import play.templates.GTIntegration1X;
import play.templates.GTLegacyFastTagResolver1X;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GTTemplateRepoTest {
    @Test
    public void testGetTemplateInstance() throws Exception {

        GTTemplateRepo repo = new GTTemplateRepo(getClass().getClassLoader(), true, new GTIntegration1X());

        File templateFile = new File("test-src/play/template2/template_ifs.html");
        String templatePath = templateFile.getCanonicalPath();

        Map<String, Object> args = new HashMap<String, Object>();
        GTJavaBase t = repo.getTemplateInstance( templatePath);
        t.renderTemplate(args);
        t.writeOutput( System.out, "utf-8");

        t = repo.getTemplateInstance( templatePath);
        t.renderTemplate(args);
        t.writeOutput( System.out, "utf-8");

        Thread.sleep(10);
        templateFile.setLastModified( new Date().getTime()); // touch the file

        t = repo.getTemplateInstance( templatePath);
        t.renderTemplate(args);
        t.writeOutput( System.out, "utf-8");
        
    }

    @Test
    public void test_using_extends() throws Exception {

        Map<String, Object> args = new HashMap<String, Object>();

        GTTemplateRepo repo = new GTTemplateRepo(getClass().getClassLoader(), true, new GTIntegration1X());

        File templateFile = new File("test-src/play/template2/template_using_extends.html");
        String templatePath = templateFile.getCanonicalPath();

        GTJavaBase t = repo.getTemplateInstance( templatePath);
        t.renderTemplate(args);
        t.writeOutput( System.out, "utf-8");
        
    }

    @Test
    public void test_using_fasttags() throws Exception {

        Map<String, Object> args = new HashMap<String, Object>();

        GTTemplateRepo repo = new GTTemplateRepo(getClass().getClassLoader(), true, new GTIntegration1X());

        File templateFile = new File("test-src/play/template2/template_using_fasttags.html");
        String templatePath = templateFile.getCanonicalPath();

        GTJavaBase t = repo.getTemplateInstance( templatePath);
        t.renderTemplate(args);
        t.writeOutput( System.out, "utf-8");

    }

    @Test
    public void test_using_legacy_fasttags() throws Exception {

        Map<String, Object> args = new HashMap<String, Object>();

        GTPreCompiler.legacyFastTagResolver = new GTLegacyFastTagResolver1X();
        GTTemplateRepo repo = new GTTemplateRepo(getClass().getClassLoader(), true, new GTIntegration1X());

        File templateFile = new File("test-src/play/template2/template_using_legacy_fasttags.html");
        String templatePath = templateFile.getCanonicalPath();

        GTJavaBase t = repo.getTemplateInstance( templatePath);
        t.renderTemplate(args);
        t.writeOutput( System.out, "utf-8");

    }

}
