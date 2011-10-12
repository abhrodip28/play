package play.template2;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class GTRenderingResult {

    protected List<StringWriter> allOuts = new ArrayList<StringWriter>();

    public GTRenderingResult() {
    }

    public GTRenderingResult(List<StringWriter> allOuts) {
        this.allOuts = allOuts;
    }
}
