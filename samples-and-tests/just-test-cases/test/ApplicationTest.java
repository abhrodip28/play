import org.junit.*;
import play.test.*;
import play.mvc.*;
import play.mvc.Http.*;
import models.*;
import play.Play;

public class ApplicationTest extends FunctionalTest {

    @Test
    public void testThatIndexPageWorks() {
        Response response = GET("/");
        assertIsOk(response);
        assertContentType("text/html", response);
        assertCharset(play.Play.defaultWebEncoding, response);
    }

    @Test
    public void testSimpleStatusCode() {
        Response response = GET("/application/simplestatuscode");
        assertStatus(204, response);
    }
    
    @Test
    public void testGettingUTF8FromConfig() {
        assertEquals("欢迎", Play.configuration.getProperty("utf8value"));
    }
    
    @Test
    public void testFastTag_render() {
        Response response = GET("/application/fastTag_render_test");
        assertContentEquals("OuterInnerRenderPart", response);
    }
    
    @Test
    public void testNewFastTag() {
        Response response = GET("/application/newFastTag_test");
        assertContentEquals("from tag", response);
    }
    
    @Test
    public void testRenderAndCompileTemplateFromStringSource() {
        Response response = GET("/application/renderAndCompileTemplateFromStringSource");
        assertContentEquals("\n"+
        "Source1\n"+
        "123\n"+
        "FromTag: x:"+
        "myDatamyDataQmyDataQ", response);
    }
    
    

}

