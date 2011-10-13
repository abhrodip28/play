package play.template2;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GTTemplateRepo {

    public final ClassLoader parentClassLoader;
    public final boolean checkForChanges;

    public static TemplateFileResolver templateFileResolver = new DefaultTemplateFileResolver();

    public final GTIntegration integration;

    private Map<String, TemplateInfo> loadedTemplates = new HashMap<String, TemplateInfo>();

    public static interface TemplateFileResolver {
        public File resolveTemplatePathToFile(String templatePath);
    }

    public static class DefaultTemplateFileResolver implements TemplateFileResolver {

        // Just maps it directly to file as is
        public File resolveTemplatePathToFile(String templatePath) {
            return new File(templatePath);
        }
    }

    private static class TemplateInfo {
        public final File file;
        public final long fileSize;
        public final long fileDate;
        public final GTTemplateInstanceFactory templateInstanceFactory;

        private TemplateInfo(File file, GTTemplateInstanceFactory templateInstanceFactory) {
            this.file = file;
            // store fileSize and time so we can detech changes.
            fileSize = file.length();
            fileDate = file.lastModified();
            this.templateInstanceFactory = templateInstanceFactory;
        }

        public boolean isModified() {
            File freshFile = new File(file.getAbsolutePath());
            if (!freshFile.exists() || !freshFile.isFile()) {
                return true;
            }
            if (fileSize != freshFile.length()) {
                return true;
            }

            if ( fileDate != freshFile.lastModified()) {
                return true;
            }

            return false;
        }
    }


    public GTTemplateRepo(ClassLoader parentClassLoader, boolean checkForChanges, GTIntegration integration) {
        this.parentClassLoader = parentClassLoader;
        if (parentClassLoader== null) {
            throw new RuntimeException("parentClassLoader cannot be null");
        }
        this.checkForChanges = checkForChanges;
        this.integration = integration;
        if (integration== null) {
            throw new RuntimeException("integration cannot be null");
        }
    }


    public GTJavaBase getTemplateInstance( String templatePath) {

        // Is this a loaded template ?
        TemplateInfo ti = null;
        if ( checkForChanges ) {
            synchronized(loadedTemplates) {
                ti = loadedTemplates.get(templatePath);
                if ( ti != null) {
                    // is it changed on disk?
                    if (ti.isModified()) {
                        // remove it
                        loadedTemplates.remove( templatePath);
                        ti = null;
                    }
                }

                if (ti == null) {
                    // new or modified - must compile it

                    try {
                        // Must map templatePath to File
                        File file = templateFileResolver.resolveTemplatePathToFile( templatePath);

                        if ( !file.exists() || !file.isFile() ) {
                            throw new RuntimeException("Cannot find template file " + templatePath + " (file: )" + file.getAbsolutePath());
                        }

                        // compile it
                        GTCompiler.CompiledTemplate compiledTemplate = new GTCompiler(parentClassLoader).compile(file);

                        GTTemplateInstanceFactory templateInstanceFactory = new GTTemplateInstanceFactory(parentClassLoader, compiledTemplate);

                        ti = new TemplateInfo(file, templateInstanceFactory);
                    } catch (Exception e) {
                        // Must only store it if no error occurs
                        throw new RuntimeException(e);
                    }

                    // store it
                    loadedTemplates.put(templatePath, ti);

                }
            }
        } else {
            // Skip synchronized and modified-check - typically prod mode.
            ti = loadedTemplates.get(templatePath);
            if ( ti == null) {
                throw new RuntimeException("Unknown template " + templatePath);
            }
        }

        if (ti == null) {
            throw new RuntimeException("Not supposed to happen - no template...");
        }

        // already compile and unchanged - lets return the template instance
        GTJavaBase templateInstance = ti.templateInstanceFactory.create();
        // Must tell the template Instance that "we" are the repo - needed when processing #{extends} and custom tags
        templateInstance.templateRepo = this;
        return templateInstance;
    }

}
