package play.templates;

import play.Play;
import play.template2.GTGroovyBase;
import play.template2.GTJavaBase;
import play.template2.GTTemplateRepo;
import play.template2.compile.GTPreCompiler;
import play.template2.legacy.GTLegacyFastTagResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GTPreCompiler1xImpl extends GTPreCompiler {

    private GTLegacyFastTagResolver legacyFastTagResolver = new GTLegacyFastTagResolver1X();

    public GTPreCompiler1xImpl(GTTemplateRepo templateRepo) {
        super(templateRepo);
    }


    // must modify all use of @{} in tag args
    @Override
    protected String checkAndPatchActionStringsInTagArguments(String tagArgs) {
        // We only have to try to replace the following if we find at least one
        // @ in tagArgs..
        if (tagArgs.indexOf('@')>=0) {
            tagArgs = tagArgs.replaceAll("[:]\\s*[@]{2}", ":actionBridge._abs().");
            tagArgs = tagArgs.replaceAll("(\\s)[@]{2}", "$1actionBridge._abs().");
            tagArgs = tagArgs.replaceAll("[:]\\s*[@]{1}", ":actionBridge.");
            tagArgs = tagArgs.replaceAll("(\\s)[@]{1}", "$1actionBridge.");
        }
        return tagArgs;
    }

    static final Pattern staticFileP = Pattern.compile("^'(.*)'$");

    @Override
    protected GTFragmentCode generateRegularActionPrinter(boolean absolute, String action, SourceContext sc) {

        String code;
        Matcher m = staticFileP.matcher(action.trim());
        if (m.find()) {
            // This is an action/link to a static file.
            // resolve it now and include the resolved link in the template code.
            action = m.group(1); // without ''
            // resolve it
            String link = GTJavaBase1xImpl.__reverseWithCheck(action, absolute);
            // prepare link for java-code-string
            link = link.replaceAll("\\\\", "\\\\");
            //code = " out.append(__reverseWithCheck("+action+", "+absolute+"));\n";
            code = " out.append(\""+link+"\");\n";
        } else {
            if (!action.endsWith(")")) {
                action = action + "()";
            }

            // actionBridge is a special groovy object that will be an object present in the groovy runtime.
            // we must create a groovy method that execute this special groovy actionBridge-hack code, then
            // we return the java code snipit that will get the result from the groovy method, then print the result

            // generate groovy code
            StringBuilder gout = sc.gout;
            String groovyMethodName = "action_resolver_" + (sc.nextMethodIndex++);

            gout.append(" String " + groovyMethodName + "() {\n");
            if (absolute) {
                gout.append(" return actionBridge._abs()." + action + ";\n");
            } else {
                gout.append(" return actionBridge." + action + ";\n");
            }
            gout.append(" }\n");

            // generate java code that prints it
            code = " out.append(g."+groovyMethodName+"());";
        }

        return new GTFragmentCode(code);
    }

    @Override
    public Class<? extends GTGroovyBase> getGroovyBaseClass() {
        return GTGroovyBase1xImpl.class;
    }

    @Override
    public Class<? extends GTJavaBase> getJavaBaseClass() {
        return GTJavaBase1xImpl.class;
    }

    @Override
    public List<String> getJavaExtensionClasses() {
        List<String> extensionsClassnames = new ArrayList<String>(5);
        extensionsClassnames.add(JavaExtensions.class.getName());
        try {
            extensionsClassnames.addAll( Play.pluginCollection.addTemplateExtensions());
            List<Class> extensionsClasses = Play.classloader.getAssignableClasses(JavaExtensions.class);
            for (Class extensionsClass : extensionsClasses) {
                extensionsClassnames.add(extensionsClass.getName());
            }
        } catch (Throwable e) {
            //
        }
        return extensionsClassnames;
    }

    @Override
    public GTLegacyFastTagResolver getGTLegacyFastTagResolver() {
        return legacyFastTagResolver;
    }
}
