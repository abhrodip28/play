package play.template2;

import java.lang.reflect.Method;

/**
 * Created by IntelliJ IDEA.
 * User: mortenkjetland
 * Date: 10/11/11
 * Time: 12:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class GTInternalTagsCompiler {


    public boolean generateCodeForGTFragments( String tagName, String tagArgString, String contentMethodName, GTCompiler.SourceContext sc) {

        // Check if we have a method named 'tag_tagName'

        Method tagMethod = null;
        try {
            tagMethod = getClass().getMethod("tag_"+tagName, String.class, String.class, String.class, GTCompiler.SourceContext.class);
        } catch( Exception e) {
            // did not find a method to handle this tag
            return false;
        }

        try {
            tagMethod.invoke(this, tagName, tagArgString, contentMethodName, sc);
        } catch (Exception e) {
            throw new RuntimeException("Error generating code for tag '"+tagName+"'");
        }

        return true;
    }

    public void tag_set(String tagName, String tagArgString, String contentMethodName, GTCompiler.SourceContext sc) {
        StringBuilder out = sc.out;
        String contentVariableName = "content";
        generateContentOutputCapturing(contentMethodName, contentVariableName, out);
        //store the output in the set-/get-map
        out.append("tag_set_get_store.put");

    }

    protected void generateContentOutputCapturing( String contentMethodName, String outputVariableName, StringBuilder out) {
        out.append("//generateContentOutputCapturing\n");
        // remember the original out
        out.append("StringBuilder org = out;\n");
        // create a new one for capture
        out.append("out = new StringBuilder();\n");
        // call the content-method
        out.append(contentMethodName+"();\n");
        // store the output
        out.append("String "+outputVariableName+" = out.toString();\n");
        // restore the original out
        out.append("out = org;\n");
    }
}
