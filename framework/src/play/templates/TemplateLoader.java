package play.templates;

import play.Logger;
import play.Play;
import play.classloading.GTTypeResolver1xImpl;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateNotFoundException;
import play.libs.Codec;
import play.template2.GTFileResolver;
import play.template2.GTJavaBase;
import play.template2.GTTemplateInstanceFactoryLive;
import play.template2.GTTemplateLocation;
import play.template2.GTTemplateLocationReal;
import play.template2.GTTemplateRepo;
import play.template2.compile.GTCompiler;
import play.template2.compile.GTGroovyPimpTransformer;
import play.template2.compile.GTJavaCompileToClass;
import play.template2.exceptions.GTCompilationException;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;
import play.template2.exceptions.GTTemplateNotFound;
import play.templates.gt_integration.GTFileResolver1xImpl;
import play.templates.gt_integration.GTJavaExtensionMethodResolver1x;
import play.templates.gt_integration.PreCompilerFactory;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Load templates
 */
public class TemplateLoader {

    private static GTTemplateRepo templateRepo;

    public static void init() {

        if ( templateRepo != null && Play.mode == Play.Mode.PROD) {
            return ;
        }

        GTGroovyPimpTransformer.gtJavaExtensionMethodResolver = new GTJavaExtensionMethodResolver1x();
        GTTemplateInstanceFactoryLive.protectionDomain = Play.classloader.protectionDomain;
        // set up folder where we dump generated src
        GTFileResolver.impl = new GTFileResolver1xImpl(Play.templatesPath);
        if (Boolean.parseBoolean( Play.configuration.getProperty("save-gttemplate-source-to-disk", "false") )) {
            GTCompiler.srcDestFolder = new File(Play.applicationPath, "generated-src");
        }

        File folderToDumpClassesIn = null;
        if ( System.getProperty("precompile")!=null) {
            folderToDumpClassesIn = new File(Play.applicationPath, "precompiled/java");
        } else if( Play.mode != Play.Mode.PROD ) {
            folderToDumpClassesIn = new File(Play.applicationPath, "tmp/gttemplates");
        }

        GTJavaCompileToClass.typeResolver = new GTTypeResolver1xImpl();

        templateRepo = new GTTemplateRepo(
                Play.classloader,
                Play.mode == Play.Mode.DEV,
                new PreCompilerFactory(),
                Play.usePrecompiled,
                folderToDumpClassesIn);
    }

    /**
     * Load a template from a virtual file
     * @param file A VirtualFile
     * @return The executable template
     */
    public static Template load(VirtualFile file) {
        // Try with plugin
        Template pluginProvided = Play.pluginCollection.loadTemplate(file);
        if (pluginProvided != null) {
            return pluginProvided;
        }

        // Use default engine

        GTTemplateLocationReal templateLocation = new GTTemplateLocationReal(file.relativePath(), file.getRealFile());

        // get it to check and compile it
        GTJavaBase gtJavaBase = getGTTemplateInstance(templateLocation);

        return new GTTemplate(templateLocation, gtJavaBase);

    }

    protected static GTJavaBase getGTTemplateInstance( GTTemplateLocation templateLocation) {
        try {
            return templateRepo.getTemplateInstance( templateLocation );
        } catch ( GTTemplateNotFound e) {
            throw new TemplateNotFoundException(e.queryPath);
        } catch (GTCompilationExceptionWithSourceInfo e) {
            GTTemplate t = new GTTemplate(e.templateLocation);
            t.loadSource();
            throw new TemplateCompilationException( t, e.oneBasedLineNo, e.specialMessage);
        } catch (GTCompilationException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static class TemplateLocationWithEmbeddedSource extends GTTemplateLocation {
        private String source;

        private TemplateLocationWithEmbeddedSource(String relativePath, String source) {
            super(relativePath);
            this.source = source;
        }

        @Override
        public String readSource() {
            return source;
        }
    }

    /**
     * Load a template from a String
     * @param key A unique identifier for the template, used for retreiving a cached template
     * @param source The template source
     * @return A Template
     */
    public static Template load(String key, String source) {

        TemplateLocationWithEmbeddedSource tl = new TemplateLocationWithEmbeddedSource(key, source);

        // get it or compile it
        GTJavaBase gtJavaBase = getGTTemplateInstance(tl);

        return new GTTemplate(tl, gtJavaBase);
    }

    /**
     * Clean the cache for that key
     * Then load a template from a String
     * @param key A unique identifier for the template, used for retreiving a cached template
     * @param source The template source
     * @return A Template
     */
    public static Template load(String key, String source, boolean reload) {
        // reload is also ignored in the old template implementation...

        TemplateLocationWithEmbeddedSource tl = new TemplateLocationWithEmbeddedSource(key, source);

        // remove it first
        templateRepo.removeTemplate(tl);

        // get it or compile it
        GTJavaBase gtJavaBase = getGTTemplateInstance(tl);

        return new GTTemplate(tl, gtJavaBase);
    }

    /**
     * Load template from a String, but don't cache it
     * @param source The template source
     * @return A Template
     */
    public static Template loadString(final String source) {

        final String key = Codec.UUID();

        GTTemplateLocation templateLocation = new TemplateLocationWithEmbeddedSource(key, source);

        GTTemplateRepo.TemplateInfo ti = templateRepo.compileTemplate(templateLocation);

        GTJavaBase gtJavaBase = ti.templateInstanceFactory.create(templateRepo);

        return new GTTemplate(templateLocation, gtJavaBase);
    }

    /**
     * Cleans the cache for all templates
     */
    public static void cleanCompiledCache() {
        init();
    }

    /**
     * Cleans the specified key from the cache
     * @param key The template key
     */
    public static void cleanCompiledCache(String key) {
        // should only clean cached templates without source
        templateRepo.removeTemplate(new GTTemplateLocation(key));
    }

    /**
     * Load a template
     * @param path The path of the template (ex: Application/index.html)
     * @return The executable template
     */
    public static Template load(String path) {
        Template template = null;
        for (VirtualFile vf : Play.templatesPath) {
            if (vf == null) {
                continue;
            }
            VirtualFile tf = vf.child(path);
            if (tf.exists()) {
                template = TemplateLoader.load(tf);
                break;
            }
        }

        //TODO: remove ?
        if (template == null) {
            VirtualFile tf = Play.getVirtualFile(path);
            if (tf != null && tf.exists()) {
                template = TemplateLoader.load(tf);
            } else {
                throw new TemplateNotFoundException(path);
            }
        }
        return template;
    }

    /**
     * List all found templates
     * @return A list of executable templates
     */
    public static List<Template> getAllTemplate() {
        List<Template> res = new ArrayList<Template>();
        for (VirtualFile virtualFile : Play.templatesPath) {
            scan(res, virtualFile);
        }
        for (VirtualFile root : Play.roots) {
            VirtualFile vf = root.child("conf/routes");
            if (vf != null && vf.exists()) {
                Template template = load(vf);
            }
        }
        return res;
    }

    private static void scan(List<Template> templates, VirtualFile current) {
        if (!current.isDirectory() && !current.getName().startsWith(".") && !current.getName().endsWith(".scala.html")) {
            long start = System.currentTimeMillis();
            Template template = load(current);
            if (Logger.isTraceEnabled()) {
                Logger.trace("%sms to load %s", System.currentTimeMillis() - start, current.getName());
            }
            templates.add(template);
        } else if (current.isDirectory() && !current.getName().startsWith(".")) {
            for (VirtualFile virtualFile : current.list()) {
                scan(templates, virtualFile);
            }
        }
    }
}
