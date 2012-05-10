package play.plugins;

import play.PlayPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class PluginMethodMapping {

    public static Set<String> playPluginMethodNames = resolveAllImplementedMethodNames(PlayPlugin.class);

    public final List<PlayPlugin> compileSources = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> detectClassesChange = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> invocationFinally = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> beforeInvocation = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> afterInvocation = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onInvocationSuccess = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onInvocationException = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> beforeDetectingChanges = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> detectChange = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onApplicationReady = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onConfigurationRead = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onApplicationStart = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> afterApplicationStart = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onApplicationStop = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onEvent = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> enhance = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onClassesChange = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> compileAll = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> bind = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> bindBean = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> unBind = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> willBeValidated = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> modelFactory = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> getMessage = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> beforeActionInvocation = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onActionInvocationResult = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> afterActionInvocation = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> routeRequest = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onRequestRouting = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> onRoutesLoaded = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> rawInvocation = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> serveStatic = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> addTemplateExtensions = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> overrideTemplateSource = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> loadTemplate = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> afterFixtureLoad = new ArrayList<PlayPlugin>(0);
    public final List<PlayPlugin> runTest = new ArrayList<PlayPlugin>(0);


    public PluginMethodMapping() {
    }

    public void enablePlugin(PlayPlugin plugin) {
        Set<String> implementedMethods = resolveImplementedPluginMethod(plugin.getClass());
        for (String name : playPluginMethodNames ) {
            List<PlayPlugin> list = getPluginsForMethod(name);
            if (list != null && implementedMethods.contains(name)) {
                list.add(plugin);
                Collections.sort(list);
            }
        }
    }

    public void disablePlugin(PlayPlugin plugin) {
        Set<String> implementedMethods = resolveImplementedPluginMethod(plugin.getClass());
        for (String name : playPluginMethodNames ) {
            List<PlayPlugin> list = getPluginsForMethod(name);
            if (list != null && implementedMethods.contains(name)) {
                list.remove(plugin);
            }
        }
    }

    protected List<PlayPlugin> getPluginsForMethod(String methodName) {
        try {
            Field f = this.getClass().getField(methodName);
            List<PlayPlugin> list = (List<PlayPlugin>)f.get(this);
            return list;

        } catch (Exception e) {
            // nop - if we do not find the field, this method does not have special mapping.
            return null;
        }
    }

    /**
     * Returns the name of all methods in class - Only implemented methods - not those available from parent class
     */
    public static Set<String> resolveAllImplementedMethodNames(Class<? extends PlayPlugin> clazz) {
        Set<String> pluginMethodNames = new HashSet<String>();
        for (Method m : clazz.getDeclaredMethods() ) {
            pluginMethodNames.add( m.getName());
        }

        return pluginMethodNames;
    }

    public static Set<String> resolveImplementedPluginMethod(Class<? extends PlayPlugin> clazz) {
        Set<String> implementedPluginMethodNames = new HashSet<String>();
        Set<String> allMethodNames = resolveAllImplementedMethodNames(clazz);
        for ( String name : playPluginMethodNames) {
            if ( allMethodNames.contains(name) ) {
                implementedPluginMethodNames.add(name);
            }
        }

        if ( !clazz.getSuperclass().equals(PlayPlugin.class)) {
            // check patent class
            implementedPluginMethodNames.addAll( resolveImplementedPluginMethod( (Class<? extends PlayPlugin>)clazz.getSuperclass()));
        }

        return implementedPluginMethodNames;
    }

}
