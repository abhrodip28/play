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


    public boolean generateCodeForGTFragments( String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {

        // Check if we have a method named 'tag_tagName'

        Method tagMethod = null;
        try {
            tagMethod = getClass().getMethod("tag_"+tagName, String.class, String.class, GTPreCompiler.SourceContext.class);
        } catch( Exception e) {
            // did not find a method to handle this tag
            return false;
        }

        try {
            tagMethod.invoke(this, tagName, contentMethodName, sc);
        } catch (Exception e) {
            throw new RuntimeException("Error generating code for tag '"+tagName+"'");
        }

        return true;
    }

    public void tag_list(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;
        out.append(" Collection items = (Collection)tagArgs.get(\"items\");\n");
        out.append(" String as = (String)tagArgs.get(\"as\");\n");
        out.append(" String itemName = (as==null?\"_\":as);\n");
        out.append(" as = (as == null ? \"\" : as);\n");
        out.append(" int i=0;\n");
        out.append(" int size=items.size();\n");
        out.append(" for(Object item : items) {\n");
        // prepare for next iteration
        out.append("   i++;\n");
        out.append("   binding.setProperty(itemName, item);\n");
        out.append("   binding.setProperty(as+\"_index\", i);\n");
        out.append("   binding.setProperty(as+\"_isLast\", i==size);\n");
        out.append("   binding.setProperty(as+\"_isFirst\", i==1);\n");
        out.append("   binding.setProperty(as+\"_parity\", (i%2==0?\"even\":\"odd\"));\n");

        // call list tag content
        out.append("   "+contentMethodName+"();\n");

        out.append(" }\n");
    }

    public void tag_set(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;
        String contentVariableName = "content";
        generateContentOutputCapturing(contentMethodName, contentVariableName, out);
        //store the output in the set-/get-map
        out.append("tag_set_get_store.put");

    }


    public void tag_if(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // extract the argument named "arg"
        out.append(" Object e = tagArgs.get(\"arg\");\n");
        // evaluate it to boolean
        out.append(" Boolean b = g.ifChecker(e);\n");

        // clear the runNextElse
        out.append(" runNextElse.remove(tlid);\n");
        // do the if
        out.append(" if(b) {"+contentMethodName+"();} else { runNextElse.add(tlid); }\n");
    }

    public void tag_ifnot(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // extract the argument named "arg"
        out.append(" Object e = tagArgs.get(\"arg\");\n");
        // evaluate it to boolean
        out.append(" Boolean b = g.ifChecker(e);\n");

        // clear the runNextElse
        out.append(" runNextElse.remove(tlid);\n");
        // do the if
        out.append(" if(!b) {"+contentMethodName+"();} else { runNextElse.add(tlid); }\n");
    }

    public void tag_else(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // run the else if runNextElse is true

        // do the if
        out.append(" if( runNextElse.contains(tlid)) {"+contentMethodName+"();}\n");

        // clear runNextElse
        out.append(" runNextElse.remove(tlid);\n");
    }

    public void tag_elseif(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // run the elseif if runNextElse is true AND expression is true

        // do the if
        out.append(" if( runNextElse.contains(tlid)) {\n");

        // Just include the regluar if-tag here..
        tag_if(tagName, contentMethodName, sc);

        out.append(" }\n");
    }

    public void tag_extends(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // the template we extends is the single argument named 'args'
        out.append(" this.extendsTemplatePath = (String)tagArgs.get(\"arg\");\n");

        // that's it..
    }

    public void tag_doLayout(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // someone is extending us - and we are supposed to dump the output now..
        out.append(" if( this.extendingTemplate == null) throw new RuntimeException(\"No template is currently extending this template\");\n");
        // inject all the output from the extending template into our output stream
        out.append(" this.insertOutput(this.extendingTemplate);\n");

        // done..

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
