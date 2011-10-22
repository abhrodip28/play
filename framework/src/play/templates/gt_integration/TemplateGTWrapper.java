package play.templates.gt_integration;

import play.libs.IO;
import play.templates.Template;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.Map;


public class TemplateGTWrapper extends Template {



    public TemplateGTWrapper(File srcFile) {
        this.source = IO.readContentAsString( srcFile);
        this.name = VirtualFile.open(srcFile).relativePath();
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
