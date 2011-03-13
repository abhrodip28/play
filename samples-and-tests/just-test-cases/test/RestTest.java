import java.io.File;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

import play.Logger;
import play.libs.WS;
import play.libs.WS.FileParam;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Header;
import play.test.UnitTest;

import com.google.gson.JsonObject;


public class RestTest extends UnitTest {
    static Map<String, Object> params;

    @Before
    public void setUp() {
        params = new HashMap<String, Object>();
        params.put("timestamp", 1200000L);
        params.put("cachable", true);
        params.put("multipleValues", new String[]{"欢迎", "dobrodošli", "ยินดีต้อนรับ"});
    }

    @Test
    public void testGet() throws Exception {
        // we have to explicit specify UTF-8 since we know we're sending chinese charachters
        // utf-8 is the default, but we might have configured WS to use other default encoding.
        assertEquals("对!", WS.withEncoding("utf-8").url("http://localhost:9003/ressource/%s", "ééééééçççççç汉语漢語").get().getString());
    }

    @Test
    public void testASCIIGet() throws Exception {
        assertEquals("toto", WS.url("http://localhost:9003/ressource/%s", "foobar").get().getString());
    }

    @Test
    public void testPost() throws Exception {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("id", 101);
        assertEquals(jsonResponse.toString(), WS.withEncoding("utf-8").url("http://localhost:9003/ressource/%s", "名字").params(params).post().getJson().toString());
        File fileToSend = new File(new URLDecoder().decode(getClass().getResource("/kiki.txt").getFile(), "UTF-8"));
        assertTrue(fileToSend.exists());
        
        assertEquals("POSTED!", WS.withEncoding("utf-8").url("http://localhost:9003/ressource/file/%s", "名字").files(new FileParam(fileToSend, "file")).post().getString());
        if( play.mvc.Http.Request.defaultEncoding.equalsIgnoreCase("utf-8")) {
            assertEquals("FILE AND PARAMS POSTED!", WS.withEncoding("utf-8").url("http://localhost:9003/ressource/fileAndParams/%s", "名字").files(new FileParam(fileToSend, "file")).params(params).post().getString());
        } else {
            // multipartUrlEncodingWorkaround: when server an client is not "hardcoded" to use same encoding,
    		// we cannot use other than default server-encoding in url, when we send both form-param AND fileupload (multipart)
    		assertEquals("FILE AND PARAMS POSTED!", WS.withEncoding("utf-8").url("http://localhost:9003/ressource/fileAndParams/%s", "multipartUrlEncodingWorkaround").files(new FileParam(fileToSend, "file")).params(params).post().getString());
        }

    }

    @Test
    public void testHead() throws Exception {
        HttpResponse headResponse = WS.withEncoding("utf-8").url("http://localhost:9003/ressource/%s", "ééééééçççççç汉语漢語").head();
        List<Header> headResponseHeaders = headResponse.getHeaders();
        assertTrue(headResponse.getStatus() == 200);
        assertEquals("", headResponse.getString());
        HttpResponse getResponse = WS.withEncoding("utf-8").url("http://localhost:9003/ressource/%s", "ééééééçççççç汉语漢語").get();
        assertTrue(getResponse.getStatus() == 200);
        List<Header> getResponseHeaders = getResponse.getHeaders();
        for (int i = 0; i < getResponseHeaders.size(); i++) {
            if (!"Set-Cookie".equals(getResponseHeaders.get(i).name)) {
                assertEquals(getResponseHeaders.get(i).value(), headResponseHeaders.get(i).value());
            }
        }
    }

    @Test
    public void testPut() throws Exception {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("id", 101);
        assertEquals(jsonResponse.toString(), WS.withEncoding("utf-8").url("http://localhost:9003/ressource/%s", "名字").params(params).put().getJson().toString());
        File fileToSend = new File(new URLDecoder().decode(getClass().getResource("/kiki.txt").getFile(), "UTF-8"));
        assertTrue(fileToSend.exists());
        assertEquals("POSTED!", WS.withEncoding("utf-8").url("http://localhost:9003/ressource/file/%s", "名字").files(new FileParam(fileToSend, "file")).put().getString());
        
        if( play.mvc.Http.Request.defaultEncoding.equalsIgnoreCase("utf-8")) {
            assertEquals("FILE AND PARAMS POSTED!", WS.withEncoding("utf-8").url("http://localhost:9003/ressource/fileAndParams/%s", "名字").files(new FileParam(fileToSend, "file")).params(params).put().getString());
        } else {
            // multipartUrlEncodingWorkaround: when server an client is not "hardcoded" to use same encoding,
    		// we cannot use other than default server-encoding in url, when we send both form-param AND fileupload (multipart)
    		assertEquals("FILE AND PARAMS POSTED!", WS.withEncoding("utf-8").url("http://localhost:9003/ressource/fileAndParams/%s", "multipartUrlEncodingWorkaround").files(new FileParam(fileToSend, "file")).params(params).put().getString());
        }

    }

    @Test
    public void testParallelCalls() throws Exception {
        Future<HttpResponse> response = WS.withEncoding("utf-8").url("http://localhost:9003/ressource/%s", "ééééééçççççç汉语漢語").getAsync();
        Future<HttpResponse> response2 = WS.withEncoding("utf-8").url("http://localhost:9003/ressource/%s", "foobar").getAsync();
        int success = 0;
        while (success < 2) {
            if (response.isDone()) {
                assertEquals("对!", response.get().getString());
                success++;
            }
            if (response2.isDone()) {
                assertEquals("toto", response2.get().getString());
                success++;
            }
            Thread.sleep(1000);
        }
    }

}
