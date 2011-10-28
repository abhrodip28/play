package play.templates;

import play.Logger;
import play.Play;
import play.exceptions.TemplateCompilationException;
import play.exceptions.TemplateNotFoundException;
import play.libs.Codec;
import play.libs.Crypto;
import play.template2.GTFileResolver;
import play.template2.GTJavaBase;
import play.template2.GTTemplateInstanceFactory;
import play.template2.GTTemplateLocation;
import play.template2.GTTemplateLocationReal;
import play.template2.GTTemplateRepo;
import play.template2.compile.GTCompiler;
import play.template2.exceptions.GTCompilationException;
import play.template2.exceptions.GTCompilationExceptionWithSourceInfo;
import play.template2.exceptions.GTTemplateNotFound;
import play.templates.gt_integration.GTFileResolver1xImpl;
import play.templates.gt_integration.PreCompilerFactory;
import play.utils.HTML;
import play.utils.Utils;
import play.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load templates
 */
public class TemplateLoader {

    private static GTTemplateRepo templateRepo;
    private static Map<String, Template> templatesWithoutFileCache = new HashMap<String, Template>();

    public static void init() {
        // set up folder where we dump generated src
        GTFileResolver.impl = new GTFileResolver1xImpl(Play.templatesPath);
        GTCompiler.srcDestFolder = new File(Play.applicationPath, "generated-src");
        templateRepo = new GTTemplateRepo( Play.classloader,  Play.mode == Play.Mode.DEV, new PreCompilerFactory());
        templatesWithoutFileCache.clear();
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
        getGTTemplateInstance(templateLocation);

        return new GTTemplate(templateLocation);

    }

    protected static GTJavaBase getGTTemplateInstance( GTTemplateLocationReal templateLocation) {
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

    /**
     * Load a template from a String
     * @param key A unique identifier for the template, used for retreiving a cached template
     * @param source The template source
     * @return A Template
     */
    public static Template load(String key, String source) {

        Template template = templatesWithoutFileCache.get(key);
        if ( template != null) {
            return template;
        }

        template = generateTemplateFromEmbededSource(key, source);

        templatesWithoutFileCache.put(key, template);
        return template;
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
        templatesWithoutFileCache.remove(key);
        return load( key, source);
    }


    private static class GTTemplateLocationWithEmbeddedSource extends GTTemplateLocationReal {

        private final String key;
        private final String source;

        private GTTemplateLocationWithEmbeddedSource(String key, String source) {
            super("embedded_source_"+ Codec.encodeBASE64(key), null);
            this.key = key;
            this.source = source;
        }

        @Override
        public String readSource() {
            return this.source;
        }
    }

    private static class GTTemplateWithEmbeddedSource extends GTTemplate {
        private final GTTemplateInstanceFactory templateInstanceFactory;

        private GTTemplateWithEmbeddedSource(String name, GTTemplateInstanceFactory templateInstanceFactory) {
            super(name);
            this.templateInstanceFactory = templateInstanceFactory;
        }

        @Override
        protected GTJavaBase getGTTemplateInstance() {
            return templateInstanceFactory.create(templateRepo);
        }
    }

    /**
     * Load template from a String, but don't cache it
     * @param source The template source
     * @return A Template
     */
    public static Template loadString(final String source) {

        final String key = Codec.UUID();
        return generateTemplateFromEmbededSource(key, source);
    }

    private static Template generateTemplateFromEmbededSource(String key, String source) {
        GTCompiler.CompiledTemplate compiledTemplate = new GTCompiler(Play.classloader, templateRepo, new PreCompilerFactory(), false).compile(new GTTemplateLocationWithEmbeddedSource(key, source));
        GTTemplateInstanceFactory templateInstanceFactory = new GTTemplateInstanceFactory(Play.classloader, compiledTemplate);

        return new GTTemplateWithEmbeddedSource(key, templateInstanceFactory);
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
        throw new RuntimeException("Not implemented yet");
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
