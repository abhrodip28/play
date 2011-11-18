package play.template2;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

public class DefInScriptsRemoverTest {
    
    @Test
    public void testIt() throws Exception{
        TemplateSourceRenderer r = new TemplateSourceRenderer( new GTTemplateRepoBuilder().build());
        
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("myData", "xxx");

        assertThat(r.renderSrc("%{ a = 'x'; b = 'y' }%${a}:${b}", args)).isEqualTo("x:y");
        assertThat(r.renderSrc("%{ def a = 'x'; b = 'y' }%${a}:${b}", args)).isEqualTo("x:y");
        assertThat(r.renderSrc("%{ if(true){def a = 'x'}; b = 'y' }%${a}:${b}", args)).isEqualTo("x:y");

        assertThat(r.renderSrc("%{ String a = 'x'; b = 'y' }%${a}:${b}", args)).isEqualTo("x:y");
        assertThat(r.renderSrc("%{ if(true){String a = 'x'}; b = 'y' }%${a}:${b}", args)).isEqualTo("x:y");

        assertThat(r.renderSrc("%{ Integer a = 1; b = 'y' }%${a}:${b}", args)).isEqualTo("1:y");
        assertThat(r.renderSrc("%{ if(true){Long a = 2}; b = 'y' }%${a}:${b}", args)).isEqualTo("2:y");


    }
}
