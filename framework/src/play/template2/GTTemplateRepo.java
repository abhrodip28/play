package play.template2;

import play.template2.compile.GTCompiler;
import play.template2.compile.GTPreCompilerFactory;
import play.template2.exceptions.GTCompilationException;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;
import play.template2.exceptions.GTException;
import play.template2.exceptions.GTTemplateNotFound;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GTTemplateRepo {

    public final ClassLoader parentClassLoader;
    public final boolean checkForChanges;
    public final GTPreCompilerFactory preCompilerFactory;

    public static GTTemplateFileResolver templateFileResolver = new GTDefaultTemplateFileResolver();

    public final GTIntegration integration;

    private Map<String, TemplateInfo> loadedTemplates = new HashMap<String, TemplateInfo>();


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


    public GTTemplateRepo(ClassLoader parentClassLoader, boolean checkForChanges, GTIntegration integration, GTPreCompilerFactory preCompilerFactory) {
        this.parentClassLoader = parentClassLoader;
        if (parentClassLoader== null) {
            throw new GTException("parentClassLoader cannot be null");
        }
        this.checkForChanges = checkForChanges;
        this.integration = integration;
        if (integration== null) {
            throw new GTException("integration cannot be null");
        }

        this.preCompilerFactory = preCompilerFactory;
        if ( preCompilerFactory ==null ) {
            throw new GTException("preCompilerFactory cannot be null");
        }
    }


    public boolean templateExists( String templatePath) {
        File file = templateFileResolver.resolveTemplatePathToFile( templatePath);

        if ( file == null || !file.exists() || !file.isFile() ) {
            return false;
        }
        return true;
    }

    public GTJavaBase getTemplateInstance( String templatePath) throws GTTemplateNotFound {

        // Is this a loaded template ?
        TemplateInfo ti = loadedTemplates.get(templatePath);
        if ( ti == null || checkForChanges ) {
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

                        if ( file == null || !file.exists() || !file.isFile() ) {
                            throw new GTTemplateNotFound(templatePath);
                        }

                        // compile it
                        GTCompiler.CompiledTemplate compiledTemplate = new GTCompiler(parentClassLoader, this, preCompilerFactory).compile( templatePath, file);

                        GTTemplateInstanceFactory templateInstanceFactory = new GTTemplateInstanceFactory(parentClassLoader, compiledTemplate);

                        ti = new TemplateInfo(file, templateInstanceFactory);
                    } catch(GTTemplateNotFound e) {
                        throw e;
                    } catch(GTCompilationExceptionWithSourceInfo e) {
                        throw e;
                    } catch (Exception e) {
                        // Must only store it if no error occurs
                        throw new GTCompilationException(e);
                    }

                    // store it
                    loadedTemplates.put(templatePath, ti);

                }
            }
        } else {
            if ( ti == null) {
                throw new GTTemplateNotFound(templatePath);
            }
        }

        if (ti == null) {
            throw new GTException("Not supposed to happen - no template...");
        }

        // already compile and unchanged - lets return the template instance
        GTJavaBase templateInstance = ti.templateInstanceFactory.create();
        // Must tell the template Instance that "we" are the repo - needed when processing #{extends} and custom tags
        templateInstance.templateRepo = this;
        return templateInstance;
    }

}
