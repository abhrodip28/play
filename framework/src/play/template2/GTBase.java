package play.template2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GTBase {

    // Used by the set-/get-tags
    protected Map<String, String> tag_set_get_store = new HashMap<String, String>();

    protected StringBuilder out = new StringBuilder();
    protected List<StringBuilder> allOuts = new ArrayList<StringBuilder>();

    public GTBase() {
        allOuts.add(out);
    }

    protected void insertNewOut( StringBuilder outToInsert) {
        allOuts.add( outToInsert);
        // must create new live out
        out = new StringBuilder();
        allOuts.add( out );
    }
    
}
