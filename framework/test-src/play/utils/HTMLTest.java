package play.utils;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class HTMLTest {
    @Test
    public void testHtmlEscape() throws Exception {

        assertThat(HTML.htmlEscape("a")).isEqualTo("a");
        assertThat(HTML.htmlEscape("a<")).isEqualTo("a&lt;");
        assertThat(HTML.htmlEscape("a<<<")).isEqualTo("a&lt;&lt;&lt;");
        assertThat(HTML.htmlEscape("<<<A")).isEqualTo("&lt;&lt;&lt;A");
        assertThat(HTML.htmlEscape("<<<")).isEqualTo("&lt;&lt;&lt;");
        assertThat(HTML.htmlEscape("a<b<cd<e")).isEqualTo("a&lt;b&lt;cd&lt;e");

    }
}
