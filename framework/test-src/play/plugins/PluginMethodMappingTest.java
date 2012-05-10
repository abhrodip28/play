package play.plugins;

import org.junit.Test;
import play.PlayPlugin;

import java.util.Set;
import static org.fest.assertions.Assertions.assertThat;


public class PluginMethodMappingTest {

    @Test
    public void testResolvePlayPluginMethod() throws Exception {
        for ( String name : PluginMethodMapping.resolveAllImplementedMethodNames(PlayPlugin.class) ) {
            System.out.println("Plugin method name: " + name);
        }
    }

    @Test
    public void testResolveImplementedPluginMethod() {
        Set<String> methods = PluginMethodMapping.resolveImplementedPluginMethod(ConfigurablePluginDisablingPlugin.class);
        assertThat(methods.size()).isEqualTo(1);
        assertThat(methods.contains("onConfigurationRead")).isTrue();

        // Test inheritance
        methods = PluginMethodMapping.resolveImplementedPluginMethod(TestPluginChild.class);
        assertThat(methods.size()).isEqualTo(2);
        assertThat(methods.contains("onLoad")).isTrue();
        assertThat(methods.contains("compileSources")).isTrue();

    }

}

class TestPluginSuper extends PlayPlugin {

    @Override
    public void onLoad() {
        super.onLoad();    //To change body of overridden methods use File | Settings | File Templates.
    }

}

class TestPluginChild extends TestPluginSuper {
    @Override
    public boolean compileSources() {
        return super.compileSources();    //To change body of overridden methods use File | Settings | File Templates.
    }
}
