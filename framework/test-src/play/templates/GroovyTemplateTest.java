package play.templates;

import org.junit.Test;
import play.PlayBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class GroovyTemplateTest {

    @Test
    public void runit() {

        new PlayBuilder().build();

        String groovySrc = "#{list items: myList, as: 'item'}\n" +
                "    a${item}\n" +
                "    b${item}\n" +
                "    c${item}\n" +
                "    d${item}\n" +
                "    e${item}\n" +
                "    f${item}\n" +
                "    g${item}\n" +
                "    h${item}\n" +
                "    i${item}\n" +
                "    a${item}\n" +
                "    b${item}\n" +
                "    c${item}\n" +
                "    d${item}\n" +
                "    e${item}\n" +
                "    f${item}\n" +
                "    g${item}\n" +
                "    h${item}\n" +
                "    i${item}\n" +
                "#{/list}\n" +
                "Second go\n" +
                "#{list items: myList, as: 'item2'}\n" +
                "    a${item2} - ${item2_index} - ${item2_parity}\n" +
                "    b${item2} - ${item2_index} - ${item2_parity}\n" +
                "    c${item2} - ${item2_index} - ${item2_parity}\n" +
                "    d${item2} - ${item2_index} - ${item2_parity}\n" +
                "    e${item2} - ${item2_index} - ${item2_parity}\n" +
                "    f${item2} - ${item2_index} - ${item2_parity}\n" +
                "    g${item2} - ${item2_index} - ${item2_parity}\n" +
                "    h${item2} - ${item2_index} - ${item2_parity}\n" +
                "    i${item2} - ${item2_index} - ${item2_parity}\n" +
                "    a${item2} - ${item2_index} - ${item2_parity}\n" +
                "    b${item2} - ${item2_index} - ${item2_parity}\n" +
                "    c${item2} - ${item2_index} - ${item2_parity}\n" +
                "    d${item2} - ${item2_index} - ${item2_parity}\n" +
                "    e${item2} - ${item2_index} - ${item2_parity}\n" +
                "    f${item2} - ${item2_index} - ${item2_parity}\n" +
                "    g${item2} - ${item2_index} - ${item2_parity}\n" +
                "    h${item2} - ${item2_index} - ${item2_parity}\n" +
                "    i${item2} - ${item2_index} - ${item2_parity}\n" +
                "#{/list}\n" +
                "third go\n" +
                "#{list items: myList, as: 'item'}\n" +
                "    a${item}\n" +
                "    b${item}\n" +
                "    c${item}\n" +
                "    d${item}\n" +
                "    e${item}\n" +
                "    f${item}\n" +
                "    g${item}\n" +
                "    h${item}\n" +
                "    i${item}\n" +
                "    a${item}\n" +
                "    b${item}\n" +
                "    c${item}\n" +
                "    d${item}\n" +
                "    e${item}\n" +
                "    f${item}\n" +
                "    g${item}\n" +
                "    h${item}\n" +
                "    i${item}\n" +
                "#{/list}\n" +
                "forth go\n" +
                "#{list items: myList, as: 'item2'}\n" +
                "    a${item2} - ${item2_index} - ${item2_parity}\n" +
                "    b${item2} - ${item2_index} - ${item2_parity}\n" +
                "    c${item2} - ${item2_index} - ${item2_parity}\n" +
                "    d${item2} - ${item2_index} - ${item2_parity}\n" +
                "    e${item2} - ${item2_index} - ${item2_parity}\n" +
                "    f${item2} - ${item2_index} - ${item2_parity}\n" +
                "    g${item2} - ${item2_index} - ${item2_parity}\n" +
                "    h${item2} - ${item2_index} - ${item2_parity}\n" +
                "    i${item2} - ${item2_index} - ${item2_parity}\n" +
                "    a${item2} - ${item2_index} - ${item2_parity}\n" +
                "    b${item2} - ${item2_index} - ${item2_parity}\n" +
                "    c${item2} - ${item2_index} - ${item2_parity}\n" +
                "    d${item2} - ${item2_index} - ${item2_parity}\n" +
                "    e${item2} - ${item2_index} - ${item2_parity}\n" +
                "    f${item2} - ${item2_index} - ${item2_parity}\n" +
                "    g${item2} - ${item2_index} - ${item2_parity}\n" +
                "    h${item2} - ${item2_index} - ${item2_parity}\n" +
                "    i${item2} - ${item2_index} - ${item2_parity}\n" +
                "#{/list}";

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);


        List<Integer> myList = new ArrayList<Integer>();
        for (int i=0;i<1;i++) {
            myList.add(i);
        }

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("myList", myList);

        String out = t.render( args );
        out = t.render( args );
        out = t.render( args );
        long start = System.currentTimeMillis();
        out = t.render( args );
        long now = System.currentTimeMillis();
        long diff = now-start;
        System.out.println("mills: " + diff);

        //System.out.println(out);

    }

    @Test
    public void verifyRenderingTwice() {

        new PlayBuilder().build();

        String groovySrc = "hello world: ${name}";

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("name", "Morten");
        assertThat( t.render( args ) ).isEqualTo("hello world: Morten");

        //do it again
        assertThat( t.render( args ) ).isEqualTo("hello world: Morten");

    }

    @Test
    public void verifyCompilingExtremelyLongLines() {

        new PlayBuilder().build();

        StringBuilder longString = new StringBuilder();
        for (int i=0;i<1000;i++) {
            longString.append("11111111112222222222333333333344444444445555555555");
            longString.append("11111111112222222222333333333344444444445555555555");
        }

        String groovySrc = "hello world"+longString+": ${name}";
        // make sure our test line is longer then maxPlainTextLength
        assertThat(groovySrc.length()).isGreaterThan( GroovyTemplateCompiler.maxPlainTextLength + 100);

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("name", "Morten");
        assertThat( t.render( args ) ).isEqualTo("hello world"+longString+": Morten");

    }

    @Test
    public void verifyCompilingExtremelyLongLinesWithLinefeed() {

        new PlayBuilder().build();

        // when printing text from template, newlines (0x0d) is transformed into the string '\n'.
        // when breaking lines it is a problem if the '\' is at the end on one line and 'n'
        // is at the beginning of the next line.


        //first we test with just a '\' as last char
        internalVerifyCompilingExtremelyLongLinesWithSpecialCharAsLastCharBeforeBreak('\\');

        // now we test with 0x0d '\n' as last char
        internalVerifyCompilingExtremelyLongLinesWithSpecialCharAsLastCharBeforeBreak('\n');

    }

    private void internalVerifyCompilingExtremelyLongLinesWithSpecialCharAsLastCharBeforeBreak(char lastChar) {
        StringBuilder longString = new StringBuilder();
        for (int i=0;i<1000;i++) {
            longString.append("11111111112222222222333333333344444444445555555555");
            longString.append("11111111112222222222333333333344444444445555555555");
        }

        // now insert a special char on the last line before we split the plainText with new print
        longString.insert(GroovyTemplateCompiler.maxPlainTextLength-1, lastChar);

        String groovySrc = longString+": ${name}";
        // make sure our test line is longer then maxPlainTextLength
        assertThat(groovySrc.length()).isGreaterThan( GroovyTemplateCompiler.maxPlainTextLength + 100);

        GroovyTemplate t = new GroovyTemplate("Template_123", groovySrc);
        new GroovyTemplateCompiler().compile(t);

        Map<String, Object> args = new HashMap<String,Object>();
        args.put("name", "Morten");
        assertThat( t.render( args ) ).isEqualTo(longString+": Morten");
    }
  
    // [#107] caused any tag broken with a CR to fail. (It would be compiled to list arg:items:....).
    @Test
    public void verifyCompilingWithCR() {
        final String source = "#{list items:1..3,\ras:'i'}${i}#{/list}";
        GroovyTemplate groovyTemplate = new GroovyTemplate("tag_broken_by_CR", source);
        new GroovyTemplateCompiler().compile(groovyTemplate);
        assertEquals("123",groovyTemplate.render());
    }

    @Test
    public void verifyCompilingWithLF() {
        final String source = "#{list items:1..3,\nas:'i'}${i}#{/list}";
        GroovyTemplate groovyTemplate = new GroovyTemplate("tag_broken_by_LF", source);
        new GroovyTemplateCompiler().compile(groovyTemplate);
        assertEquals("123", groovyTemplate.render());
    }

    @Test
    public void verifyCompilingWithCRLF() {
        final String source = "#{list items:1..3,\r\nas:'i'}${i}#{/list}";
        GroovyTemplate groovyTemplate = new GroovyTemplate("tag_broken_by_CRLF", source);
        new GroovyTemplateCompiler().compile(groovyTemplate);
        assertEquals("123", groovyTemplate.render());
    }

    @Test
    public void verifyCompilingWithMultipleCRandLF() {
        final String source = "#{list items:1..3,\r\n\r\r\n\nas:'i'}${i}#{/list}";
        GroovyTemplate groovyTemplate = new GroovyTemplate("Broken_with_multiple_CR_and_LF", source);
        new GroovyTemplateCompiler().compile(groovyTemplate);
        assertEquals("123", groovyTemplate.render());
    }
}
