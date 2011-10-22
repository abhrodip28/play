package play.templates.gt_integration;

import play.libs.IO;
import play.templates.Template;

import java.io.File;
import java.util.Map;


public class TemplateGTWrapper extends Template {



    public TemplateGTWrapper(File templatePath) {
        this.source = IO.readContentAsString( templatePath);
        this.name = templatePath.toString();
    }


    @Override
    public void compile() {
        throw new RuntimeException("Not implemented in this wrapper");
    }

    @Override
    protected String internalRender(Map<String, Object> args) {
        throw new RuntimeException("Not implemented in this wrapper");
    }
}
