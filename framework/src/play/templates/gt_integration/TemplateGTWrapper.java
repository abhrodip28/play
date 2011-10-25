package play.templates.gt_integration;

import play.libs.IO;
import play.template2.GTFileResolver;
import play.templates.Template;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.Map;


public class TemplateGTWrapper extends Template {

    public TemplateGTWrapper(String relativePath) {
        File file = GTFileResolver.impl.getRealFile( relativePath);
        this.source = IO.readContentAsString( file );
        this.name = relativePath;
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
