package play.data.parsing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import play.exceptions.UnexpectedException;
import play.mvc.Http;
import play.utils.Utils;

/**
 * Parse url-encoded requests.
 */
public class UrlEncodedParser extends DataParser {
    
    boolean forQueryString = false;
    
    public static Map<String, String[]> parse(String urlEncoded) {
        try {
            final String encoding = Http.Request.current().encoding;
            return new UrlEncodedParser().parse(new ByteArrayInputStream(urlEncoded.getBytes( encoding )));
        } catch (UnsupportedEncodingException ex) {
            throw new UnexpectedException(ex);
        }
    }
    
    public static Map<String, String[]> parseQueryString(InputStream is) {
        UrlEncodedParser parser = new UrlEncodedParser();
        parser.forQueryString = true;
        return parser.parse(is);
    }

    @Override
    public Map<String, String[]> parse(InputStream is) {
        final String encoding = Http.Request.current().encoding;
        try {
            Map<String, String[]> params = new HashMap<String, String[]>();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            int b;
            //TODO: This is realy slow!!
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            byte[] data = os.toByteArray();
            // add the complete body as a parameters
            if(!forQueryString) {
                params.put("body", new String[] {new String(data, encoding)});
            }
            
            int ix = 0;
            int ox = 0;
            String key = null;
            String value = null;
            while (ix < data.length) {
                byte c = data[ix++];
                switch ((char) c) {
                    case '&':
                        value = new String(data, 0, ox, encoding);
                        if (key != null) {
                            Utils.Maps.mergeValueInMap(params, key, value);
                            key = null;
                        } else {
                            Utils.Maps.mergeValueInMap(params, value, (String) null);
                        }
                        ox = 0;
                        break;
                    case '=':
                        if (key == null) {
                            key = new String(data, 0, ox, encoding);
                            ox = 0;
                        } else {
                            data[ox++] = c;
                        }
                        break;
                    case '+':
                        data[ox++] = (byte) ' ';
                        break;
                    case '%':
                        data[ox++] = (byte) ((convertHexDigit(data[ix++]) << 4) + convertHexDigit(data[ix++]));
                        break;
                    default:
                        data[ox++] = c;
                }
            }
            //The last value does not end in '&'.  So save it now.
            value = new String(data, 0, ox, encoding);
            if (key != null) {
                Utils.Maps.mergeValueInMap(params, key, value);
            } else if (!value.isEmpty()) {
                Utils.Maps.mergeValueInMap(params, value, (String) null);
            }
            return params;
        } catch (Exception e) {
            throw new UnexpectedException(e);
        }
    }

    private static byte convertHexDigit(byte b) {
        if ((b >= '0') && (b <= '9')) {
            return (byte) (b - '0');
        }
        if ((b >= 'a') && (b <= 'f')) {
            return (byte) (b - 'a' + 10);
        }
        if ((b >= 'A') && (b <= 'F')) {
            return (byte) (b - 'A' + 10);
        }
        return 0;
    }
}
