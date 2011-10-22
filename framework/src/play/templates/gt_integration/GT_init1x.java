package play.templates.gt_integration;

import play.Play;
import play.template2.GTDefaultTemplateFileResolver;
import play.template2.compile.GTCompiler;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.List;

public class GT_init1x {

    /**
     * Must be called once to init the GT-template-engine so it works in our play 1.x framework
     */
    public static void initGTTemplateEngine(List<VirtualFile> templatesPaths) {

        File[] folders = new File[templatesPaths.size()];

        int i=0;
        for (VirtualFile vf : templatesPaths) {
            folders[i] = vf.getRealFile();
            i++;
        }

        GTDefaultTemplateFileResolver.templateFolders = folders;

        // set up folder where we dump generated src
        GTCompiler.srcDestFolder = new File(Play.applicationPath, "generated-src");

    }
}
