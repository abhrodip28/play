package play.template2.compile;

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
        out.append(" if (items == null ) return ;\n");
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


    public void tag_if(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // extract the argument named "arg"
        out.append(" Object e = tagArgs.get(\"arg\");\n");
        // clear the runNextElse
        out.append(" clearElseFlag();\n");
        // do the if
        out.append(" if(evaluateCondition(e)) {"+contentMethodName+"();} else { setElseFlag(); }\n");
    }

    public void tag_ifnot(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // extract the argument named "arg"
        out.append(" Object e = tagArgs.get(\"arg\");\n");

        // clear the runNextElse
        out.append(" clearElseFlag();\n");
        // do the if
        out.append(" if(!evaluateCondition(e)) {"+contentMethodName+"();} else { setElseFlag(); }\n");
    }

    public void tag_else(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // run the else if runNextElse is true

        // do the if
        out.append(" if( elseFlagIsSet()) {"+contentMethodName+"();}\n");

        // clear runNextElse
        out.append(" clearElseFlag();\n");
    }

    public void tag_elseif(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // run the elseif if runNextElse is true AND expression is true

        // do the if
        out.append(" if( elseFlagIsSet()) {\n");

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

    // used when dumping the output from the template that extended this one
    public void tag_doLayout(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // someone is extending us - and we are supposed to dump the output now..
        out.append(" if( this.extendingTemplate == null) throw new RuntimeException(\"No template is currently extending this template\");\n");
        // inject all the output from the extending template into our output stream
        out.append(" this.insertOutput(this.extendingTemplate);\n");

        // done..
    }

    // used when dumping the content-output when rendering a tag-file / template
    public void tag_doBody(String tagName, String contentMethodName, GTPreCompiler.SourceContext sc) {
        StringBuilder out = sc.out;

        // must make sure we actually have content to dump..
        out.append(" if( this.contentRenderer == null) throw new RuntimeException(\"No body to dump - Are this template used as a tag?\");\n");
        // render the content and inject all the output into our output stream
        out.append(" this.insertOutput(this.contentRenderer.render());\n");

        // done..
    }

    protected static void generateContentOutputCapturing( String contentMethodName, String outputVariableName, StringBuilder out) {
        out.append("//generateContentOutputCapturing\n");
        // remember the original out
        out.append("StringWriter org = out;\n");
        // remember the original list
        out.append("List<StringWriter> orgAllOuts = allOuts;\n");

        // create a new one for capture
        out.append("allOuts = new ArrayList<StringWriter>();\n");
        out.append("initNewOut();\n");

        // call the content-method
        out.append(contentMethodName+"();\n");
        // store the output
        out.append("List<StringWriter> "+outputVariableName+" = allOuts;\n");
        // restore the original out
        out.append("out = org;\n");
        // restore the list
        out.append("allOuts = orgAllOuts;\n");

    }
}
